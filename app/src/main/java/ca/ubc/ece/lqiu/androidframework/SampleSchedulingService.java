package ca.ubc.ece.lqiu.androidframework;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


import android.app.IntentService;
import android.content.Intent;

/**
 * Created by lina on 16-02-21.
 * This {@code IntentService} does the app's actual work.
 * {@code SampleAlarmReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */

public class SampleSchedulingService extends IntentService {
    public SampleSchedulingService() {
        super("SchedulingService");
    }

    String url = "http://study.csnow.ca/SubmitLogFile.aspx";

    // An ID used to post the notification.
    Uploader uploader;

    // onHandleIntent(): This method is invoked on the worker thread with a request to process.
    @Override
    protected void onHandleIntent(Intent intent) {

        uploader = new Uploader();

        // getApplicationContext(): Return the context of the single, global Application
        // object of the current process.
        if (uploader.prepareFilesToUpload(getApplicationContext())) {
            // Uploading logged files with device id if the wifi on
///////////////////////////////////////////////////////////////////////////////////////////
            // ???Where does it check whether WiFi is on???
            if (uploader.isConnectionFound(getApplicationContext())
                    && isConnected(url)) {

                UploadFilesTask uploaderTask = new UploadFilesTask(
                        getApplicationContext());
                // TODO(Lina): check what's wrong here.
                uploaderTask.execute(Installation.id(getApplicationContext()));

                DownloadTask downloaderTask = new DownloadTask(
                        getApplicationContext());
                
                // TODO(Lina): check what's wrong here.
                downloaderTask.execute(Installation.id(getApplicationContext()));
            }
        }
    }

    // Check whether is connected with the server (url) or not
    public boolean isConnected(String url) {
        URL serverURL;
        HttpURLConnection urlConnection = null;
        try {
            // Transfer String url to a URL variable, need to catch MalformedURLException()
            serverURL = new URL(url);
            urlConnection = (HttpURLConnection)serverURL.openConnection();
            // A perfect/good HTTP response is an HTTP_OK(200)
            return urlConnection.getResponseCode() == 200;
        } catch (MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            urlConnection.disconnect();
        }
        return false;
    }
}

