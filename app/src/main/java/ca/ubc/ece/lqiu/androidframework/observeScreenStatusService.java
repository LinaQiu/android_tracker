package ca.ubc.ece.lqiu.androidframework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;

/**
 * Created by lina on 16-02-21.
 * Background service:
 * - Register screen status and user present actions
 * - Observing user action
 */
public class observeScreenStatusService extends Service{
    static final String LOGGING_DIRECTORY = "/android_logged_data";
    static String AUTHENTICATION_ATTEMPTS_FILE = "";
    static String USER_SESSIONS_FILE = "";
    static final String LOCK_PATTERN_FILE = "/system/gesture.key";
    static final String LOCK_PASSWORD_FILE = "/system/password.key";
    static String APPLICATION_DIRECTORY_PATH = "";
    private BroadcastReceiver mScreenReceiver;
    private boolean screenOn = false;
    private boolean user_present = false;
    private long user_present_timestamp = 0;
    // New thread for observing foreground running app
    private Handler mForegroundHandler;
    private long mInterval = 500;
    String appStartTimeStamp = "";
    String appEndTimeStamp = "";
    // foregroundRunningApps records all foreground apps during the whole session
    String foregroundRunningApps = "";
    // appName stores currently/previously running foreground app name
    String appName = "";
    // appInfo stores currently/previously running foreground app info.
    String appInfo = "";
    String locked = "NA";
    private Authentication_Model _auth_model;

    public observeScreenStatusService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Register screen on, screen off and user present actions
        // IntentFilter(String action): New IntentFilter that matches a single action with no data
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);

        // addAction(String action): Add a new Intent action to match against.
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mScreenReceiver = new ScreenReceiver();

        // registerReceiver(BroadcastReceiver receiver, IntentFilter filter):
        // Register a BroadcastReceiver to be run in the main activity thread. The receiver will
        // be called with any broadcast Intent that matches filter, in the main application thread.
        registerReceiver(mScreenReceiver, filter);

        // instantiate a new thread for observing foreground apps
        mForegroundHandler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenReceiver);
        if (mCheckForegroundRunnable != null) {
            // removerCallbacks(Runnable r): remove any pending posts of Runnable r
            // that are in the message queue.
            mForegroundHandler.removeCallbacks(mCheckForegroundRunnable);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // prepare logged files to be in current day (set file path according to current day date)
        prepareLoggedFiles();

        // Get application directory path;
        // getFilesDir() returns the path of the directory holding application files
        APPLICATION_DIRECTORY_PATH = getApplicationContext().getFilesDir() + LOGGING_DIRECTORY;

        // Authentication_Model() defines a data structure,
        // which records authentication_attempts related data
        _auth_model = new Authentication_Model();

        if (intent != null) {
            // Distinguish authentication failed attempts and regular authentication
            // attempts, screenOn, screenOff, userPresent.
            // auth_attempt intent
            if (intent.getStringExtra("passFailed") != null) {
                // get record of data (outcome, time stamp, authentication
                // method)
                String passFailed = intent.getStringExtra("passFailed");

                // update and logging data
                _auth_model.set_outcome(passFailed);
                _auth_model.set_timeStamp(new SimpleDateFormat(
                        "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                        .getInstance().getTime()));
                _auth_model.set_method_type(getAuthenticationMethodType());
                _auth_model.logging_data(APPLICATION_DIRECTORY_PATH
                        + AUTHENTICATION_ATTEMPTS_FILE);
            } else {
                // get all status from screen receiver (screen on, off or user present)
                screenOn = intent.getBooleanExtra("screen_state", false);
                user_present = intent.getBooleanExtra("user_present", false);

                // screen switched on and user going to unlock device
                if (screenOn && !user_present) {

                    _auth_model.set_outcome("Screen On");
                    _auth_model.set_timeStamp(new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime()));
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        locked = ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()? "true" : "false";
                    }
                    _auth_model.set_method_type(locked);
                    _auth_model.logging_data(APPLICATION_DIRECTORY_PATH
                            + AUTHENTICATION_ATTEMPTS_FILE);
                }

                /*Do not understand very clearly. Need to read again.*/
                // screen switched off
                if (!screenOn) {
                    _auth_model.set_outcome("Screen Off");
                    _auth_model.set_timeStamp(new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime()));

                    try {
////////////////////////////////////////////////////////////////////////////////////////
                        //??????Why sleep for 1000ms?????
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        locked = ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()? "true" : "false";
                    }
                    int screen_off_time_out = Settings.Secure.getInt(getContentResolver(), "lock_screen_lock_after_timeout", 0);
                    _auth_model.set_method_type( locked + "," + screen_off_time_out);
                    _auth_model.logging_data(APPLICATION_DIRECTORY_PATH
                            + AUTHENTICATION_ATTEMPTS_FILE);
                }

                // screen on and user unlocked device "new session start"
                if (screenOn && user_present) {

                    user_present_timestamp = intent.getLongExtra(
                            "user_present_timestamp", 0);
                    String session_start_time = new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime());

                    // successful authentication happened
                    _auth_model.set_outcome("Successful");
                    _auth_model.set_timeStamp(session_start_time);
                    _auth_model.set_method_type(getAuthenticationMethodType());
                    _auth_model.logging_data(APPLICATION_DIRECTORY_PATH
                            + AUTHENTICATION_ATTEMPTS_FILE);

                    // logging session's start time stamp
                    logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE,
                            "Session start,," + session_start_time + ","
                                    + getAuthenticationMethodType() + "\n");

                    // start foreground application observer thread
                    appName = "";
                    appInfo = "";
                    foregroundRunningApps = "";
                    mCheckForegroundRunnable.run();
                }

                // screen has been locked
