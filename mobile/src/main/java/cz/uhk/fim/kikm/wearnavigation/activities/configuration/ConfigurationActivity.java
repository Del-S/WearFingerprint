package cz.uhk.fim.kikm.wearnavigation.activities.configuration;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.WearApplication;
import cz.uhk.fim.kikm.wearnavigation.model.api.ApiConnection;
import cz.uhk.fim.kikm.wearnavigation.model.api.FingerprintMeta;
import cz.uhk.fim.kikm.wearnavigation.model.api.SynchronizationJob;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ConfigurationActivity extends BaseActivity {

    private final static String TAG = "ConfigurationActivity";

    private Configuration mConfiguration;
    private DeviceEntry mDevice;
    private FingerprintMeta mMeta;
    private ApiConnection mFingerprintApi;
    private DatabaseCRUD mDatabase;
    private BroadcastReceiver mJobBroadcast;    // Receives information from Synchronization job

    private TextView mNewDownload, mNewUpload;
    private ImageButton mSynchronizationButton;
    private ConstraintLayout mSynchronization;
    private Animation mRotateAnimation;

    private long mDownloadCount = 0;
    private long mUploadCount = 0;

    private JobScheduler jobScheduler;          // JobScheduler used to run FingerprintScanner

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initiate data
        mConfiguration = Configuration.getConfiguration(this);
        mDevice = mConfiguration.getDevice(this);
        mDatabase = new DatabaseCRUD(this);
        mJobBroadcast = new SynchronizationJobReceiver();

        // Synchronization instances (api and jobScheduler)
        Retrofit retrofit = ((WearApplication) getApplicationContext()).getRetrofit();
        mFingerprintApi = retrofit.create(ApiConnection.class);
        jobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );

        // Initiate views
        mNewDownload = findViewById(R.id.as_new_download);
        mNewUpload = findViewById(R.id.as_new_upload);
        mSynchronization = findViewById(R.id.as_constraint_synchronization);
        mSynchronizationButton = findViewById(R.id.as_synchronization);
        // Load animation
        mRotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotation_infinite_counter_clock);
        // Initiate actions for views
        initiateViewActions();

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Run synchronization with the API
        if(checkMetaRefresh()) {
            runMetaRefresh();
        }

        // Add animation for sync button if it's running
        if(isSynchronizationRunning()) {
            mSynchronizationButton.setAnimation(mRotateAnimation);
        }

        // Register synchronization receiver
        IntentFilter intentFilter = new IntentFilter(SynchronizationJob.ACTION_JOB_DONE);
        intentFilter.addAction(SynchronizationJob.ACTION_JOB_UPDATE);
        registerReceiver(mJobBroadcast, intentFilter);
    }

    @Override
    protected void onPause() {
        // Try to unregister synchronization receiver
        try  {
            unregisterReceiver(mJobBroadcast);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Could not unregister receiver.", ex);
        }

        super.onPause();
    }

    @Override
    protected void updateUI() {
        mConfiguration = Configuration.getConfiguration(this);
        mMeta = mConfiguration.getMeta();
        if(mMeta != null && mDevice != null) {
            // Get download and upload count based on meta data
            mDownloadCount = mMeta.getCountNew();
            mUploadCount = mDatabase.getUploadCount(mMeta.getLastInsert(), mDevice.getTelephone());

            // Set view texts
            updateViews();
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

    private void initiateViewActions() {
        mSynchronization.setOnClickListener(v -> {
            if(!isSynchronizationRunning()) {
                // Building job to run
                JobInfo.Builder jobBuilder = new JobInfo.Builder(SynchronizationJob.JOB_ID,
                        new ComponentName(getPackageName(), SynchronizationJob.class.getName()));
                jobBuilder.setOverrideDeadline(1000);           // Set deadline which is the maximum scheduling latency.
                jobBuilder.setPersisted(false);                 // Set whether or not to persist this job across device reboots.

                jobScheduler.schedule(jobBuilder.build());      // Schedule job to run

                // Start animation of synchronization button
                mSynchronizationButton.startAnimation(mRotateAnimation);
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
     * Runs meta refresh call to the API.
     */
    private void runMetaRefresh() {
        if( mFingerprintApi != null ) {
            Call<FingerprintMeta> metaCall = mFingerprintApi.getFingerprintsMeta(mDevice.getTelephone(),
                    mConfiguration.getLastDownloadTime());
            metaCall.enqueue(mMetaCallback);
        }
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

    private void updateViews() {
        mNewDownload.setText(String.valueOf(mDownloadCount));
        mNewUpload.setText(String.valueOf(mUploadCount));
    }

    private Callback<FingerprintMeta> mMetaCallback = new Callback<FingerprintMeta>() {
        @Override
        public void onResponse(@NonNull  Call<FingerprintMeta> call, @NonNull Response<FingerprintMeta> response) {
            FingerprintMeta fingerprintMeta = response.body();
            if(fingerprintMeta != null) {
                // Save meta into the configuration
                mConfiguration.setMeta(fingerprintMeta);
                mConfiguration.setLastSynchronizationTime(System.currentTimeMillis());
                Configuration.saveConfiguration(mConfiguration);

                // Set meta and update the screen
                mMeta = fingerprintMeta;
                updateUI();
            }
        }

        @Override
        public void onFailure(@NonNull Call<FingerprintMeta> call, @NonNull Throwable t) {
            t.printStackTrace();
        }
    };

    /**
     * Informs activity that Synchronization Job is complete.
     */
    class SynchronizationJobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(SynchronizationJob.ACTION_JOB_DONE.equals(action)) {
                updateUI();

                // Load status from intent
                int status = intent.getIntExtra(SynchronizationJob.ACTION_DATA_STATE,
                        SynchronizationJob.JOB_STATE_FAILED);

                // Print out toast message if job finished or failed
                if(status == SynchronizationJob.JOB_STATE_FINISHED) {
                    Toast.makeText(ConfigurationActivity.this,
                            R.string.ca_synchronization_successful,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConfigurationActivity.this,
                            R.string.ca_synchronization_failed,
                            Toast.LENGTH_SHORT).show();
                }

                // Remove animation for sync button
                mSynchronizationButton.clearAnimation();
            } else if(SynchronizationJob.ACTION_JOB_UPDATE.equals(action)) {
                // Load counts from the intent
                mDownloadCount = intent.getLongExtra(SynchronizationJob.ACTION_DATA_DOWNLOAD,
                        mDownloadCount);
                mUploadCount = intent.getLongExtra(SynchronizationJob.ACTION_DATA_UPLOAD,
                        mUploadCount);

                // Set min for download count
                if(mDownloadCount < 0)
                    mDownloadCount = 0;

                // Set min for upload count
                if(mUploadCount < 0)
                    mUploadCount = 0;

                // Update view numbers in the activity
                updateViews();
            }
        }
    }
}
