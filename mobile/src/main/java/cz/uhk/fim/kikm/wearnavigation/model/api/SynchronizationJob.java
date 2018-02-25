package cz.uhk.fim.kikm.wearnavigation.model.api;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.AsyncTask;
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
    public static final String ACTION_SYNC_COMPLETE = "syncComplete";
    public static final String ACTION_SYNC_FAILED = "syncFailed";

    public static final int JOB_ID = 2;     // ID of this job in JobBuilder

    // States of this scanner
    private final int TASK_STATE_NONE = 0;             // Nothing is happening
    private final int TASK_STATE_RUNNING = 1;          // Synchronization is running
    private final int TASK_STATE_FINISHING = 2;        // Synchronization is finishing
    private final int TASK_STATE_FINISHED = 3;         // Synchronization finished
    private final int TASK_STATE_FAILED = 4;           // Synchronization failed
    private int mState = TASK_STATE_NONE;

    private JobParameters mParams;

    private Configuration mConfiguration;
    private DeviceEntry mDevice;
    private ApiConnection mFingerprintApi;
    private DatabaseCRUD mDatabase;
    private SynchronizationTask mSynchronizationTask;

    private List<Fingerprint> mUploadData;

    private final int MAX_TIMEOUT = 120000;              // Max length of this job (90s)
    private final int DOWNLOAD_LIMIT = 100;
    private final int UPLOAD_LIMIT = 20;

    @Override
    public boolean onStartJob(JobParameters params) {
        mParams = params;
        mConfiguration = Configuration.getConfiguration(this);
        if(mConfiguration != null) {
            mDevice = mConfiguration.getDevice(this);
            mDatabase = new DatabaseCRUD(this);

            Retrofit retrofit = ((WearApplication) getApplicationContext()).getRetrofit();
            mFingerprintApi = retrofit.create(ApiConnection.class);

            if(mState == TASK_STATE_NONE && mFingerprintApi != null) {
                mState = TASK_STATE_RUNNING;

                mSynchronizationTask = new SynchronizationTask();
                mSynchronizationTask.execute();

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if(mSynchronizationTask != null && !mSynchronizationTask.isCancelled())
            mSynchronizationTask.cancel(true);
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    private class SynchronizationTask extends AsyncTask<Void, Void, Boolean> {

        // Time variables so the task will not run forever
        private int mRunTime = 0;
        private int mThreadUpdateDelay = 5000;              // Wait timer to update progress (1s)

        private FingerprintMeta mCurrentMeta;
        private Call<FingerprintMeta> mCallMeta;
        private Call<List<Fingerprint>> mCallDownload;
        private Call mCallUpload;

        private long mLastInsert = 0;
        private int mDownloadCount = 0;
        private int mDownloadOffset = 0;
        private long mUploadCount = 0;
        private int mUploadOffset = 0;
        private boolean mDownloadComplete = false;
        private boolean mUploadComplete = false;

        private int mDownloadRetry = 3;
        private int mUploadRetry = 3;
        private int mMetaRetry = 3;

        @Override
        protected Boolean doInBackground(Void... voids) {
            // Trigger get fingerprint meta to start synchronization
            mCallMeta = mFingerprintApi.getFingerprintsMeta(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime());
            mCallMeta.enqueue(mMetaCallback);

            // Handle pausing of this thread
            while (mRunTime < MAX_TIMEOUT &&
                    (mState == TASK_STATE_RUNNING ||
                     mState == TASK_STATE_FINISHING)) {
                Log.d(TAG, "Still in while " + mRunTime);

                // If this task was not cancelled
                if (!isCancelled()) {
                    try {
                        // Update information and increase timer
                        mRunTime += mThreadUpdateDelay;
                        publishProgress();

                        // Pause thread for few second
                        Thread.sleep(mThreadUpdateDelay);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Failed to call sleep", e);
                        mState = TASK_STATE_FAILED;
                        return false;
                    }
                } else {
                    Log.d(TAG, "Task was canceled");
                    mState = TASK_STATE_FAILED;
                    return false;
                }
            }

            Log.d(TAG, "Task complete");

            if(mState != TASK_STATE_FINISHED) {
                mState = TASK_STATE_FAILED;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean complete) {
            // Toast if the synchronization was successful
            if(mState == TASK_STATE_FINISHED) {
                // Send broadcast to inform activities that load was successful
                Intent intent = new Intent();
                intent.setAction(ACTION_SYNC_COMPLETE);
                sendBroadcast(intent);
            } else {
                // Send broadcast to inform activities that load failed
                Intent intent = new Intent();
                intent.setAction(ACTION_SYNC_FAILED);
                sendBroadcast(intent);
            }

            // Complete this job
            jobFinished(mParams, false);
        }

        private void runDownloadUpload() {
            Log.d(TAG, "Run get/post fingeprints");
            Log.d(TAG, "Start: " + System.currentTimeMillis());

            if(mDownloadCount > 0) {
                Log.d(TAG, "Trigger download job");
                runDownload();
            } else {
                mDownloadComplete = true;
            }

            if(mUploadCount > 0) {
                Log.d(TAG, "Trigger upload job");
                runUpload();
            } else {
                mUploadComplete = true;
            }
        }

        private Callback<FingerprintMeta> mMetaCallback = new Callback<FingerprintMeta>() {
            @Override
            public void onResponse(@NonNull Call<FingerprintMeta> call, @NonNull Response<FingerprintMeta> response) {
                FingerprintMeta fingerprintMeta = response.body();
                if (fingerprintMeta != null) {
                    mCurrentMeta = fingerprintMeta;

                    mDownloadCount = fingerprintMeta.getCountNew();
                    mLastInsert = fingerprintMeta.getLastInsert();
                    mUploadCount = mDatabase.getUploadCount(mLastInsert,
                            mDevice.getTelephone());

                    switch (mState) {
                        case TASK_STATE_RUNNING:
                            // TODO: this is called twice for some reason
                            runDownloadUpload();
                            break;
                        case TASK_STATE_FINISHING:
                            mState = TASK_STATE_FINISHED;
                            Log.d("TEST", "Run reset meta fingeprints");
                            mConfiguration.setMeta(mCurrentMeta);
                            mConfiguration.setLastSynchronizationTime(System.currentTimeMillis());
                            Configuration.saveConfiguration(mConfiguration);
                            break;
                        default:
                            mState = TASK_STATE_FAILED;
                            break;
                    }
                } else {
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

        private Callback<List<Fingerprint>> mDownloadCallback = new Callback<List<Fingerprint>>() {
            @Override
            public void onResponse(@NonNull Call<List<Fingerprint>> call, @NonNull Response<List<Fingerprint>> response) {
                if(response.isSuccessful()) {
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

                    //mDownloadErrors += DOWNLOAD_LIMIT - count;
                    mDownloadCount -= DOWNLOAD_LIMIT;
                    if (mDownloadCount > 0) {
                        mDownloadOffset += DOWNLOAD_LIMIT;
                        runDownload();
                    } else {
                        mDownloadComplete = true;
                        resetFingerprintMeta();
                    }
                } else {
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

        private Callback<Void> mUploadCallback = new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if(response.isSuccessful()) {
                    mUploadCount -= UPLOAD_LIMIT;
                    if (mUploadCount > 0) {
                        mUploadOffset += UPLOAD_LIMIT;
                        runUpload();
                    } else {
                        mUploadComplete = true;
                        resetFingerprintMeta();
                    }

                    try {
                        Fingerprint lastFingerprint = mUploadData.get(mUploadData.size() - 1);
                        mCurrentMeta.setLastInsert(lastFingerprint.getScanStart());
                    } catch (IndexOutOfBoundsException ex) {
                        Log.e(TAG, "Could not set last insert time. ", ex);
                    }
                } else {
                    reRunUpload();
                    Log.e(TAG, "Failed to upload fingerprints with response: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to upload fingerprints.", t);
                reRunUpload();
            }
        };

        private void runDownload() {
            Log.d("TEST", "Run get fingeprints with offset: " + mDownloadOffset);
            mCallDownload = mFingerprintApi.getFingerprints(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime(),
                    DOWNLOAD_LIMIT,
                    mDownloadOffset);
            mCallDownload.enqueue(mDownloadCallback);
        }

        private void reRunDownload() {
            if(mDownloadRetry > 0) {
                mDownloadRetry--;
                mCallDownload.clone().enqueue(mDownloadCallback);
            } else {
                mDownloadComplete = true;
                resetFingerprintMeta();
            }
        }

        private void runUpload() {
            mUploadData = mDatabase.getFingerprintsForUpload(mCurrentMeta.getLastInsert(),
                    mDevice.getTelephone(),
                    UPLOAD_LIMIT,
                    mUploadOffset);
            mCallUpload = mFingerprintApi.postFingerprints(mDevice.getTelephone(),
                    mUploadData);
            mCallUpload.enqueue(mUploadCallback);
        }

        private void reRunUpload() {
            if(mUploadRetry > 0) {
                mUploadRetry--;
                mCallUpload.clone().enqueue(mUploadCallback);
            } else {
                mUploadComplete = true;
                resetFingerprintMeta();
            }
        }

        private void reRunMeta() {
            if(mMetaRetry > 0) {
                mMetaRetry--;
                mCallMeta.clone().enqueue(mMetaCallback);
            } else {
                if(mCurrentMeta != null) {
                    mCurrentMeta.setCountNew(mDownloadCount);
                }
                mState = TASK_STATE_FAILED;
            }
        }

        private void resetFingerprintMeta() {
            Log.d(TAG, "Run reset meta");
            Log.d(TAG, "Down complete: " + mDownloadComplete);
            Log.d(TAG, "Up complete: " + mUploadComplete);
            Log.d(TAG, "Down count: " + mDownloadCount);
            Log.d(TAG, "Up count: " + mUploadCount);
            if (mDownloadComplete && mUploadComplete) {
                mState = TASK_STATE_FINISHING;

                // Set current timestamp as lastDownloadTime
                if(mDownloadCount == 0 && mUploadCount == 0) {
                    mConfiguration.setLastDownloadTime(System.currentTimeMillis());
                }

                mCallMeta = mFingerprintApi.getFingerprintsMeta(mDevice.getTelephone(),
                        mConfiguration.getLastDownloadTime());
                mCallMeta.enqueue(mMetaCallback);
            }
        }
    }
}
