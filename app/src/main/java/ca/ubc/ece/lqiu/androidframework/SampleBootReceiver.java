package ca.ubc.ece.lqiu.androidframework;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by lina on 16-02-21.
 * This BroadcastReceiver automatically (re)starts the alarm when the device is
 * rebooted. This receiver is set to be disabled (android:enabled="false") in the
 * application's manifest file. When the user sets the alarm, the receiver is enabled.
 * When the user cancels the alarm, the receiver is disabled, so that rebooting the
 * device will not trigger this receiver.
 */

/**
 * BroadcastReceiver is a Base class for code that will receive intents sent by sendBroadcast().
 * A BroadcastReceiver object is only valid for the duration of the call to onReceiver(Context, Intent).
 * Once your code returns from this function, the system considers the object to be finished and no longer active.
 * To bind to a service from within a BroadcastReceiver, using Context.startService() to send a command to the service.
 */

/**
 * By default, all alarms are canceled when a device shuts down.
 */

// BEGIN_INCLUDE(auto-start)
public class SampleBootReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent){
        SampleAlarmReceiver alarm = new SampleAlarmReceiver();
        // If BOOT_COMPLETED, set alarm, and bind to observeScreenStatusService()
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            alarm.setAlarm(context);
            Intent observeScreenStatusServiceIntent = new Intent(context, observeScreenStatusService.class);
            context.startService(observeScreenStatusServiceIntent);
        }
    }
}
