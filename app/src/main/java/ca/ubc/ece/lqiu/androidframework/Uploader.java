package ca.ubc.ece.lqiu.androidframework;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by lina on 16-02-22.
 * This class is used to upload logged files from the app to server.
 */
public class Uploader {

    static final String LOGGING_DIRECTORY = "/android_logged_data";
    static String AUTHENTICATION_ATTEMPTS_FILE = "";
    static String USER_SESSIONS_FILE = "";
    static String APPLICATION_DIRECTORY_PATH = "";
    static final int HTTP_OK = 200;

    public boolean sendData(String id, String auth, String sess, String fileName){
        String url = "http://study.csnow.ca/SubmitLogFile.aspx";
        URL serverURL;
        HttpURLConnection urlConnection = null;
        // Define statusCode to record whether the urlConnection is successful or not
        int statusCode=0;

        try {
            serverURL = new URL(url);
            urlConnection = (HttpURLConnection) serverURL.openConnection();

            // Set request method as POST
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Call function urlParametersConstructor() to create an urlParameters;
            String urlParameters = urlParametersConstructor(id,auth,sess,fileName);

            urlConnection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            urlConnection.setRequestProperty("Content-Language", "en-US");

            // The HttpURLConnection will by default buffer the entire request body before actually sending it, regardless of whether
            // you've set a fixed content length yourself using urlConnection.setRequestProperty("Content-Length",contentLength);
            // This may cause OutOfMemoryExceptions whenever you concurrently send large POST requests. Using below command to avoid this.
            urlConnection.setChunkedStreamingMode(0);

            /////////???Explain more about setUseCaches()??? How it works? Usually should we set it as true or false?/////////
            urlConnection.setUseCaches(false);
            urlConnection.setDoOutput(true);

            // Necessary to cast the statusCode first before calling getInputStream(), as the request will be automatically
            // be fired on demand when we try to get information about the HTTP response by calling getInputStream().
            statusCode = urlConnection.getResponseCode();

            // Send the POST out
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(urlParameters);
            out.close();

        } catch (MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }

        ////??? Not sure whether need to check the response is NULL or not, what Ahmed has done. If yes, do not know how to?////
        // HTTP_OK --> 200
        return statusCode == HTTP_OK;
    }

    // This function is used for constructing an urlParameters from several strings (id, auth, sess, fileName).
    public String urlParametersConstructor(String id, String auth, String sess, String fileName){
        String urlParameters = "";
        try{
            urlParameters = "txtUserId=" + URLEncoder.encode(id, "UTF-8");
            urlParameters = urlParameters + "&txtAuth="+URLEncoder.encode(auth,"UTF-8");
            urlParameters = urlParameters + "&txtSess="+URLEncoder.encode(sess,"UTF-8");
            urlParameters = urlParameters + "&txtFileName="+URLEncoder.encode(fileName,"UTF-8");
        }catch (IOException e){
            e.printStackTrace();
        }
     return urlParameters;
    }

    // Load data from logged data files
    public String loadFileData(File file) {
        String data = "";
        InputStream inputStream = null;
        byte[] buffer = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            int size = inputStream.available();
            buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            data = new String(buffer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    // Already finished to log data, here is preparation for uploading all logged data to the server
    Boolean prepareFilesToUpload(Context context) {
        APPLICATION_DIRECTORY_PATH = context.getFilesDir() + LOGGING_DIRECTORY;
        // Prepare file name according to current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar
                .getInstance().getTime());

        AUTHENTICATION_ATTEMPTS_FILE = "";
        USER_SESSIONS_FILE = "";

        String currentAuthFile = "auth-" + currentDate + ".txt";
        String currentSessFile = "session-" + currentDate + ".txt";

        String[] fileList = (new File(APPLICATION_DIRECTORY_PATH)).list();

        for (String fileName : fileList) {
            // Try to find which file we are currently in
            if (!fileName.equals(currentAuthFile)
                    && fileName.contains("auth")) {
                AUTHENTICATION_ATTEMPTS_FILE = "/" + fileName;
            }

            if (!fileName.equals(currentSessFile)
                    && fileName.contains("session")) {
                USER_SESSIONS_FILE = "/" + fileName;
            }
        }

        // Tell whether "AUTHENTICATION_ATTEMPTS_FILE" and "USER_SESSIONS_FILE" paths are created successfully.
        return !AUTHENTICATION_ATTEMPTS_FILE.isEmpty()
                && !USER_SESSIONS_FILE.isEmpty();
    }


    // Check whether has network connection or not
    public Boolean isConnectionFound(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = (activeNetwork != null
                && activeNetwork.isConnectedOrConnecting());

        return isConnected;
    }

}

