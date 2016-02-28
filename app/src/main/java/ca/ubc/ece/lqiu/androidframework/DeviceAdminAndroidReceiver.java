package ca.ubc.ece.lqiu.androidframework;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by lina on 16-02-22.
 * Observing successful and unsuccessful authentication.
 */

public class DeviceAdminAndroidReceiver extends DeviceAdminReceiver {

    /////////////////////////////////////////////////////////////////////////////////////////////
    // ???Why have these two String here??? For what???
    static final String LOCK_PATTERN_FILE = "/system/gesture.key";
    static final String LOCK_PASSWORD_FILE = "/system/password.key";

    public DeviceAdminAndroidReceiver() {
    }

    /*
     * A toast provides simple feedback about an operation in a small popup. It only fills the
     * amount of space required for the message and the current activity remains visible and
     * interactive. To instantiate a Toast object with makeText(), three parameters are needed.
     * the application Context, the text message, and the duration for the toast.
     * Use show() to display the toast notification.
     */
    void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    // onEnabled(Context context, Intent intent): Called after the administrator is first enabled,
    // as a result of receiving ACTION_DEVICE_ADMIN_ENABLED.
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
    }

    // onDisabled(Context context, Intent intent): Called prior to the administrator being disabled,
    // as a result of receiving ACTION_DEVICE_ADMIN_DISABLED.
    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }

    // onReceive(Context context, Intent intent): Intercept standard device administrator broadcasts.
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    // onPasswordFailed(Context context, Intent intent): Called after the user has failed at
    // entering their current password, as a result of receiving ACTION_PASSWORD_FAILED.
    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);

        // Send data to android service
        Intent pass_field_intent = new Intent(context, observeScreenStatusService.class);
        pass_field_intent.putExtra("passFailed", "Unsuccessful");
        context.startService(pass_field_intent);
    }

    // onPasswordSucceeded(Context context, Intent intent): Called after the user has succeeded
    // at entering their current password, as a result of receiving ACTION_PASSWORD_SUCCEEDED.
    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
    }

}
