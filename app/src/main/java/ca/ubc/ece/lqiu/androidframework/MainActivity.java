package ca.ubc.ece.lqiu.androidframework;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "ca.ubc.ece.lqiu.androidframework.DEMOGRAPHIC";
    static final String LOGGING_DIRECTORY = "/android_logged_data";
    static String AUTHENTICATION_ATTEMPTS_FILE = "";
    static final String POINTS = "/points.txt";
    static String APPLICATION_DIRECTORY_PATH = "";

    ProgressBar progressBar;
    int progressValue=1;

    //uploadingCounter used to count how many days the study has been done
    int uploadingCounter = 0;
    CheckBox checkBox;
    //How many days remains for the study
    TextView progressHintTitle;

    SampleAlarmReceiver alarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get application directory path
        APPLICATION_DIRECTORY_PATH = getApplicationContext().getFilesDir()+LOGGING_DIRECTORY;

        // If /points.txt file (which records the study progress) exists, load the progressBar
        // to indicate participants how many days they have been involved in this research.
        if (new File(APPLICATION_DIRECTORY_PATH+POINTS).exists()){
            setContentView(R.layout.check_progress_page);

            uploadingCounter = Integer.parseInt(loadFileData(new File(APPLICATION_DIRECTORY_PATH+POINTS)));

            // Load progress value (the progress value is a percentage 100%*uploadingCounter/30)
            progressValue = (int) (uploadingCounter * 3.33);

            // Set progress
            progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setProgress(progressValue);

            progressHintTitle = (TextView) findViewById(R.id.progressHintTitle);
            if(uploadingCounter == 30){
                progressHintTitle.setText("Thank you for participating, you have been entered to the raffle");
            } else {
                progressHintTitle.setText((30-uploadingCounter) + "day(s) remaining to finish the study");
            }

            // create and set wake alarm object
            alarm = new SampleAlarmReceiver();
            alarm.setAlarm(this);

            // launch android service, using observeScreenStatusService to replace Ahmed's AndroidService
            Intent startBroadcastIntent = new Intent(this, observeScreenStatusService.class);
            startService(startBroadcastIntent);
        }
        // If no /points.txt file founded, means this is the first time that user runs our app, so we are
        // going to load the main screen, and show user the consent form first.
        else {
            // load main screen
            setContentView(R.layout.content_main);

            // show consent form
            WebView consentWebView = (WebView) findViewById(R.id.consentWebView);
            consentWebView.loadUrl("file:///android_asset/consent.htm");
            // to allow user to zoom in/out the consent form
            consentWebView.getSettings().setBuiltInZoomControls(true);
            consentWebView.getSettings().setSupportZoom(true);
            // Lina adds this line
            consentWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }
    public void openDemographics(View view){
        // open demographics screen
        // use fillDemographicsActivity to replace Ahmed's Demographics, to indicate that this is an activity
        Intent openDemographicsIntent = new Intent(this, fillDemographicsActivity.class);

        // participant accepts the consent form, so further to next step, fillDemographics activity.
        openDemographicsIntent.putExtra(EXTRA_MESSAGE, "accept");
        finish();
        startActivity(openDemographicsIntent);
    }

    // Participant decides to decline the consent form, and is directed to choose close the app or not.
    public void closeApp(View view){
        // dialog asks user to confirm app closing
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setMessage(R.string.exitHint).setTitle(R.string.appClosingDialogTitle);

        // reply to user's confirmation of closing the app
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // user clicked OK button
                System.exit(0);
            }
        });

        // reply to user's cancellation of closing the app
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // user cancelled the action to close the app; dismiss the app closing confirmation dialog
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Check whether participants withdraw their participation during the study or not.
    public void onCheckboxClicked(final View view){
        // Handle checkbox status
        boolean withdrawCheckBoxChecked = ((CheckBox) view).isChecked();

        final String timestamp = new SimpleDateFormat("yyyy-MM-dd 'T' hh:mm:ss.SSS a", Locale.US).format(Calendar.getInstance().getTime());
        // prepare the path for files which record all logged data
        prepareLoggedFiles();

        // If participants decide to withdraw their participation in the research during the study
        if(withdrawCheckBoxChecked){
            // If withdrawCheckBox is checked, popup the dialog to ask user to confirm app closing
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setMessage(R.string.withdrewHint).setTitle(R.string.appClosingDialogTitle);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    // User clicked OK button
                    //////////// Why not record "User withdraw" in session file??? //////////////
                    logging_data(APPLICATION_DIRECTORY_PATH
                                    + AUTHENTICATION_ATTEMPTS_FILE,
                            "User withdrew: " + timestamp + "\n");
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    // Reset the withdrawCheckBoxChecked status to false
                    ((CheckBox) view).setChecked(false);
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Construct a method to help load data string from a specified file
    public String loadFileData(File file){
        String data ="";
        InputStream inputStream = null;
        byte[] buffer = null;
        try{
            inputStream = new BufferedInputStream(new FileInputStream(file));
            int size = inputStream.available();
            buffer = new byte[size];
            // Read all data available in the file currently one-time directly
            inputStream.read(buffer);
            inputStream.close();

            data = new String(buffer);

        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return data;
    }

    void prepareLoggedFiles(){
        // Prepare file name according to current date
        // Because upload loggedData file once a day, so only need to record the current date.
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

        AUTHENTICATION_ATTEMPTS_FILE = "/auth-"+ currentDate + ".txt";
    }

    //////////////// ??? Why define this as a private method()???////////////////////////
    private void logging_data(String filePath, String data){
        try {
            OutputStream outputFile = new FileOutputStream(filePath, true);
            outputFile.write(data.getBytes());
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
