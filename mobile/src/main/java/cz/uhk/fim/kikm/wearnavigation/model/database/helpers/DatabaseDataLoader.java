package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class DatabaseDataLoader<T> extends AsyncTask<Integer, Void, T> {

    // Modes of this loader
    public final static int MODE_FINGERPRINT = 0;

    // Database crud to get access to the data in the SQL
    private DatabaseCRUD mDatabase;
    // Interface to communicate with the context
    private DatabaseDataInterface databaseDataInterface;
    // Mode of the running download
    private Integer mMode = -1;

    /**
     * Constructor that checks for the interface and sets proper variables.
     *
     * @param context from which is this task loaded.
     */
    public DatabaseDataLoader(Context context) {
        // Checks and loads the interface
        try {
            databaseDataInterface = (DatabaseDataInterface) context;
        } catch (Exception e) {
            throw new ClassCastException(context.getClass()
                    + " must implement IndividualResultListener");
        }

        // Creates database CRUD helper instance
        mDatabase = new DatabaseCRUD(context);
    }

    @Override
    protected T doInBackground(Integer... params) {
        mMode = params[0];
        switch (mMode) {
            case MODE_FINGERPRINT:
                // Return all fingerprints from the database
                return (T) mDatabase.getAllFingerprints(false);
        }
        return null;
    }

    @Override
    protected void onPostExecute(T result) {
        if(result != null) {
            switch (mMode) {
                case MODE_FINGERPRINT:
                    // Return all fingerprints from the database
                    databaseDataInterface.allFingerprintsLoaded(result);
                    break;
                default:
                    // If mode was not set correctly then return error
                    databaseDataInterface.loadError();
                    break;
            }
        } else {
            // If there is no result then return error
            databaseDataInterface.loadError();
        }
    }
}
