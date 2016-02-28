package ca.ubc.ece.lqiu.androidframework;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by lina on 16-02-22.
 */

public class DownloadTask extends AsyncTask<String, Void, Void> {
    static final String POINTS = "/points.txt";
    static final String LOGGING_DIRECTORY = "/android_logged_data";
    static String APPLICATION_DIRECTORY_PATH = "";

    private Context aContext;
    private Downloader downloader = new Downloader();

    DownloadTask(Context context) {
        aContext = context;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //String... params? params[0]?
    @Override
    protected Void doInBackground(String... params) {
        String data = downloader.getData(params[0]);
        APPLICATION_DIRECTORY_PATH = aContext.getFilesDir() + LOGGING_DIRECTORY;
        String points = data.substring(data.indexOf("{") + 1,
                data.indexOf(",", data.indexOf("{") - 1));
        logging_data(APPLICATION_DIRECTORY_PATH + POINTS, points);

        return null;
    }

    /**
     * Logging data in specified path
     *
     * @param filePath
     *            file path store logged data
     *
     * @param data
     *            that will be logged in the file
     */
    private void logging_data(String filePath, String data) {
        try {
            FileOutputStream out = new FileOutputStream(filePath);
            out.write(data.getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

