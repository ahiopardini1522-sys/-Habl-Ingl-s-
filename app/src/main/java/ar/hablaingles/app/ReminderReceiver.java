package ar.hablaingles.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/** Recordatorio diario: avisa a la hora elegida, salvo que ese día ya hayas practicado. */
public class ReminderReceiver extends BroadcastReceiver {
    static final String PREFS = "recordatorio";
    static final String CHANNEL = "recordatorio_diario";

    static String today() {
        return new SimpleDateFormat("yyyy-M-d", Locale.US).format(new Date());
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        if (!p.getBoolean("on", false)) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, p.getInt("h", 19));
        cal.set(Calendar.MINUTE, p.getInt("m", 0));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean("on", false)) return;
        if (today().equals(p.getString("practiced", ""))) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Recordatorio diario", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Un aviso por día para practicar inglés");
            nm.createNotificationChannel(ch);
        }
        String[][] msgs = {
            {"¡Pico te espera! 🐦", "Hacé tu lección de hoy y mantené la racha."},
            {"Es hora de practicar inglés", "Solo 5 minutos y cumplís tu meta del día."},
            {"¡No pierdas tu racha! 🔥", "Una lección rápida y seguís sumando días."},
            {"Hello! 👋", "Pico preparó palabras nuevas para vos."}
        };
        String[] m = msgs[new Random().nextInt(msgs.length)];
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notif)
         .setColor(0xFF2F5BEA)
         .setContentTitle(m[0])
         .setContentText(m[1])
         .setContentIntent(pi)
         .setAutoCancel(true);
        try { nm.notify(10, b.build()); } catch (SecurityException ignored) { }
    }
}
