package cz.uhk.fim.kikm.wearnavigation.model.api;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiException;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;

public class SynchronizationJob extends JobService implements FingerprintResult {

    public static final int JOB_ID = 2;     // ID of this job in JobBuilder

    // States of this scanner
    private final int TASK_STATE_NONE = 0;            // Nothing is happening
    private final int TASK_STATE_STARTING = 1;        // Starting scan
    private final int TASK_STATE_RUNNING = 2;         // Scan is running

    private JobParameters mParams;

    private Configuration mConfiguration;
    private DeviceEntry mDevice;
    private FingerprintApi mFingerprintApi;
    //private DatabaseCRUD mDatabase;

    private final int DOWNLOAD_LIMIT = 100;
    private int mDownloadCount = 0;
    private int mDownloadOffset = 0;
    //private int mDownloadErrors = 0;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("SynchroJob", "Starting job: " + System.currentTimeMillis());
        mParams = params;
        mConfiguration = Configuration.getConfiguration(this);
        if(mConfiguration != null) {
            mDevice = mConfiguration.getDevice(this);
            mFingerprintApi = new FingerprintApi(this);
            //mDatabase = new DatabaseCRUD(this);

            // Get fingerprint meta
            Log.d("SynchroJob", "Running get metadata");
            mFingerprintApi.getFingerprintMeta(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime());

            return true;
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void loadedFingerprintMeta(FingerprintMeta fingerprintMeta) {
        Log.d("SynchroJob", "Metadata returned");
        if(fingerprintMeta != null) {
            mDownloadCount = fingerprintMeta.getCountNew();
            //mUpload = mDatabase.getUploadCount(mMeta.getLastInsert(), mDevice.getTelephone());

            Log.d("SynchroJob", "Run get fingerprints");
            mFingerprintApi.getFingerprints(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime(),
                    DOWNLOAD_LIMIT,
                    null,
                    null);

        } else {
            Log.d("SynchroJob", "Job failed");
            jobFinished(mParams, false);
        }
    }

    @Override
    public void loadedFingerprints(int count) {
        Log.d("SynchroJob", "Loading dingeprints");
        //mDownloadErrors += DOWNLOAD_LIMIT - count;
        mDownloadCount -= DOWNLOAD_LIMIT;
        if(mDownloadCount > 0) {
            mDownloadOffset += DOWNLOAD_LIMIT;

            Log.d("SynchroJob", "Run get fingerprints #2");
            mFingerprintApi.getFingerprints(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime(),
                    DOWNLOAD_LIMIT,
                    mDownloadOffset,
                    null);
        } else {
            Log.d("SynchroJob", "Ending job: " + System.currentTimeMillis());
            jobFinished(mParams, false);
        }
    }

    @Override
    public void postedFingerprints() {

    }

    @Override
    public void apiException(ApiException ex) {

    }
}
