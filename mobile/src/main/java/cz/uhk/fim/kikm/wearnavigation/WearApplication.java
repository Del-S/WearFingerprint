package cz.uhk.fim.kikm.wearnavigation;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cz.uhk.fim.kikm.wearnavigation.model.tasks.FingerprintScanner;
import cz.uhk.fim.kikm.wearnavigation.utils.bluetoothConnection.BluetoothConnectionService;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class WearApplication extends Application {

    private Configuration configuration;    // ApiConfiguration for the whole app
    private static BluetoothConnectionService sService; // Bluetooth communication service
    private BackgroundPowerSaver backgroundPowerSaver;  // Power saver for BeaconLibrary
    private JobInfo.Builder jobBuilder;                 // Specific job to run via JobScheduler
    private Retrofit retrofit;

    @Override
    public void onCreate() {
        configuration = Configuration.getConfiguration(this);
        super.onCreate();

        //BeaconManager.setDebug(true);     // Remove this after completing all scanning features
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);    // Load beacon manager instance to enable settings change
        // Enable beacon
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        backgroundPowerSaver = new BackgroundPowerSaver(this);      // This reduces bluetooth power usage by about 60% when application is not visible

        JobScheduler jobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );
        if(jobScheduler != null) {
            jobScheduler.cancel(FingerprintScanner.JOB_ID);
        }

        // Building job to run
        jobBuilder = new JobInfo.Builder(FingerprintScanner.JOB_ID,
                new ComponentName(getPackageName(), FingerprintScanner.class.getName()));
        jobBuilder.setOverrideDeadline(1000);           // Set deadline which is the maximum scheduling latency.
        jobBuilder.setPersisted(false);                 // Set whether or not to persist this job across device reboots.

        // Retrofit client with specific time limits
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(90, TimeUnit.MILLISECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Retrofit instance with background thread executor
        retrofit = new Retrofit.Builder()
                .baseUrl(Configuration.API_URL)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create())
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .build();
    }

    /**
     * Get instance of current configuration
     *
     * @return ApiConfiguration instance
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Set configuration to the application and save it to the SharedPreferences
     *
     * @param configuration to save into the app and sp
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        Configuration.saveConfiguration(configuration);
    }

    /**
     * Returns instance og Fingerprint job builder.
     *
     * @return JobInfo.Builder
     */
    public JobInfo.Builder getFingerprintJob() {
        return jobBuilder;
    }

    /**
     * Return application wide retrofit instance.
     *
     * @return retrofit instance
     */
    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * Get bluetooth connection service to be able to communicate with wear devices
     *
     * @param context
     * @return
     */
    public static BluetoothConnectionService getConnectionService(Context context) {
        if(sService == null) {
            // Construct communication service
            sService = new BluetoothConnectionService();
        }
        return sService;
    }
}
