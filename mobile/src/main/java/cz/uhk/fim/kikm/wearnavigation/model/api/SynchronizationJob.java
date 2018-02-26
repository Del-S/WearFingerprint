package cz.uhk.fim.kikm.wearnavigation.model.api;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.WearApplication;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SynchronizationJob extends JobService {

    private static final String TAG = "SynchronizationJob";
    public static final String ACTION_JOB_DONE = "syncDone";
    public static final String ACTION_DATA_STATE = "status";
    public static final String ACTION_JOB_UPDATE = "syncUpdate";
    public static final String ACTION_DATA_DOWNLOAD = "dCount";
    public static final String ACTION_DATA_UPLOAD = "uCount";

    public static final int JOB_ID = 2;     // ID of this job in JobBuilder

    // States of this scanner
    private final int JOB_STATE_NONE = 0;             // Nothing is happening
    private final int JOB_STATE_PREPARING = 1;        // Job is downloading metadata before start run
    private final int JOB_STATE_RUNNING = 2;          // Download nad upload is running
    private final int JOB_STATE_FINISHING = 3;        // Job is ending and changing metadata
    public static final int JOB_STATE_FINISHED = 4;         // Job finished successfully
    public static final int JOB_STATE_FAILED = 5;           // Synchronization failed
    private int mState = JOB_STATE_NONE;

    private JobParameters mParams;

    private Configuration mConfiguration;
    private DeviceEntry mDevice;
    private ApiConnection mFingerprintApi;
    private DatabaseCRUD mDatabase;

    private List<Fingerprint> mUploadData;

    private final int DOWNLOAD_LIMIT = 50;
    private final int UPLOAD_LIMIT = 20;

    private FingerprintMeta mCurrentMeta;
    private Call<FingerprintMeta> mCallMeta;
    private Call<List<Fingerprint>> mCallDownload;
    private Call mCallUpload;

    private long mLastInsert = 0;
    private long mDownloadCount = 0;
    private long mDownloadOffset = 0;
    private long mUploadCount = 0;
    private long mUploadOffset = 0;
    private boolean mDownloadComplete = false;
    private boolean mUploadComplete = false;

    private int mDownloadRetry = 3;
    private int mUploadRetry = 3;
    private int mMetaRetry = 3;

    /**
     * Load all required instances and runs the loadMeta job.
     *
     * @param params of this job
     * @return true/false was started
     */
    @Override
    public boolean onStartJob(JobParameters params) {
        // Load configuration
        mParams = params;
        mConfiguration = Configuration.getConfiguration(this);
        if (mConfiguration != null) {
            mDevice = mConfiguration.getDevice(this);
            mDatabase = new DatabaseCRUD(this);

            // Load API connection from application
            Retrofit retrofit = ((WearApplication) getApplicationContext()).getRetrofit();
            mFingerprintApi = retrofit.create(ApiConnection.class);

            // Try to start the job
            if (mState == JOB_STATE_NONE && mFingerprintApi != null) {
                mState = JOB_STATE_PREPARING;

                // Trigger get fingerprint meta to start synchronization
                mCallMeta = mFingerprintApi.getFingerprintsMeta(mDevice.getTelephone(),
                        mConfiguration.getLastDownloadTime());
                mCallMeta.enqueue(mMetaCallback);

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mCallMeta.cancel();
        mCallDownload.cancel();
        mCallUpload.cancel();

        mState = JOB_STATE_FAILED;
        return false;
    }

    /**
     * Callback from Call for Fingerprint meta data.
     * Handles response from the API and has three cases:
     * - Job is preparing = run download ad upload
     * - Job is finishing = save metadata and finish the job
     * - Job failed
     */
    private Callback<FingerprintMeta> mMetaCallback = new Callback<FingerprintMeta>() {
        @Override
        public void onResponse(@NonNull Call<FingerprintMeta> call, @NonNull Response<FingerprintMeta> response) {
            // Get the data and check if there is any
            FingerprintMeta fingerprintMeta = response.body();
            if (fingerprintMeta != null) {
                switch (mState) {
                    case JOB_STATE_PREPARING:
                        // Loads important data and starts the sub-jobs
                        // Load meta data variables
                        mCurrentMeta = fingerprintMeta;
                        mDownloadCount = fingerprintMeta.getCountNew();
                        mLastInsert = fingerprintMeta.getLastInsert();
                        mUploadCount = mDatabase.getUploadCount(mLastInsert,
                                mDevice.getTelephone());

                        // Run download and upload jobs to continue this thread
                        runDownloadUpload();
                        break;
                    case JOB_STATE_FINISHING:
                        // Save meta data and finish the job
                        mState = JOB_STATE_FINISHED;
                        Log.d(TAG, "Saving meta data");
                        mConfiguration.setMeta(fingerprintMeta);
                        mConfiguration.setLastSynchronizationTime(System.currentTimeMillis());
                        Configuration.saveConfiguration(mConfiguration);

                        finishSynchronization();
                        break;
                    default:
                        // Fail the job and finish
                        mState = JOB_STATE_FAILED;

                        finishSynchronization();
                        break;
                }

                // Sends update broadcast
                publishUpdate();
            } else {
                // Tries to re-run the call if it can
                Log.e(TAG, "Failed to load fingerprint meta with response: " + response.code());
                reRunMeta();
            }
        }

        @Override
        public void onFailure(@NonNull Call<FingerprintMeta> call, @NonNull Throwable t) {
            Log.e(TAG, "Failed to load fingerprint meta.", t);
            reRunMeta();
        }
    };

    /**
     * Callback from Call for downloading Fingerprints.
     * Handles response from the API and has three cases:
     * - Run next call (saves data into database)
     * - Finish download job (saves data into database)
     * - Failed
     */
    private Callback<List<Fingerprint>> mDownloadCallback = new Callback<List<Fingerprint>>() {
        @Override
        public void onResponse(@NonNull Call<List<Fingerprint>> call, @NonNull Response<List<Fingerprint>> response) {
            // Check if the call was successful
            if (response.isSuccessful()) {
                // Load fingerprint data and try to save them into the database
                List<Fingerprint> fingerprints = response.body();
                if (fingerprints != null) {
                    mDatabase.saveMultipleFingerprints(fingerprints);

                    /*try {
                        Fingerprint lastFingerprint = fingerprints.get(fingerprints.size() - 1);
                        mConfiguration.setLastDownloadTime(lastFingerprint.getScanStart());
                    } catch (IndexOutOfBoundsException ex) {
                        Log.e(TAG, "Could not set download time. ", ex);
                    }*/
                }

                // Run a new download call if there is data to download
                mDownloadCount -= DOWNLOAD_LIMIT;
                if (mDownloadCount > 0) {
                    mDownloadOffset += DOWNLOAD_LIMIT;
                    runDownload();
                } else {
                    // Finish download job
                    mDownloadComplete = true;
                    resetFingerprintMeta();
                }

                // Sends update broadcast
                publishUpdate();
            } else {
                // Try to run this call again if it can
                reRunDownload();
                Log.e(TAG, "Failed to download fingerprints with response: " + response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<Fingerprint>> call, @NonNull Throwable t) {
            Log.e(TAG, "Failed to download fingerprints.", t);
            reRunDownload();
        }
    };

    /**
     * Callback from Call for uploading Fingerprints.
     * Handles response from the API and has three cases:
     * - Run next call
     * - Finish upload job
     * - Failed
     */
    private Callback<Void> mUploadCallback = new Callback<Void>() {
        @Override
        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            // Check if the call was successful
            if (response.isSuccessful()) {
                // Run next call if it should
                mUploadCount -= UPLOAD_LIMIT;
                if (mUploadCount > 0) {
                    mUploadOffset += UPLOAD_LIMIT;
                    runUpload();
                } else {
                    // Finish the upload job
                    mUploadComplete = true;
                    resetFingerprintMeta();
                }

                /*try {
                    Fingerprint lastFingerprint = mUploadData.get(mUploadData.size() - 1);
                    mCurrentMeta.setLastInsert(lastFingerprint.getScanStart());
                } catch (IndexOutOfBoundsException ex) {
                    Log.e(TAG, "Could not set last insert time. ", ex);
                }*/

                // Sends update broadcast
                publishUpdate();
            } else {
                // Tries to re-run this call if it can
                reRunUpload();
                Log.e(TAG, "Failed to upload fingerprints with response: " + response.code());
                Log.e(TAG, "Failed to upload fingerprints with response: " + response.message());
            }
        }

        @Override
        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
            // Re-run the job id it can
            Log.e(TAG, "Failed to upload fingerprints.", t);
            reRunUpload();
        }
    };

    /**
     * Runs download and upload job for fingerprints.
     * If none of them are able to run then finish this job.
     */
    private void runDownloadUpload() {
        Log.d(TAG, "Run get/post fingeprints");
        Log.d(TAG, "Start: " + System.currentTimeMillis());

        // Run download job
        /*if (mDownloadCount > 0) {
            Log.d(TAG, "Trigger download job");
            runDownload();
        } else {*/
            mDownloadComplete = true;
        //}

        // Run upload job
        if (mUploadCount > 0) {
            Log.d(TAG, "Trigger upload job");
            runUpload();
        } else {
            mUploadComplete = true;
        }

        // Finish the job if there is nothing to do
        if(mDownloadComplete && mUploadComplete) {
            mState = JOB_STATE_FINISHED;
            finishSynchronization();
        } else {
            mState = JOB_STATE_RUNNING;
        }
    }

    /**
     * Creates new download call based on variables and runs it.
     */
    private void runDownload() {
        Log.d("TEST", "Run get fingeprints with offset: " + mDownloadOffset);
        mCallDownload = mFingerprintApi.getFingerprints(mDevice.getTelephone(),
                mConfiguration.getLastDownloadTime(),
                DOWNLOAD_LIMIT,
                mDownloadOffset);
        mCallDownload.enqueue(mDownloadCallback);
    }

    /**
     * Re runs previous download job if it can.
     * If it cannot it will finish the download jobs.
     */
    private void reRunDownload() {
        // Try to re run the call.
        if (mDownloadRetry > 0) {
            mDownloadRetry--;
            mCallDownload.clone().enqueue(mDownloadCallback);
        } else {
            // Finish the download call.
            mDownloadComplete = true;
            resetFingerprintMeta();
        }
    }

    /**
     * Creates new upload call based on variables and runs it.
     */
    private void runUpload() {
        // Load fingerprints from the database
        mUploadData = mDatabase.getFingerprintsForUpload(mCurrentMeta.getLastInsert(),
                mDevice.getTelephone(),
                UPLOAD_LIMIT,
                mUploadOffset);

        // Create new call and run it
        mCallUpload = mFingerprintApi.postFingerprints(mDevice.getTelephone(),
                mUploadData);
        mCallUpload.enqueue(mUploadCallback);
    }

    /**
     * Re runs previous upload job if it can.
     * If it cannot it will finish the upload jobs.
     */
    private void reRunUpload() {
        // Try to re run the call
        if (mUploadRetry > 0) {
            mUploadRetry--;
            mCallUpload.clone().enqueue(mUploadCallback);
        } else {
            // Finish the upload call.
            mUploadComplete = true;
            resetFingerprintMeta();
        }
    }

    /**
     * Re runs previous meta data call if it can.
     * If it cannot it will fail the job
     */
    private void reRunMeta() {
        // Try to re run the call
        if (mMetaRetry > 0) {
            mMetaRetry--;
            mCallMeta.clone().enqueue(mMetaCallback);
        } else {
            // Finish the synchronization job.
            /*if (mCurrentMeta != null) {
                mCurrentMeta.setCountNew(mDownloadCount);
            }*/
            mState = JOB_STATE_FAILED;

            finishSynchronization();
        }
    }

    /**
     * Run's a call to reset fingerprint meta data.
     * It only runs when both download and upload are complete.
     */
    private void resetFingerprintMeta() {
        Log.d(TAG, "Run reset meta");
        Log.d(TAG, "Down complete: " + mDownloadComplete);
        Log.d(TAG, "Up complete: " + mUploadComplete);
        Log.d(TAG, "Down count: " + mDownloadCount);
        Log.d(TAG, "Up count: " + mUploadCount);
        // Check if this call should run or not
        if (mDownloadComplete && mUploadComplete) {
            mState = JOB_STATE_FINISHING;

            // Set current timestamp as lastDownloadTime
            if (mDownloadCount <= 0 && mUploadCount <= 0) {
                mConfiguration.setLastDownloadTime(System.currentTimeMillis());
            }

            // Create and run meta data call
            mCallMeta = mFingerprintApi.getFingerprintsMeta(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime());
            mCallMeta.enqueue(mMetaCallback);
        }
    }

    /**
     * Send update broadcast to the activities to display changes.
     */
    private void publishUpdate() {
        // Sends state, download and upload counts
        Intent intent = new Intent();
        intent.setAction(ACTION_JOB_UPDATE);
        intent.putExtra(ACTION_DATA_STATE, mState);
        intent.putExtra(ACTION_DATA_DOWNLOAD, mDownloadCount);
        intent.putExtra(ACTION_DATA_UPLOAD, mUploadCount);
        sendBroadcast(intent);
    }

    /**
     * Finishes this Synchronization job.
     * Sends a Broadcast to inform about state change.
     */
    private void finishSynchronization() {
        Log.d(TAG, "Finished: " + System.currentTimeMillis());

        // Create and send broadcast updating Activities about synchronization status
        Intent intent = new Intent();
        intent.setAction(ACTION_JOB_DONE);
        if(mState == JOB_STATE_FINISHED) {
            intent.putExtra(ACTION_DATA_STATE, mState);             // Job successful
        } else {
            intent.putExtra(ACTION_DATA_STATE, JOB_STATE_FAILED);   // Job failed
        }
        sendBroadcast(intent);

        // Complete this job
        jobFinished(mParams, false);
    }
}
