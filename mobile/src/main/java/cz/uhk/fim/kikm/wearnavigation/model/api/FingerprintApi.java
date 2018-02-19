package cz.uhk.fim.kikm.wearnavigation.model.api;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiException;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiResponse;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.LocationEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;

/**
 * Api for synchronization of Fingerprints between beacons server and mobile application.
 * This API class is mediator between the App and Swagger api classes.
 *
 * Api specification can be found here:
 * https://app.swaggerhub.com/apis/Del-S/FingerprintAPI/1.0.2
 */
public class FingerprintApi {

    // Global variables
    private static final String TAG = "FingerprintApi"; // Logging and exception tags
    private Context mContext;   // Context to use for the database and interface

    // Api variables
    private MobileUsersApi mua = new MobileUsersApi();
    private FingerprintResult mInterface;

    // Call parameters
    private String deviceId;        // Id of device used for authentication and blacklisting
    private Long timestamp;         // Limiting time in milliseconds
    private Integer limit;          // Limit of data to load from a database
    private Integer offset;         // Offset to start from in the database
    private LocationEntry location; // Location to get fingerprints for
    private List<Fingerprint> fingerprints; // Fingerprints to post to the server

    public FingerprintApi(Context context) {
        this.mContext = context;

        // Initiate interface connection or throw Exception
        try {
            mInterface = (FingerprintResult) mContext;
        } catch (Exception e) {
            throw new ClassCastException(TAG
                    + " must implement IndividualResultListener");
        }

    }

    /**
     * Loads fingerprints from the beacon server.
     * Can filter fingerprints by specific timestamp (ms) and location.
     * Everything with higher timestamp will be displayed.
     *
     * @param deviceId used for authentication and blacklisting
     * @param timestamp filter out fingerprints with higher timestamp
     * @param limit for database query used to limit amount of data
     * @param offset used with the limit to load different data within the specify limit
     * @param location to get fingerprints for
     */
    public void getFingerprints(String deviceId, Long timestamp, Integer limit, Integer offset, LocationEntry location) {
        // Put call data into instance variables
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.limit = limit;
        this.offset = offset;
        this.location = location;

        // Run asyncTask to get fingerprints
        new ApiAsyncTask("get").execute();
    }

    /**
     *  Adds fingerprint into the database from mobile device.
     *  Can handle multiple fingerprints send as JSONArray.
     *  Update entry is based on id of specific Fingerprint.
     *
     * @param deviceId used for authentication and blacklisting
     * @param fingerprints fingerprint to save
     */
    public void postFingerprints(String deviceId, List<Fingerprint> fingerprints) {
        // Put call data into instance variables
        this.deviceId = deviceId;
        this.fingerprints = fingerprints;

        // Run asyncTask to post fingerprints
        new ApiAsyncTask("post").execute();
    }

    /**
     * Gets meta information about fingerprints.
     * Two parts of data are count of new fingerprints
     * and last update time based on device id.
     *
     * @param deviceId used for authentication and blacklisting
     * @param timestamp find fingerprints with higher timestamp (new ones)
     */
    public void getFingerprintMeta(String deviceId, Long timestamp) {
        // Put call data into instance variables
        this.deviceId = deviceId;
        this.timestamp = timestamp;

        // Run asyncTask to get fingerprints meta information
        new ApiAsyncTask("getMeta").execute();
    }

    /**
     * AsyncTask that servers as a mediator between the app code and
     * swagger code for api connection. It simplifies the api for common use.
     */
    private class ApiAsyncTask extends AsyncTask<String, Void, ApiResponse> {

        // Changes api calls base on mode
        private final String mode;

        ApiAsyncTask(String mode) {
            this.mode = mode;
        }

        @Override
        protected ApiResponse doInBackground(String... params) {
            // Call api based on the mode
            try {
                switch (mode) {
                    case "get":
                        return mua.getFingerprintsWithHttpInfo(deviceId, timestamp, limit, offset, location);
                    case "getMeta":
                        return mua.getFingerprintMetaWithHttpInfo(deviceId, timestamp);
                    case "post":
                        return mua.addFingerprintsWithHttpInfo(deviceId, fingerprints);
                }
            } catch (ApiException e) {
                mInterface.apiException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ApiResponse result) {
            // Return result to he Activities based on mode
            if (result != null) {

                // Return specific data based on the mode
                switch (mode) {
                    case "get":
                        int count = parseFingerprints( (List<Fingerprint>) result.getData() );
                        mInterface.loadedFingerprints(count);
                        break;
                    case "getMeta":
                        mInterface.loadedFingerprintMeta( (FingerprintMeta) result.getData() );
                        break;
                    case "post":
                        mInterface.postedFingerprints();
                        break;
                }
            }
            super.onPostExecute(result);
        }

        /**
         * Saves fingerprints into SQLite database.
         *
         * @return int count of fingerprints
         */
        @NonNull
        private Integer parseFingerprints(List<Fingerprint> fingerprints) {
            // Initiate database connection
            DatabaseCRUD dbcrud = new DatabaseCRUD(mContext);

            // Save all fingerprints into the database
            return dbcrud.saveMultipleFingerprints(fingerprints);
        }
    }

}