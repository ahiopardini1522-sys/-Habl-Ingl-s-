package ar.hablaingles.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/** Widget de la pantalla de inicio: racha, XP de hoy y palabra del día. */
public class StreakWidget extends AppWidgetProvider {
    static final String PREFS = "widget";

    static RemoteViews views(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        int streak = p.getInt("streak", 0);
        v.setTextViewText(R.id.w_streak, "🔥 " + streak + (streak == 1 ? " día" : " días"));
        v.setTextViewText(R.id.w_xp, p.getInt("xp", 0) + "/" + p.getInt("goal", 20) + " XP");
        v.setTextViewText(R.id.w_word, p.getString("word", "hello"));
        v.setTextViewText(R.id.w_tr, p.getString("tr", "hola"));
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 3, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.w_root, pi);
        return v;
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, StreakWidget.class));
        for (int id : ids) m.updateAppWidget(id, views(c));
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, views(c));
    }
}
