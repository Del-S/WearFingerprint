package cz.uhk.fim.kikm.wearnavigation.activities.configuration;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.api.FingerprintApi;
import cz.uhk.fim.kikm.wearnavigation.model.api.FingerprintMeta;
import cz.uhk.fim.kikm.wearnavigation.model.api.FingerprintResult;
import cz.uhk.fim.kikm.wearnavigation.model.api.SynchronizationJob;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiException;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;

public class ConfigurationActivity extends BaseActivity implements FingerprintResult {

    private Configuration mConfiguration;
    private DeviceEntry mDevice;
    private FingerprintMeta mMeta;
    private FingerprintApi mFingerprintApi;
    private DatabaseCRUD mDatabase;

    private TextView mNewDownload, mNewUpload;
    private ConstraintLayout mLayoutDownload, mLayoutUpload;

    private JobScheduler jobScheduler;          // JobScheduler used to run FingerprintScanner

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initiate data
        mConfiguration = Configuration.getConfiguration(this);
        mDevice = mConfiguration.getDevice(this);
        mMeta = mConfiguration.getMeta();
        mFingerprintApi = new FingerprintApi(this);
        mDatabase = new DatabaseCRUD(this);

        jobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );
        // Todo: remove this it is a test
        if(jobScheduler != null) {
            jobScheduler.cancel(SynchronizationJob.JOB_ID);
        }

        // Initiate views
        mNewDownload = findViewById(R.id.as_new_download);
        mNewUpload = findViewById(R.id.as_new_upload);
        mLayoutDownload = findViewById(R.id.as_constraint_new_download);
        mLayoutUpload = findViewById(R.id.as_constraint_new_upload);
        initiateViewActions();

        updateUI();
    }

    private void initiateViewActions() {
        mLayoutDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isSynchronizationRunning()) {
                    // Building job to run
                    JobInfo.Builder jobBuilder = new JobInfo.Builder(SynchronizationJob.JOB_ID,
                            new ComponentName(getPackageName(), SynchronizationJob.class.getName()));
                    jobBuilder.setMinimumLatency(0);                // Specify that this job should be delayed by the provided amount of time.
                    jobBuilder.setOverrideDeadline(1000);           // Set deadline which is the maximum scheduling latency.
                    jobBuilder.setPersisted(false);                 // Set whether or not to persist this job across device reboots.

                    jobScheduler.schedule(jobBuilder.build());      // Schedule job to run
                }
            }
        });
    }

    /**
     * Runs a check if the api was last called more than a set limit
     * If it was than fingerprint meta data will be loaded
     *
     * @return true if the data should be updated
     */
    private boolean checkMetaRefresh() {
        // Get synchronization interval
        long refreshInterval = mConfiguration.getSynchronizationInterval();
        if(refreshInterval == 0) {
            // Default time is 1 day
            refreshInterval = 86400000;
        }

        // Calculate and return time difference in millis
        long diff = System.currentTimeMillis() - mConfiguration.getLastSynchronizationTime();
        return diff >= refreshInterval;
    }

    /**
     * Check if there is synchronization running.
     *
     * @return true/false if synchronization running.
     */
    private boolean isSynchronizationRunning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return jobScheduler.getPendingJob(SynchronizationJob.JOB_ID) != null;   // Check if job is pending based on id
        } else {
            // Check all the jobs and if one of the ids is the same then it is running
            List<JobInfo> jobs = jobScheduler.getAllPendingJobs();
            for (JobInfo job : jobs) {
                if (job.getId() == SynchronizationJob.JOB_ID)
                    return true;
            }

            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Run synchronization with the API
        if( checkMetaRefresh() ) {
            mFingerprintApi.getFingerprintMeta(mDevice.getTelephone(), mConfiguration.getLastDownloadTime());
        }
    }

    @Override
    protected void updateUI() {
        if(mMeta != null && mDevice != null) {
            // Get upload count based on meta data
            Long uploadCount = mDatabase.getUploadCount(mMeta.getLastInsert(), mDevice.getTelephone());

            // Set view texts
            mNewDownload.setText(String.valueOf(mMeta.getCountNew()));
            mNewUpload.setText(String.valueOf(uploadCount));
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_synchronization;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_synchronization;
    }

    @Override
    public void loadedFingerprintMeta(FingerprintMeta fingerprintMeta) {
        // Save meta into the configuration
        mConfiguration.setMeta(fingerprintMeta);
        mConfiguration.setLastSynchronizationTime(System.currentTimeMillis());
        Configuration.saveConfiguration(mConfiguration);

        // Set meta and update the screen
        mMeta = fingerprintMeta;
        updateUI();
    }

    @Override
    public void loadedFingerprints(int count) {
        // Not used
    }

    @Override
    public void postedFingerprints() {
        // Not used
    }

    @Override
    public void apiException(ApiException ex) {
        // Not used
    }
}
