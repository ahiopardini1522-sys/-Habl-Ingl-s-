package ar.hablaingles.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class MainActivity extends Activity {
    /* ===== Actualización automática =====
       El contenido de la app (index.html) se descarga de GitHub cada vez que se abre.
       Si hay una versión más nueva, se guarda en el celular y se ofrece «Actualizar».
       NATIVE_VERSION se sube solo cuando cambia este código Java: ahí la app avisa que hay un APK nuevo. */
    private static final int NATIVE_VERSION = 2;
    private static final String REPO = "ahiopardini1522-sys/-Habl-Ingl-s-";
    private static final String RAW = "https://raw.githubusercontent.com/" + REPO + "/main/";
    private static final String HTML_URL = RAW + "app/src/main/assets/index.html";
    private static final String NATIVE_URL = RAW + "native-version.txt";
    private static final String RELEASES_URL = "https://github.com/" + REPO + "/releases/latest";
    private static final Pattern VER = Pattern.compile("name=\"app-version\" content=\"(\\d+)\"");

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private SpeechRecognizer recognizer;
    private static final int REQ_MIC = 7;
    private static final int REQ_NOTIF = 8;
    private final Locale EN = Locale.US;
    private Locale es = new Locale("es", "AR");
    private long loadedVersion = 0;
    private long lastCheck = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#2A4FE0"));
        getWindow().setNavigationBarColor(Color.parseColor("#F4F7FF"));

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                if (tts.isLanguageAvailable(es) < TextToSpeech.LANG_AVAILABLE) {
                    es = new Locale("es", "US");
                    if (tts.isLanguageAvailable(es) < TextToSpeech.LANG_AVAILABLE) es = new Locale("es");
                }
                tts.setLanguage(EN);
                ttsReady = true;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { }
                    @Override public void onDone(String id) { }
                    @Override public void onError(String id) { }
                });
            }
        });

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F4F7FF"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        setContentView(web);
        loadBest();
        checkUpdates();
    }

    /* ---------- carga de la mejor versión disponible ---------- */
    private File cacheFile() { return new File(getFilesDir(), "index.html"); }

    private static long versionOf(String html) {
        if (html == null) return 0;
        Matcher m = VER.matcher(html);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[16384];
        int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    private void loadBest() {
        String asset = null, cached = null;
        try { asset = readAll(getAssets().open("index.html")); } catch (Exception ignored) { }
        try { File f = cacheFile(); if (f.exists()) cached = readAll(new java.io.FileInputStream(f)); } catch (Exception ignored) { }
        String html = asset;
        if (versionOf(cached) > versionOf(asset) && cached.contains("</html>")) html = cached;
        loadedVersion = versionOf(html);
        // misma dirección base siempre, así el progreso guardado no se pierde
        web.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
    }

    private static String download(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url + "?t=" + System.currentTimeMillis()).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(15000);
            c.setUseCaches(false);
            c.setRequestProperty("Cache-Control", "no-cache");
            if (c.getResponseCode() != 200) return null;
            return readAll(c.getInputStream());
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private void checkUpdates() {
        lastCheck = System.currentTimeMillis();
        new Thread(() -> {
            String html = download(HTML_URL);
            long v = versionOf(html);
            if (html != null && v > loadedVersion && html.contains("</html>")) {
                try {
                    File tmp = new File(getFilesDir(), "index.tmp");
                    FileOutputStream o = new FileOutputStream(tmp);
                    o.write(html.getBytes(StandardCharsets.UTF_8));
                    o.close();
                    if (tmp.renameTo(cacheFile())) js("window.__updateReady&&window.__updateReady(" + v + ")");
                } catch (Exception ignored) { }
            }
            String nv = download(NATIVE_URL);
            try {
                if (nv != null && Integer.parseInt(nv.trim()) > NATIVE_VERSION)
                    js("window.__apkUpdate&&window.__apkUpdate(" + JSONObject.quote(RELEASES_URL) + ")");
            } catch (Exception ignored) { }
        }).start();
    }

    private void js(String code) {
        runOnUiThread(() -> { if (web != null) web.evaluateJavascript(code, null); });
    }

    class Bridge {
        /** lang: "en" o "es" */
        @JavascriptInterface
        public void speak(String text, String lang, float pitch, float rate) {
            if (!ttsReady || text == null) return;
            tts.setLanguage("es".equals(lang) ? es : EN);
            tts.setPitch(pitch);
            tts.setSpeechRate(rate);
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "t" + System.nanoTime());
        }

        @JavascriptInterface
        public void stop() { if (ttsReady) tts.stop(); }

        @JavascriptInterface
        public boolean ready() { return ttsReady; }

        @JavascriptInterface
        public boolean canListen() { return SpeechRecognizer.isRecognitionAvailable(MainActivity.this); }

        @JavascriptInterface
        public void listen() { runOnUiThread(MainActivity.this::startListening); }

        @JavascriptInterface
        public void exit() { runOnUiThread(MainActivity.this::finish); }

        @JavascriptInterface
        public void reload() { runOnUiThread(MainActivity.this::loadBest); }

        @JavascriptInterface
        public void checkNow() { runOnUiThread(MainActivity.this::checkUpdates); }

        @JavascriptInterface
        public int nativeVersion() { return NATIVE_VERSION; }

        @JavascriptInterface
        public void openUrl(String url) {
            runOnUiThread(() -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
            });
        }

        /* ----- tema del sistema y barras ----- */
        @JavascriptInterface
        public boolean systemDark() {
            return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        }

        @JavascriptInterface
        public void setBars(boolean dark) {
            runOnUiThread(() -> {
                getWindow().setStatusBarColor(Color.parseColor(dark ? "#0D1230" : "#2A4FE0"));
                getWindow().setNavigationBarColor(Color.parseColor(dark ? "#161D48" : "#F4F7FF"));
                web.setBackgroundColor(Color.parseColor(dark ? "#0D1230" : "#F4F7FF"));
                View d = getWindow().getDecorView();
                int f = d.getSystemUiVisibility();
                if (Build.VERSION.SDK_INT >= 26) {
                    if (dark) f &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR; else f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                d.setSystemUiVisibility(f);
            });
        }

        /* ----- recordatorio diario ----- */
        @JavascriptInterface
        public void setReminder(boolean on, int hour, int minute) {
            SharedPreferences p = getSharedPreferences(ReminderReceiver.PREFS, MODE_PRIVATE);
            p.edit().putBoolean("on", on).putInt("h", hour).putInt("m", minute).apply();
            if (on && Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF));
            }
            ReminderReceiver.schedule(MainActivity.this);
        }

        @JavascriptInterface
        public void markPracticed(String day) {
            getSharedPreferences(ReminderReceiver.PREFS, MODE_PRIVATE).edit().putString("practiced", day).apply();
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(ms);
        }
    }

    /* ---------- micrófono ---------- */
    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speechError("Este celular no tiene reconocimiento de voz.");
            return;
        }
        if (ttsReady) tts.stop();
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) { js("window.__onSpeechReady&&window.__onSpeechReady()"); }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float v) { }
            @Override public void onBufferReceived(byte[] b) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onError(int error) {
                speechError(error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ? "No te escuché bien. Probá de nuevo." : "No pude usar el micrófono. Probá de nuevo.");
            }
            @Override public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String t = (r != null && !r.isEmpty()) ? r.get(0) : "";
                js("window.__onSpeech&&window.__onSpeech(" + JSONObject.quote(t) + ")");
            }
            @Override public void onPartialResults(Bundle b) { }
            @Override public void onEvent(int t, Bundle b) { }
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        recognizer.startListening(i);
    }

    private void speechError(String msg) {
        js("window.__onSpeechErr&&window.__onSpeechErr(" + JSONObject.quote(msg) + ")");
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == REQ_MIC) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) startListening();
            else speechError("Sin permiso de micrófono. Activalo en Ajustes > Apps > Hablá Inglés.");
        }
        if (code == REQ_NOTIF && (res.length == 0 || res[0] != PackageManager.PERMISSION_GRANTED)) {
            js("window.__notifDenied&&window.__notifDenied()");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // si la app quedó abierta en segundo plano, vuelve a buscar novedades cada 30 minutos
        if (System.currentTimeMillis() - lastCheck > 30 * 60 * 1000L) checkUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ttsReady) tts.stop();
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        js("window.__back?window.__back():0");
    }
}
