package ar.hablaingles.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Al reiniciar el celular, vuelve a programar el recordatorio. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) ReminderReceiver.schedule(c);
    }
}
