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

/**
 * This job download and uploads fingerprint into the API connected to the couchbase server.
 * Keeps track of status of both sub-jobs and posts broadcast with more information.
 * Tries to re-run sub jobs three times before failing the job but it keeps track of
 * current status so no data is processed twice.
 */
public class SynchronizationJob extends JobService {

    private static final String TAG = "SynchronizationJob";     // Logging tag
    // Broadcast actions and data tags
    public static final String ACTION_SYNC_JOB = "syncAction";
    public static final String ACTION_DATA_STATE = "status";
    public static final String ACTION_DATA_DOWN_STATE = "statusDownload";
    public static final String ACTION_DATA_UP_STATE = "statusUpload";
    public static final String ACTION_DATA_DOWNLOAD = "dCount";
    public static final String ACTION_DATA_UPLOAD = "uCount";

    public static final int JOB_ID = 2;     // ID of this job in JobBuilder

    // States of this scanner
    public static final int JOB_STATE_NONE = 0;       // Nothing is happening
    public static final int JOB_STATE_PREPARING = 1;  // Job is downloading metadata before start run
    public static final int JOB_STATE_RUNNING = 2;    // Download and upload is running
    public static final int JOB_STATE_FINISHING = 3;  // Job is ending and changing metadata
    public static final int JOB_STATE_FINISHED = 4;   // Job finished successfully
    public static final int JOB_STATE_FAILED = 5;     // Synchronization failed
    // Variable that keep track of job and sub-jobs states
    private int mState = JOB_STATE_NONE;              // Keeps state of this job as a whole
    private int mStateDownload = JOB_STATE_NONE;      // Keeps state of download sub job
    private int mStateUpload = JOB_STATE_NONE;        // Keeps state of upload sub job

    // Job instance variables
    private JobParameters mParams;          // Parameters of this job to finish or reschedule it
    private Configuration mConfiguration;   // Application configuration to load and save meta data
    private FingerprintMeta mCurrentMeta;   // Meta data for this job
    private DeviceEntry mDevice;            // Instance of this device information class
    private ApiConnection mFingerprintApi;  // Retrofit api connection interface
    private DatabaseCRUD mDatabase;         // Database to save data into
    private SynchronizationTask mTask;      // Synchronization task in a different thread

    // Limits for upload and download to protect from memory shortage and crashes
    private final int DOWNLOAD_LIMIT = 50;
    private final int UPLOAD_LIMIT = 20;

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
                // Create task instance and start it
                if (mTask == null) {
                    mTask = new SynchronizationTask();
                }
                mTask.start();

