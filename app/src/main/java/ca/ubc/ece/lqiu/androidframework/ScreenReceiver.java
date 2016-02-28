package ca.ubc.ece.lqiu.androidframework;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by lina on 16-02-22.
 * Observing #screen status and #user present actions
 */

public class ScreenReceiver extends BroadcastReceiver {
    private boolean screen_status;
    private boolean user_present;
    private String screenOn_timeStamp;
    private long user_present_timeStamp;

    public ScreenReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // Listen for screen on action and record the timeStamp for screenOn event
        // ACTION_SCREEN_ON: Broadcast Action, sent when the device wakes up and
        // become interactive.
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screenOn_timeStamp = new SimpleDateFormat(
                    "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                    .getInstance().getTime());
            screen_status = true;
        }
        // Listen for screen off action
        // ACTION_SCREEN_OFF: Broadcast Action, sent when the device goes to sleep
        // and becomes non-interactive.
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screen_status = false;
            user_present = false;
        }
        // Listen for user present action
        // ACTION_USER_PRESENT: Broadcast Action, sent when the user is present after
        // device wakes up (e.g when the keyguard is gone).
        // This is a protected intent that can only be sent by the system.
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            user_present_timeStamp = System.currentTimeMillis();
            user_present = true;
            screen_status = true;
        }

        Intent observeScreenStatusServiceIntent = new Intent(context, observeScreenStatusService.class);
        observeScreenStatusServiceIntent.putExtra("screen_state", screen_status);
        observeScreenStatusServiceIntent.putExtra("screenOn_timestamp", screenOn_timeStamp);
        observeScreenStatusServiceIntent.putExtra("user_present", user_present);
        observeScreenStatusServiceIntent.putExtra("user_present_timestamp", user_present_timeStamp);

        context.startService(observeScreenStatusServiceIntent);
    }

}
