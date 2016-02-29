package ca.ubc.ece.lqiu.androidframework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by lina on 16-02-22.
 * This class is used to collect participant's demographic information.
 */
public class fillDemographicsActivity extends Activity{
    public final static String EXTRA_MESSAGE = "ca.ubc.ece.lqiu.androidframework.DEMOGRAPHIC_FINISHED";
    static final String LOGGING_DIRECTORY = "/android_logged_data";
    static final String UPLOADED_DIRECTORY = "/usage_pattern_study";
    static String AUTHENTICATION_ATTEMPTS_FILE = "";
    static String USER_SESSIONS_FILE = "";
    static final String USER_PROFILE = "/user_profile.txt";
    static final String POINTS = "/points.txt";
    static String demographics = "";
    static int validData = 0;
    static final int CHECK_DEVICE_ADMIN = 10;

    private DevicePolicyManager mgr = null;
    private ComponentName cn = null;
    private CheckBox deviceAdminBox = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demographic_page);

        cn = new ComponentName(this, DeviceAdminAndroidReceiver.class);
        mgr = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

        deviceAdminBox = (CheckBox) findViewById(R.id.deviceAdminCheckBox);
        deviceAdminBox.setChecked(mgr.isAdminActive(cn));

        // generate UUID
        Installation.id(getApplicationContext());
    }

    public void onDeviceAdminCheckboxClicked(final View view) {

        boolean checked = ((CheckBox) view).isChecked();
        if (checked) {
            ((CheckBox) view).setTextColor(Color.BLACK);
            Intent securitySettingsIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            // startActivityForResult(Intent intent, int requestCode):
            // intent: The intent to start
            // requestCode: If >= 0, this code will be returned in onActivityResult() when the activity exits.
/////////////////////////////////////////////////////////////////////////////////////////////
            // ???Do not understand why set CHECK_DEVICE_ADMIN equals to 10 here???
            startActivityForResult(securitySettingsIntent, CHECK_DEVICE_ADMIN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check device admin state
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHECK_DEVICE_ADMIN) {
            deviceAdminBox = (CheckBox) findViewById(R.id.deviceAdminCheckBox);
            deviceAdminBox.setChecked(mgr.isAdminActive(cn));
        }
    }

    public void openProgress(View view) {

        demographics = "";
        validData = 0;

        // Get demographics data
        CheckBox deviceAdminCheckBox = (CheckBox) findViewById(R.id.deviceAdminCheckBox);
        EditText email = (EditText) findViewById(R.id.email);
        EditText age = (EditText) findViewById(R.id.age);
        Spinner genderSpinner = (Spinner) findViewById(R.id.genderSpinner);
        Spinner educationSpinner = (Spinner) findViewById(R.id.educationSpinner);
        EditText job = (EditText) findViewById(R.id.job);

        // Validate input data
        validData += (deviceAdminCheckBox.isChecked()) ? 1 : 0;
        validData += (android.util.Patterns.EMAIL_ADDRESS.matcher(email
                .getText().toString()).matches()) ? 1 : 0;
        validData += (!age.getText().toString().isEmpty() && Integer
                .parseInt(age.getText().toString()) >= 19) ? 1 : 0;
        validData += (!genderSpinner.getSelectedItem().toString().isEmpty()) ? 1
                : 0;
        validData += (!educationSpinner.getSelectedItem().toString().isEmpty()) ? 1
                : 0;
        validData += (!job.getText().toString().isEmpty()) ? 1 : 0;

        if (validData == 6) {
            demographics += email.getText().toString() + ",";
            demographics += age.getText().toString() + ",";
            demographics += genderSpinner.getSelectedItem().toString() + ",";
            demographics += educationSpinner.getSelectedItem().toString() + ",";
            demographics += job.getText().toString() + ",";
            demographics += android.os.Build.VERSION.RELEASE + ","
                    + android.os.Build.MODEL + "\n";

            // Get current data
            String currentDay = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(Calendar.getInstance().getTime());

            // Prepare file name according to current date
            AUTHENTICATION_ATTEMPTS_FILE = "/auth-" + currentDay + ".txt";
            USER_SESSIONS_FILE = "/session-" + currentDay + ".txt";

            // create a folder in external storage directory
            createLoggingDirectory(LOGGING_DIRECTORY);

            // Go to main screen (progress state)
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(EXTRA_MESSAGE, "finished");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            finish();
            startActivity(intent);
        } else {
            TextView validatorTextView = (TextView) findViewById(R.id.validatorMessage);
            validatorTextView.setVisibility(View.VISIBLE);
            if(!deviceAdminCheckBox.isChecked())
            {
                deviceAdminCheckBox.setTextColor(Color.RED);
            }
        }
    }

    private boolean createLoggingDirectory(String directory) {
        File androidDirectory = new File(getApplicationContext().getFilesDir()
                + directory);

        // create directory if it dose not exist
        if (!androidDirectory.exists()) {

            androidDirectory.mkdir();

            new File(Environment.getExternalStorageDirectory() + UPLOADED_DIRECTORY).mkdir();

            // create authentication attempts file
            try {
                FileOutputStream out = new FileOutputStream(
                        androidDirectory.getPath()
                                + AUTHENTICATION_ATTEMPTS_FILE);
                out.write(demographics.getBytes());
                out.write("outcome,timestamp,method.type\n".getBytes());
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // create user sessions file
            try {
                FileOutputStream out = new FileOutputStream(
                        androidDirectory.getPath() + USER_SESSIONS_FILE);
                out.write("record.type,application.name,timestamp,method.type\n"
                        .getBytes());
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Create user profile with a unique id
            try {
                FileOutputStream out = new FileOutputStream(
                        androidDirectory.getPath() + USER_PROFILE);
                out.write(Installation.id(getApplicationContext()).getBytes());
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Create points file, which is for recording the progress of the study
            try {
                FileOutputStream out = new FileOutputStream(
                        androidDirectory.getPath() + POINTS);
                out.write("1".getBytes());
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // load public key from assets; the key will be used to encrypt the logged data,
            // in order to upload all data secretly to the server.
            try {
                InputStream pubKeyInputStream = getAssets().open("rsa.pub");
                OutputStream pubKeyOutputStream = new FileOutputStream(
                        new File(androidDirectory.getPath() + "/rsa.pub"));

                byte[] buffer = new byte[1024];

                int length;
                while ((length = pubKeyInputStream.read(buffer)) > 0) {
                    pubKeyOutputStream.write(buffer, 0, length);
                }

                pubKeyInputStream.close();
                pubKeyOutputStream.close();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return true;
    }
}
