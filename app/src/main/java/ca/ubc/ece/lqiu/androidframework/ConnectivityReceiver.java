package ca.ubc.ece.lqiu.androidframework;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;

/**
 * Created by lina on 16-02-22.
 * Observing connectivity devices (Wifi, Bluetooth, NFC and GPS) status
 */

public class ConnectivityReceiver extends BroadcastReceiver {

    static final String LOGGING_DIRECTORY = "/android_logged_data";
    static String AUTHENTICATION_ATTEMPTS_FILE = "";
    static String USER_SESSIONS_FILE = "";
    static String APPLICATION_DIRECTORY_PATH = "";

    public ConnectivityReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // prepare logged files to be in current day
        prepareLoggedFiles();

        // Get application directory path
        APPLICATION_DIRECTORY_PATH = context.getFilesDir() + LOGGING_DIRECTORY;

        String action = intent.getAction();
        int state = -1;
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US)
                .format(Calendar.getInstance().getTime());

        // WIFI_STATE_CHANGED_ACTION: Broadcast intent action indictaing that Wi-Fi has been
        // enabled, disabled, enabling, disabling, or unknown. One extra provides this state
        // as an int. Another extra provides the previous state, if available.
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            // int getIntExtra(String name, int defaultValue): Retrieve extended data from the
            // intent. Returns the value of an item that previously added with putExtra() or
            // the default value if none was found.
            // EXTRA_WIFI_STATE: The lookup key for an int that indicates whether Wi-Fi is enabled,
            // disabled, enabling, disabling, or unknown.
            state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            if (state == 3) {
                logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE, "Wifi On,,"
                        + timeStamp + "\n");

///////////////////////////////////////////////////////////////////////////////////////////
                // ???Why sleep for 10000s???
                // Check to upload if there is a connection
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
//////////////////////////////////////////////////////////////////////////////////////////
                // When Wi-Fi is on, start SampleSchedulingService, to upload files?
                Intent service = new Intent(context, SampleSchedulingService.class);
                context.startService(service);

            } else if (state == 1) {
                logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE,
                        "Wifi Off,," + timeStamp + "\n");
            }
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equalsIgnoreCase(action)) {
            state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (state == 12) {
                logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE,
                        "Bluetooth On,," + timeStamp + "\n");
            } else if (state == 10) {
                logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE,
                        "Bluetooth Off,," + timeStamp + "\n");
            }
        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2 && NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
            state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, -1);
            if (state == 3) {
                logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE, "NFC On,,"
                        + timeStamp + "\n");
            } else if (state == 1) {
                logging_data(APPLICATION_DIRECTORY_PATH + USER_SESSIONS_FILE, "NFC Off,,"
                        + timeStamp + "\n");
            }
        }
    }

    /*
     * Logging data in specified path
     * @filepath: file path for data logging
     * @data: data the will be logged in the file
     */
    private void logging_data(String filePath, String data) {
        try {
            FileOutputStream out = new FileOutputStream(filePath, true);
            out.write(data.getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void prepareLoggedFiles()
    {
        // Prepare file name according to current date
        String currentDay = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar
                .getInstance().getTime());

        AUTHENTICATION_ATTEMPTS_FILE = "/auth-" + currentDay + ".txt";
        USER_SESSIONS_FILE ="/session-" + currentDay + ".txt";
    }

}

