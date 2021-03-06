package ca.ubc.ece.lqiu.androidframework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

/**
 * Created by lina on 16-02-22.
 * AsyncTask enables proper and easy use of the UI thread. This class allows to perform
 * background operations and publish results on the UI thread without having to manipulate
 * threads and/or handlers.
 */


public class UploadFilesTask extends AsyncTask<String, Void, Void> {

    static final String LOGGING_DIRECTORY = "/android_logged_data";
    static String UPLOADED_DIRECTORY = "";
    static String AUTHENTICATION_ATTEMPTS_FILE = "";
    static String USER_SESSIONS_FILE = "";
    static String APPLICATION_DIRECTORY_PATH = "";

    private Context aContext;
    private Uploader uploader = new Uploader();

    public UploadFilesTask(Context context) {
        aContext = context;
    }

    // doInBackground(Params...), invoked on the background thread immediately after onPreExecute()
    // finishes executing. It's used to perform background computation that can take a long time.
    @Override
    protected Void doInBackground(String... params) {
        // Post data to the server

        APPLICATION_DIRECTORY_PATH = aContext.getFilesDir() + LOGGING_DIRECTORY;
        // getExternalStorageDirectory(): returns the primary shared/external storage directory.
        // This directory may not currently be accessible if it has been mounted by the user on
        // their computer, has been removed from the device, or some other problem has happened.
        UPLOADED_DIRECTORY = Environment.getExternalStorageDirectory() + "/usage_pattern_study";

        // Detect files for uploading
        boolean isFilePrepared = uploader.prepareFilesToUpload(aContext);

        while (isFilePrepared) {

            // Encrypt files before uploading
            String authData = encrypt(APPLICATION_DIRECTORY_PATH
                    + AUTHENTICATION_ATTEMPTS_FILE);
            String sessionData = encrypt(APPLICATION_DIRECTORY_PATH
                    + USER_SESSIONS_FILE);

            // Send authentication_attempts data and session_data together, so only need currentDate for filePath
            boolean response = uploader.sendData(params[0], authData,
                    sessionData, AUTHENTICATION_ATTEMPTS_FILE.substring(6));

           /* if (response != null && response.getStatusLine().getStatusCode() == 200) */
            if (response){
                logging_data_truncate(APPLICATION_DIRECTORY_PATH
                        + AUTHENTICATION_ATTEMPTS_FILE, authData);
                logging_data_truncate(APPLICATION_DIRECTORY_PATH
                        + USER_SESSIONS_FILE, sessionData);

                // Move uploaded files to "uploaded" folder
                moveFile(AUTHENTICATION_ATTEMPTS_FILE,
                        APPLICATION_DIRECTORY_PATH, UPLOADED_DIRECTORY);
                moveFile(USER_SESSIONS_FILE, APPLICATION_DIRECTORY_PATH, UPLOADED_DIRECTORY);
            } else {
                return null;
            }
            isFilePrepared = uploader.prepareFilesToUpload(aContext);
        }

        return null;
    }

    void moveFile(String fileName, String from, String to) {
        InputStream inStream = null;
        OutputStream outStream = null;

        try {

            File sourceFile = new File(from + fileName);
            File destinationFile = new File(to + fileName);

            inStream = new FileInputStream(sourceFile);
            outStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];

            int length;
            // copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0) {

                outStream.write(buffer, 0, length);
            }

            inStream.close();
            outStream.close();

            // delete the original file
            sourceFile.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String encrypt(String filePath) {
        String plain = uploader.loadFileData(new File(filePath));
        String cipher = "";

        try {
            byte[] key = Crypto.generateKey().getEncoded();
            cipher = Crypto.encrypt_key(key, APPLICATION_DIRECTORY_PATH
                    + "/rsa.pub");
            cipher += Crypto.encrypt(key, plain.getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipher;
    }

    /*
     * Logging data in specified path
     *
     * @param filepath file path store logged data
     *
     * @param data that will be logged in the file
     */
    private void logging_data_truncate(String filePath, String data) {
        try {
            FileOutputStream out = new FileOutputStream(filePath, false);
            // getBytes(): Encodes this String into a sequence of bytes using the platform's
            // default charset, storing the result into a new byte array.
            out.write(data.getBytes());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

