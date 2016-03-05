package ca.ubc.ece.lqiu.androidframework;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by lina on 16-02-22.
 * Modeling authentication attempts in (outcome, start time, end time, method type)
 * Define data structure, which records authentication_attempts related data
 */

public class Authentication_Model {

    String _outcome;
    String _timeStamp;
    String _method_type;

    public void set_outcome(String _outcome) {
        this._outcome = _outcome;
    }

    public void set_method_type(String _method_type) {
        this._method_type = _method_type;
    }

    public void set_timeStamp(String _timeStamp) {
        this._timeStamp = _timeStamp;
    }

    /*
     * Logging data in specified path
     * @filepath: file path for data logging
     */
    public void logging_data(String filePath) {
        try {
            String data = _outcome + "," + _timeStamp + ","
                    + _method_type + "\n";
            FileOutputStream out = new FileOutputStream(filePath, true);
            out.write(data.getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

