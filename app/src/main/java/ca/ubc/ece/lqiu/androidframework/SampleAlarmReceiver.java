package ca.ubc.ece.lqiu.androidframework;

/**
 * Created by lina on 16-02-21.
 * This file is created for setting a daily alarm to invoke the app
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import java.util.Calendar;

public class SampleAlarmReceiver extends WakefulBroadcastReceiver{

    private AlarmManager alarmManager;
    private PendingIntent pendingAlarmIntent;

    @Override
    public void onReceive(Context context, Intent intent){
        Intent service = new Intent(context, SampleSchedulingService.class);
        startWakefulService(context, service);
    }

    // BEGIN_INCLUDE(set_alarm)
    // The Alarm Manager is intended for cases where you want to have your application code run
    // at a specific time, even if your application is not currently running.
    // You do not instantiate this class directly; instead, retrieve it through
    // Context.getSystemService(Context.ALARM_SERVICE).
    // Alarms (based on the AlarmManager class) give you a way to perform time-based operations
    // outside the lifetime of your application.
    /**
     * Sets a repeating alarm that runs once a day at approximately 12:15 a.m.
     * When the alarm fires, the app broadcasts an Intent to this
     * WakefulBroadcastReceiver.
     *
     * @param context
     */
    public void setAlarm(Context context) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SampleAlarmReceiver.class);
        pendingAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // getInstance() method returns a calendar whose locale is based on system settings
        // and whose time fields have been initialized with the current date and time.
        Calendar calendar = Calendar.getInstance();
        // Sets the time of this calendar to the given Unix time
        calendar.setTimeInMillis(System.currentTimeMillis());
        // Set the alarm's trigger time to 12:15 a.m.
        calendar.set(Calendar.HOUR_OF_DAY, 00);
        calendar.set(Calendar.MINUTE, 15);

        // Schedule a repeating alarm that has inexact trigger time requirements;
        // e.g. an alarm that repeats every hour, but not necessarily at the top of every hour.
        // RTC_WAKEUP: Wakes up the device to fire the pending intent at the specified time
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY,
                pendingAlarmIntent);

        /*
         * The receiver will not be called unless the application explicitly enables it, which
         * prevents the boot receiver from being called unnecessarily. Below is the code to
         * enable a receiver, if the user sets an alarm.
         */
        ComponentName receiver = new ComponentName(context,
                SampleBootReceiver.class);
        // PackageManager: Class for retrieving various kinds of information related to
        // the application packages that are currently installed on the device.
        // You can find this class through getPackageManager()
        PackageManager pm = context.getPackageManager();

        // COMPONENT_ENABLED_STATE_ENABLED: Flag for setApplicationEnabledSetting(String, int, int)
        // and setComponentEnabledSetting(ComponentName, int, int)
        // This component/application has been explicitly enabled, regardless of what it has
        // specified in its manifest.
        // DONT_KILL_APP: Flag parameter for setComponentEnabledSetting(ComponentName, int, int)
        // to indicate that you don't want to kill the app containing the component
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    // END_INCLUDE(set_alarm)

    /**
     * Cancels the alarm.
     *
     * @param context
     */
    // BEGIN_INCLUDE(cancel_alarm)
    public void cancelAlarm(Context context) {
        // If the alarm has been set, cancel it.
        if (alarmManager != null) {
            alarmManager.cancel(pendingAlarmIntent);
        }

        // Disable {@code SampleBootReceiver} so that it doesn't automatically
        // restart the alarm when the device is rebooted.
        ComponentName receiver = new ComponentName(context,
                SampleBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
    // END_INCLUDE(cancel_alarm)
}
