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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;

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

    // _outcome, _timeStamp and _method_type are used to store the parameters for function logging_auth_model_Data(Authentication_Model _auth_model, String _outcome, String _timeStamp, String _method_type, String filePath)
    String _outcome = null;
    String _timeStamp = null;
    String _method_type = null;
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
                // get record of data (outcome, time stamp, authentication method)
                _outcome = intent.getStringExtra("passFailed");

                // update and logging data
                _timeStamp = new SimpleDateFormat(
                        "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                        .getInstance().getTime());
                _method_type = getAuthenticationMethodType();
                logging_auth_model_Data(_auth_model, _outcome, _timeStamp, _method_type, APPLICATION_DIRECTORY_PATH
                        + AUTHENTICATION_ATTEMPTS_FILE);
            } else {
                // get all status from screen receiver (screen on, off or user present)
                screenOn = intent.getBooleanExtra("screen_state", false);
                user_present = intent.getBooleanExtra("user_present", false);

                // screen switched on and user going to unlock device
                if (screenOn && !user_present) {

                    _outcome = "Screen On";
                    _timeStamp = new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime());
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        locked = ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()? "true" : "false";
                    }
                    _method_type = locked;
                    logging_auth_model_Data(_auth_model, _outcome, _timeStamp, _method_type, APPLICATION_DIRECTORY_PATH
                            + AUTHENTICATION_ATTEMPTS_FILE);
                }

                /*Do not understand very clearly. Need to read again.*/
                // screen switched off
                if (!screenOn) {
                    _outcome = "Screen Off";
                    _timeStamp = new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime());


                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        locked = ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()? "true" : "false";
                    }
                    int screen_off_time_out = Settings.Secure.getInt(getContentResolver(), "lock_screen_lock_after_timeout", 0);
                    _method_type= locked + "," + screen_off_time_out;
                    logging_auth_model_Data(_auth_model, _outcome, _timeStamp, _method_type, APPLICATION_DIRECTORY_PATH
                            + AUTHENTICATION_ATTEMPTS_FILE);
                }

                // screen on and user unlocked device "new session start"
                if (screenOn && user_present) {

                    user_present_timestamp = intent.getLongExtra(
                            "user_present_timestamp", 0);


                    // successful authentication happened
                    _outcome = "Successful";
                    // The start _timeStamp of this session
                    String session_start_time = new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime());
                    _method_type = getAuthenticationMethodType();
                    logging_auth_model_Data(_auth_model, _outcome, session_start_time, _method_type, APPLICATION_DIRECTORY_PATH
                            + AUTHENTICATION_ATTEMPTS_FILE);

                    // logging session's start time stamp
                    logging_sess_data("Session start,," + session_start_time + "," + _method_type + "\n", APPLICATION_DIRECTORY_PATH
                            + USER_SESSIONS_FILE);

                    // To direct the user to the settings page, in order to get user authorization to request the stats.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                        Intent usageAccessSettingsIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(usageAccessSettingsIntent);
                    }

                    // start foreground application observer thread
                    appName = "";
                    foregroundRunningApps = "";
                    mCheckForegroundRunnable.run();
                }

                // screen has been locked
///////////////////////////////////////////////////////////////////////////////////////
                // ???Why user_present_timestamp!=0 Does press lock button count???
                if (!screenOn && !user_present && user_present_timestamp != 0) {
                    user_present_timestamp = 0;
                    // Logging foreground running apps
                    logging_sess_data(foregroundRunningApps, APPLICATION_DIRECTORY_PATH
                            + USER_SESSIONS_FILE);

                    // Because screen has been locked, we need to remove all post runnables
                    mForegroundHandler.removeCallbacks(mCheckForegroundRunnable);

                    // Record session_end_time here
                    String session_end_time  = new SimpleDateFormat(
                            "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                            .getInstance().getTime());
                    // logging session's end time stamp
                    logging_sess_data("Session end,," + session_end_time + "\n", APPLICATION_DIRECTORY_PATH
                            + USER_SESSIONS_FILE);
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
     * Logging session data in specified path
     *
     * @param data that will be logged in the file
     *
     * @param filepath path of the file that stores logged data
     *
     */
    private void logging_sess_data(String data, String filePath) {
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

    /*
    * Logging authentication data in specified path
    *
    * @param _auth_model a specific Authentication_Model data structure
    *
    * @param _outcome outcome of unlocking/screenStatus
    *
    * @param _timeStamp currently time stamp
    *
    * @param _method_type unlocking method / whether Keyguard is removed or not
    *
    * @param filepath path of the file that stores logged data
    *
    */
    private void logging_auth_model_Data(Authentication_Model _auth_model, String _outcome, String _timeStamp, String _method_type, String filePath){
        _auth_model.set_outcome(_outcome);
        _auth_model.set_timeStamp(_timeStamp);
        _auth_model.set_method_type(_method_type);
        _auth_model.logging_data(filePath);
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Get currently running foreground app name
    private String getForegroundAppName() {
        // TODO(Lina): to replace this deprecated function getRunningTasks() with a similar one to accomplish this function.
        // Issues met:
        // Someone suggested to use getAppRunningProcess(), but this no longer work with Update 5.1.1. The method will only return your own package processes.
        // Someone suggested to use UsageStatsManager, but it will need user authorization to request the stats. It is documented that there may not be a system
        // activity to handle the Settings.ACTION_USAGE_ACCESS_SETTINGS intent. LG are among the manufacturers who have removed this activity from their
        // Lollipop builds, so this solution will never work on their devices.
        // Another solution: Use an AccessibilityService. to be discussed.
        // getRunningTasks(int maxNum): return a list of the tasks that are currently running,
        // with the most recent being first and older ones after in order.
        // maxNum: the maximum number of entries to return in the list.
        String appName = "";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // USAGE_STATS_SERVICE is added in API level 22, not 21. Seems a GAP in API level 21.
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST,
                    time - 1000 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();

                // Sort all applications used in last 1000s by the time they were last used
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(),
                            usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    appName = mySortedMap.get(
                            mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            // getRunningAppProcesses() is deprecated in API level 21
            ActivityManager taskManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = taskManager.getRunningAppProcesses();
            appName = tasks.get(0).processName;
        }

        return appName;
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
            if (getForegroundAppName().equals(appName)) {
                appEndTimeStamp = new SimpleDateFormat(
                        "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                        .getInstance().getTime());
                if (appName.equals("")) {
                    foregroundRunningApps += "Application background,"
                            + appName + "," + appEndTimeStamp + "\n";
                }
                // Change appName to currently running application
                appName = getForegroundAppName();

                appStartTimeStamp = new SimpleDateFormat(
                        "yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar
                        .getInstance().getTime());
                foregroundRunningApps += "Application foreground," + appName
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





