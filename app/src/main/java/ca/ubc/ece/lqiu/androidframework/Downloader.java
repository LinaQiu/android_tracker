package ca.ubc.ece.lqiu.androidframework;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by lina on 16-02-22.
 * Read how much data has been recorded in the server for a certain user,
 * in order to compute how long the user should continue to participate in the study.
 */


public class Downloader {

    public String getData(String id) {
        InputStream content = null;
        String url = "http://study.csnow.ca/ProgressResult.aspx?id=" + id;
        String data = "";

        URL serverURL;
        HttpURLConnection urlConnection = null;

        // ???? Not sure whether to use getErrorStream() or not????
        try {
            serverURL = new URL(url);
            urlConnection = (HttpURLConnection) serverURL.openConnection();
            // For GET request
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);

            // HTTP_OK --> 200
            int statusCode = urlConnection.getResponseCode();

            if (statusCode == 200){
                content = new BufferedInputStream(urlConnection.getInputStream());
            }

        } catch (MalformedURLException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }

        try {
            data = readInputStream(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public String readInputStream(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];

        // read(byte[]buffer): Equivalent to read(buffer, 0, buffer.length)
        // read(byte[]buffer, int byteOffset, int byteCount): reads up to byteCount bytes from
        // this stream and stores them in the byte array buffer starting at byteOffset. Returns
        // the number of bytes actually read or -1 if the end of the stream has been reached.
        for (int i; (i = in.read(b)) != -1;) {
            // append(String str): Appends the specified string to this character sequence.
            // the read buffer is stored at b after calling read() method
            out.append(new String(b, 0, i));
        }
        return out.toString();
    }
}

