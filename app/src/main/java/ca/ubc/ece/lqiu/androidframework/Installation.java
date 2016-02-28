package ca.ubc.ece.lqiu.androidframework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;
import android.content.Context;

/**
 * Created by lina on 16-02-22.
 * This class is for Identifying App Installations. To track installations, we can use a UUID
 * as an identifier, and simply create a new one the first time an app runs after installation.
 * UUID: Universally unique identifier, an identifier standard used in software construction.
 */

public class Installation {
    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    public synchronized static String id(Context context) {
        if (sID == null) {
            File installationFile = new File(context.getFilesDir(), INSTALLATION);
            try {
                // If installationFile does not exist, which means the app has not been installed before.
                // Then install this app, and create an installationFile to record the UUID.
                if (!installationFile.exists())
                    writeInstallationFile(installationFile);
                sID = readInstallationFile(installationFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    // readInstallationFile reads the installationFile to retrieve the UUID (sID)
    private static String readInstallationFile(File installationFile) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installationFile, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    // If the app has not been installed before, call this method to create UUID and record it in installationFile.
    private static void writeInstallationFile(File installationFile) throws IOException {
        FileOutputStream out = new FileOutputStream(installationFile);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }
}
