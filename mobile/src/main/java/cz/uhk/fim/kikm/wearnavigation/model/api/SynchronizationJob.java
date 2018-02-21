package cz.uhk.fim.kikm.wearnavigation.model.api;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiException;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;

public class SynchronizationJob extends JobService implements FingerprintResult {

    public static final int JOB_ID = 2;     // ID of this job in JobBuilder

    // States of this scanner
    private final int TASK_STATE_NONE = 0;             // Nothing is happening
    private final int TASK_STATE_RUNNING = 1;          // Synchronization is running
    private final int TASK_STATE_FINISHING = 2;        // Synchronization is finishing
    private final int TASK_STATE_FINISHED = 3;         // Synchronization finished
    private final int TASK_STATE_FAILED = 4;         // Synchronization failed
    private int mState = TASK_STATE_NONE;

    private JobParameters mParams;

    private Configuration mConfiguration;
    private DeviceEntry mDevice;
    private FingerprintMeta mCurrentMeta;
    private FingerprintApi mFingerprintApi;
    private DatabaseCRUD mDatabase;

    private List<Fingerprint> uploadData;

    private final int DOWNLOAD_LIMIT = 100;
    private final int UPLOAD_LIMIT = 10;
    private int mDownloadCount = 0;
    private int mDownloadOffset = 0;
    //private int mDownloadErrors = 0;
    private long mUploadCount = 0;
    private int mUploadOffset = 0;

    @Override
    public boolean onStartJob(JobParameters params) {
        mParams = params;
        mConfiguration = Configuration.getConfiguration(this);
        if(mConfiguration != null) {
            mDevice = mConfiguration.getDevice(this);
            mFingerprintApi = new FingerprintApi(this);
            mDatabase = new DatabaseCRUD(this);

            if(mState == TASK_STATE_NONE) {
                // Get fingerprint meta
                mFingerprintApi.getFingerprintMeta(mDevice.getTelephone(),
                        mConfiguration.getLastDownloadTime());

                mState = TASK_STATE_RUNNING;

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void loadedFingerprintMeta(FingerprintMeta fingerprintMeta) {
        Log.d("Test", "Got fingerprint meta");
        if(fingerprintMeta != null) {
            mCurrentMeta = fingerprintMeta;
            Log.d("TEST", fingerprintMeta.toString());

            mDownloadCount = fingerprintMeta.getCountNew();
            mUploadCount = mDatabase.getUploadCount(fingerprintMeta.getLastInsert(),
                    mDevice.getTelephone());

            switch (mState) {
                case TASK_STATE_RUNNING:
                    Log.d("TEST", "Run get fingeprints");
                    mFingerprintApi.getFingerprints(mDevice.getTelephone(),
                            mConfiguration.getLastDownloadTime(),
                            DOWNLOAD_LIMIT,
                            null,
                            null);
                    break;
                case TASK_STATE_FINISHING:
                    Log.d("TEST", "Run reset meta fingeprints");
                    mConfiguration.setMeta(fingerprintMeta);
                    Configuration.saveConfiguration(mConfiguration);
                    runJobFinish(false);
                    break;
                default:
                    runJobFinish(true);
                    break;
            }
        } else {
            runJobFinish(true);
        }
    }

    @Override
    public void loadedFingerprints(int count) {
        Log.d("TEST", "Loaded fingerprints: " + count);
        //mDownloadErrors += DOWNLOAD_LIMIT - count;
        mDownloadCount -= DOWNLOAD_LIMIT;
        if(mDownloadCount > 0) {
            mDownloadOffset += DOWNLOAD_LIMIT;

            Log.d("TEST", "Run get fingeprints with offset: " + mDownloadOffset);
            mFingerprintApi.getFingerprints(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime(),
                    DOWNLOAD_LIMIT,
                    mDownloadOffset,
                    null);
        } else {
            if(mUploadCount > 0) {
                Log.d("TEST", "Run upload fingeprints: " + mDownloadOffset);
                uploadData = mDatabase.getFingerprintsForUpload(mCurrentMeta.getLastInsert(),
                        mDevice.getTelephone(),
                        UPLOAD_LIMIT,
                        mUploadOffset);
                mFingerprintApi.postFingerprints(mDevice.getTelephone(), uploadData);
            } else {
                resetFingerprintMeta();
            }
        }
    }

    @Override
    public void postedFingerprints() {
        Log.d("Test", "Fingerprints posted");
        mUploadCount -= UPLOAD_LIMIT;
        if(mUploadCount > 0) {
            mUploadOffset += UPLOAD_LIMIT;

            uploadData = mDatabase.getFingerprintsForUpload(mCurrentMeta.getLastInsert(),
                    mDevice.getTelephone(),
                    UPLOAD_LIMIT,
                    mUploadOffset);
            mFingerprintApi.postFingerprints(mDevice.getTelephone(), uploadData);
        } else {
            resetFingerprintMeta();
        }
    }

    @Override
    public void apiException(ApiException ex) {
        Log.e("TEST", "API exception: ", ex);
        runJobFinish(true);
    }

    private void resetFingerprintMeta() {
        Log.d("TEST", "Reset meta");
        mState = TASK_STATE_FINISHING;
        mFingerprintApi.getFingerprintMeta(mDevice.getTelephone(),
                mConfiguration.getLastDownloadTime());
    }

    private void runJobFinish(boolean failed) {
        Log.d("TEST", "Finish the job.");
        mState = (failed) ? TASK_STATE_FAILED : TASK_STATE_FINISHED ;
        jobFinished(mParams, false);
    }
}