///////////////////////////////////////////////////////////////////////////////////////
                // ???Why user_present_timestamp!=0 Does press lock button count???
                if (!screenOn && !user_present && user_present_timestamp != 0) {
                    user_present_timestamp = 0;
                    // Logging foreground running apps
                    logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE,
                            foregroundRunningApps);
                    // Because screen has been locked, we need to remove all post runnables
                    mForegroundHandler.removeCallbacks(mCheckForegroundRunnable);

                    // Record session_end_time here
                    String session_end_time = new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime());
                    // logging session's end time stamp
                    logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE,
                            "Session end,," + session_end_time + "\n");
                }
            }
        }

        // START_STICKY: Constant to return from onStartCommand(Intent, int, int), if this
        // service's process is killed while it is started, then leave it in the started state
        // but don't retain this delivered intent.
        // It makes sense for things that will be explicitly started and stopped to run for
        // arbitrary periods of time, such as a service performing background music playback.
        return START_STICKY;
    }

    /*
     * Determine authentication method type (DAS, PIN or Password)
     */
    private String getAuthenticationMethodType() {

        boolean havePattern = nonEmptyFileExists(Environment.getDataDirectory()
                .getAbsolutePath() + LOCK_PATTERN_FILE);
        boolean havePassword = nonEmptyFileExists(Environment
                .getDataDirectory().getAbsolutePath() + LOCK_PASSWORD_FILE);

        if (havePattern) {
            return "DAS";
        } else if (havePassword) {
            return "Password";
        } else {
            return "None";
        }
    }

    /*
     * Check file exist and its length
     */
    private static boolean nonEmptyFileExists(String filename) {
        File file = new File(filename);
        return file.exists() && file.length() > 0;
    }

    /*
     * Logging data in specified path
     *
     * @param filepath path of the file that stores logged data
     *
     * @param data that will be logged in the file
     */
    private void logging_data(String filePath, String data) {
        try {
            FileOutputStream out = new FileOutputStream(filePath, true);
/////////////////////////////////////////////////////////////////////////////////////
            // ???Check function getBytes()???How it works.
            out.write(data.getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Get currently running foreground app name
    private String getForegroundAppName() {
        String appName = "";
        ActivityManager taskManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        // getRunningTasks(int maxNum): return a list of the tasks that are currently running,
        // with the most recent being first and older ones after in order.
        // maxNum: the maximum number of entries to return in the list.
        RunningTaskInfo foreGroundTaskInfo = taskManager.getRunningTasks(1)
                .get(0);
        // topActivity: the activity component at the top of the history stack of the task.
        String packageName = foreGroundTaskInfo.topActivity.getPackageName();
        PackageManager pManager = getApplicationContext().getPackageManager();
        try {
            //getApplicationInfo(): return the full application info for this context's package
            appName = (String) pManager.getApplicationLabel(pManager
                    .getApplicationInfo(packageName, 0));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }

    // Get currently running foreground app info., including its name.
    // ???Why need two almost similar function: getForegroundAppName() and getForegroundAppInfo()
    private String getForegroundAppInfo() {
        String appName = "";
        ActivityManager taskManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ApplicationInfo appInfoString = null;

        RunningTaskInfo foreGroundTaskInfo = taskManager.getRunningTasks(1)
                .get(0);
        String packageName = foreGroundTaskInfo.topActivity.getPackageName();
        PackageManager pManager = getApplicationContext().getPackageManager();
        try {
            appName = (String) pManager.getApplicationLabel(pManager
                    .getApplicationInfo(packageName, 0));
            appInfoString = pManager.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appInfoString.packageName + ":" + appName;
    }

    /*
     * Observing foreground apps in a separated thread to check foreground app
     * every 5ms
     */
    // The Runnable interface should be implemented by any class whose instances are
    // intended to be executed by a thread. The class must define a method of no
    // arguments called run().
    Runnable mCheckForegroundRunnable = new Runnable() {

        @Override
        public void run() {
            // If currently running app is not the same as previous foreground app,
            // then record the appEndTimeStamp for previous foreground app.
            // It is possible that there is no previous foreground app, which is
            // corresponding to appName="" situation.
            if (getForegroundAppName() != appName) {
                appEndTimeStamp = new SimpleDateFormat(
                        "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                        .getInstance().getTime());
                if (appName != "") {
                    foregroundRunningApps += "Application background,"
                            + appInfo + "," + appEndTimeStamp + "\n";
                }
                // Change appName to currently running application
                appName = getForegroundAppName();
                appInfo = getForegroundAppInfo();
                appStartTimeStamp = new SimpleDateFormat(
                        "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                        .getInstance().getTime());
                foregroundRunningApps += "Application foreground," + appInfo
                        + "," + appStartTimeStamp + "\n";
            }
            // postDelayed(Runnable r, long delayMillis): causes the Runnable r to be
            // added to the message queue, to be run after the specified amount of time
            // elapses. The runnable will be run on the thread to which this handler is attached.
            mForegroundHandler.postDelayed(mCheckForegroundRunnable, mInterval);
        }
    };

    void prepareLoggedFiles() {
        // Prepare file name according to current date
        String currentDay = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar
                .getInstance().getTime());

        AUTHENTICATION_ATTEMPTS_FILE = "/auth-" + currentDay + ".txt";
        USER_SESSIONS_FILE = "/session-" + currentDay + ".txt";
    }
}





