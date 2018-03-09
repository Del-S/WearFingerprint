package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

import android.os.AsyncTask;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;

public class DatabaseDataLoader extends AsyncTask<Integer, Void, List<Fingerprint>> {

    // Modes of this loader
    public final static int MODE_FINGERPRINT = 0;

    private DatabaseCRUD mDatabase;             // Database crud to get access to the data in the SQL
    private DatabaseDataInterface mInterface;   // Interface to communicate with the context
    private Integer mMode = -1;                 // Mode of the running download

    /**
     * Constructor that checks for the interface and sets proper variables.
     *
     * @param database instance so we do not have to use Context
     * @param pInterface to communicate via
     */
    public DatabaseDataLoader(DatabaseCRUD database, DatabaseDataInterface pInterface) {
        mDatabase = database;
        mInterface = pInterface;
    }

    @Override
    protected List<Fingerprint> doInBackground(Integer... params) {
        if(mDatabase != null && mInterface != null) {
            mMode = params[0];
            switch (mMode) {
                case MODE_FINGERPRINT:
                    // Return all fingerprints from the database
                    return mDatabase.getAllFingerprints(false);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Fingerprint> result) {
        if(result != null) {
            switch (mMode) {
                case MODE_FINGERPRINT:
                    // Return all fingerprints from the database
                    mInterface.allFingerprintsLoaded(result);
                    break;
                default:
                    // If mode was not set correctly then return error
                    mInterface.loadError();
                    break;
            }
        } else {
            // If there is no result then return error
            mInterface.loadError();
        }
    }
}