                return true;
            }
        }

        return false;
    }

    /**
     * Stops all sub-job calls if they are scheduler or running.
     *
     * @param params of this job
     * @return true/false is running
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        // Try to interrupt current task
        if (mTask != null) {
            mTask.interrupt();
            mTask = null;
        }
        return false;
    }

    /**
     * Thread that runs synchronization calls since JobService must offload your execution
     * logic to another thread/handler/AsyncTask.
     */
    private class SynchronizationTask extends Thread {

        // Call instances to enable re-runs
        private Call<FingerprintMeta> mCallMeta;       // Call for fingerprint meta data
        private Call<List<Fingerprint>> mCallDownload; // Call for downloading fingerprints
        private Call<Void> mCallUpload;                // Call for upload of fingerprints

        // Sub job primitive variables
        private long mDownloadCount = 0;    // How many fingerprints to download
        private long mDownloadOffset = 0;   // Download offset for database call
        private long mUploadCount = 0;      // How many fingerprint to upload
        private long mUploadOffset = 0;     // Upload offset for database call
        private long mLastUpdate = 0;       // Max update timestamp from database
        // Call re-try limits every call be re-tried 3 times
        private int mDownloadRetry = 3;
        private int mUploadRetry = 3;
        private int mMetaRetry = 3;
        // Checks if sub-jobs are complete or not
        private boolean mDownloadFinished = false;
        private boolean mUploadFinished = false;

        @Override
        public void run() {
            // Set thread name and change state
            setName("SynchronizationTask");
            mState = JOB_STATE_PREPARING;

            // Trigger get fingerprint meta to start synchronization
            mCallMeta = mFingerprintApi.getFingerprintsMeta(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime());
            mCallMeta.enqueue(mMetaCallback);

            // Run sleep on this thread if it's not canceled or complete
            while(!isInterrupted() &&
                    ((mState != JOB_STATE_FINISHED) && (mState != JOB_STATE_FAILED) )) {
                try {
                    // Put this thread into sleep for 5s
                    sleep(5000);
                } catch (InterruptedException e) {
                    // Log this exception
                    Log.e(TAG, "Failed to call sleep() on interrupted thread", e);

                    // Cancel calls if they are still running
                    cancelCalls();

                    // Finish this synchronization
                    mState = JOB_STATE_FAILED;
                    finishSynchronization();
                }
            }
        }

        @Override
        public void interrupt() {
            // Cancel calls if they are still running
            cancelCalls();

            // Finish this synchronization
            mState = JOB_STATE_FAILED;
            finishSynchronization();

            super.interrupt();
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
                    // Save current fingerprint meta into the configuration and sync time
                    mConfiguration.setMeta(fingerprintMeta);
                    mConfiguration.setLastSynchronizationTime(System.currentTimeMillis());

                    // Run state specific actions
                    switch (mState) {
                        case JOB_STATE_PREPARING:
                            // Load meta data and variables from it variables
                            mCurrentMeta = fingerprintMeta;
                            mDownloadCount = fingerprintMeta.getCountNew();
                            mUploadCount = mDatabase.getUploadCount(fingerprintMeta.getLastInsert(),
                                    mDevice.getTelephone());

                            // Run download and upload jobs to continue this thread
                            runDownloadUpload();
                            break;
                        case JOB_STATE_FINISHING:
                            // Finish the job
                            mState = JOB_STATE_FINISHED;
                            finishSynchronization();
                            break;
                        default:
                            // Fail the job and finish
                            mState = JOB_STATE_FAILED;
                            finishSynchronization();
                            break;
                    }

                    // Sends update broadcast
                    publishUpdate(false);
                } else {
                    // Tries to re-run the call if it can
                    Log.e(TAG, "Failed to load fingerprint meta with response: " + response.code());
                    reRunMeta();
                }
            }

            @Override
            public void onFailure(@NonNull Call<FingerprintMeta> call, @NonNull Throwable t) {
                // Tries to re-run the call if it can
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
                    if (fingerprints != null && !fingerprints.isEmpty()) {
                        mDatabase.saveMultipleFingerprints(fingerprints);
                        // Clear the list up to flag for memory clear
                        fingerprints.clear();

                        // Set update time into the configuration to protect from
                        // downloading all fingerprint when previous load failed.
                        mLastUpdate = mDatabase.getMaxUpdateTime();
                    }

                    // Run a new download call if there is data to download
                    mDownloadCount -= DOWNLOAD_LIMIT;
                    if (mDownloadCount > 0) {
                        mDownloadOffset += DOWNLOAD_LIMIT;
                        runDownload();
                    } else {
                        // Finish download job if complete
                        setDownloadFinished(true);
                        resetFingerprintMeta();
                    }

                    // Sends update broadcast
                    publishUpdate(false);
                } else {
                    // Try to run this call again if it can
                    reRunDownload();
                    Log.e(TAG, "Failed to download fingerprints with response: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Fingerprint>> call, @NonNull Throwable t) {
                // Try to run this call again if it can
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
                        setUploadFinished(true);
                        resetFingerprintMeta();
                    }

                    // Sends update broadcast
                    publishUpdate(false);
                } else {
                    // Tries to re-run this call if it can
                    reRunUpload();
                    Log.e(TAG, "Failed to upload fingerprints with response: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Tries to re-run this call if it can
                Log.e(TAG, "Failed to upload fingerprints.", t);
                reRunUpload();
            }
        };

        /**
         * Runs download and upload job for fingerprints.
         * If none of them are able to run then finish this job.
         */
        private void runDownloadUpload() {
            Log.i(TAG, "Synchronization started at: " + System.currentTimeMillis());

            // Run upload job or set is as complete
            // Upload must run first since download would
            //   also downloaded this fingerprints
            if (mUploadCount > 0) {
                mStateUpload = JOB_STATE_RUNNING;
                runUpload();
            } else {
                setUploadFinished(true);
            }

            // Run download job or set is as complete
            if (mDownloadCount > 0) {
                mStateDownload = JOB_STATE_RUNNING;
                runDownload();
            } else {
                setDownloadFinished(true);
            }

            // Finish the job if there is nothing to do
            if(mDownloadFinished && mUploadFinished) {
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
                // Fail the download and finish the download call.
                setDownloadFinished(false);
                resetFingerprintMeta();
            }
        }

        /**
         * Creates new upload call based on variables and runs it.
         */
        private void runUpload() {
            // Load fingerprints from the database
            List<Fingerprint> uploadData = mDatabase.getFingerprintsForUpload(mCurrentMeta.getLastInsert(),
                    mDevice.getTelephone(),
                    UPLOAD_LIMIT,
                    mUploadOffset);

            // Create new call and run it
            mCallUpload = mFingerprintApi.postFingerprints(mDevice.getTelephone(),
                    uploadData);
            mCallUpload.enqueue(mUploadCallback);

            // Clear the list up to flag for memory clear
            uploadData.clear();
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
                setUploadFinished(false);
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
                // Save download count to not reset the view
                if(mCurrentMeta != null && mDownloadCount > 0) {
                    mCurrentMeta.setCountNew(mDownloadCount);
                    mConfiguration.setMeta(mCurrentMeta);
                }

                // Finish the synchronization job.
                mState = JOB_STATE_FAILED;
                finishSynchronization();
            }
        }

        /**
         * Run's a call to reset fingerprint meta data.
         * It only runs when both download and upload are complete.
         */
        private void resetFingerprintMeta() {
            // Check if this call should run or not
            if (mDownloadFinished && mUploadFinished) {
                mState = JOB_STATE_FINISHING;

                // Set maximum of last update into configuration
                if (mLastUpdate > 0) {
                    mConfiguration.setLastDownloadTime(mLastUpdate);
                }

                // Save configuration data and  current timestamp as lastDownloadTime
                // Added 10 seconds to let couchbase complete the upload
                if (mDownloadCount <= 0 && mStateDownload == JOB_STATE_FINISHED) {
                    mConfiguration.setLastDownloadTime(System.currentTimeMillis() + 10000);
                }

                // Create and run meta data call
                mCallMeta = mFingerprintApi.getFingerprintsMeta(mDevice.getTelephone(),
                        mConfiguration.getLastDownloadTime());
                mCallMeta.enqueue(mMetaCallback);
            }
        }

        /**
         * Tries to cancel calls that may be running.
         */
        private void cancelCalls() {
            if(mCallMeta != null && !mCallMeta.isExecuted())
                mCallMeta.cancel();
            if(mCallDownload != null && !mCallDownload.isExecuted())
                mCallDownload.cancel();
            if(mCallUpload != null && !mCallUpload.isExecuted())
                mCallUpload.cancel();
        }

        /**
         * Send update broadcast to the activities to display changes.
         */
        private void publishUpdate(boolean finished) {
            // Broadcasts state of jobs with download and upload counts
            Intent intent = new Intent();
            intent.setAction(ACTION_SYNC_JOB);
            intent.putExtra(ACTION_DATA_STATE, mState);
            intent.putExtra(ACTION_DATA_DOWN_STATE, mStateDownload);
            intent.putExtra(ACTION_DATA_UP_STATE, mStateUpload);
            // Don't send counts if this job did not load meta data
            if(!finished) {
                intent.putExtra(ACTION_DATA_DOWNLOAD, mDownloadCount);
                intent.putExtra(ACTION_DATA_UPLOAD, mUploadCount);
            }
            sendBroadcast(intent);
        }

        /**
         * Finishes this Synchronization job.
         * Sends a Broadcast to inform about state change.
         */
        private void finishSynchronization() {
            Log.i(TAG, "Synchronization finished at: " + System.currentTimeMillis());

            // Change job state
            if(mState != JOB_STATE_FINISHED) {
                mState = JOB_STATE_FAILED;
            }

            Configuration.saveConfiguration(mConfiguration);

            // Publish update and finish this job
            publishUpdate(true);

            // Stop the task and the job
            mTask = null;
            jobFinished(mParams, false);
        }

        /**
         * Sets download job as finished and changes all
         * connected variables.
         *
         * @param success if download finished successfully
         */
        private void setDownloadFinished(boolean success) {
            mDownloadFinished = true;
            mStateDownload = (success) ? JOB_STATE_FINISHED : JOB_STATE_FAILED;
        }

        /**
         * Sets upload job as finished and changes all
         * connected variables.
         *
         * @param success if upload finished successfully
         */
        private void setUploadFinished(boolean success) {
            mUploadFinished = true;
            mStateUpload = (success) ? JOB_STATE_FINISHED : JOB_STATE_FAILED;
        }
    }
}
