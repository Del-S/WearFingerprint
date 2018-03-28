package cz.uhk.fim.kikm.wearnavigation;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import java.util.UUID;

import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.FingerprintScanner;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.ScanProgress;
import cz.uhk.fim.kikm.wearnavigation.utils.ParcelableUtils;
import cz.uhk.fim.kikm.wearnavigation.utils.ProgressBarAnimation;

public class MainActivity extends WearableActivity implements
        DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener {

    private static final String TAG = "MainActivity";   // Logging tag

    // Request permissions parameters
    private static final int REQUEST_ENABLE_BT = 1000;         // Bluetooth check request code
    private static final int REQUEST_PERMISSIONS = 1001;   // Request access to coarse location

    private TextView mIntro;                    // Intro before scan is initiated
    private RelativeLayout mProgressContent;    // Content holding all scanning progress data
    private TextView mProgressStatus;           // Status of scanning
    private ProgressBar mProgressBar;           // Progress bar of scanning
    private ProgressBarAnimation mProgressAnimation;    // ProgressBar animation class

    // Scan progress numbers
    private TextView mBlCount;                  // Count of bluetooth devices
    private TextView mWCount;                   // Count of wireless devices
    private TextView mCCount;                   // Count of cellular devices
    private TextView mSCount;                   // Count of sensor devices

    // Scanner variables
    private JobScheduler jobScheduler;          // JobScheduler used to run FingerprintScanner
    private JobInfo.Builder jobBuilder;         // Job builder for FingerprintScanner
    private ScannerProgressReceiver mReceiver;  // Scanner receiver instance
    private final Gson gson = new Gson();       // Class to json (and reverse) parser
    private LocationManager locationManager;    // Location manager to get location from the network

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setAmbientEnabled();    // Enables Always-on

        // Lad fingerprint scanner job instances
        jobBuilder = ((WearApplication) getApplicationContext()).getFingerprintJob();   // Load scanner job from Application
        // Load system services
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);    // Load location manager
        jobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );        // Load JobScheduler

        // Load displays
        mIntro = findViewById(R.id.am_intro);                       // Find view with into text
        mProgressContent = findViewById(R.id.am_progress_content);  // Find view containing all progress widgets
        mProgressStatus = findViewById(R.id.am_progress_status);    // Find view with scan status
        mProgressBar = findViewById(R.id.am_progress);              // Find view with progress bar

        // Load progress widgets
        mBlCount = findViewById(R.id.am_bluetooth_count);
        mWCount = findViewById(R.id.am_wireless_count);
        mCCount = findViewById(R.id.am_cellular_count);
        mSCount = findViewById(R.id.am_sensor_count);

        // Initiate progress bar animation
        mProgressAnimation = new ProgressBarAnimation(mProgressBar, 1000);

        checkBluetooth();           // Bluetooth check
        checkPermissions(); // Location permission check

        // Start dummy wifi scan
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null) {
            wifiManager.setWifiEnabled(true);
            wifiManager.startScan();                        // Start wifi scan
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register scanner receiver to display state of the scan
        IntentFilter filter = new IntentFilter(FingerprintScanner.ACTION_POST_PROGRESS);
        mReceiver = new ScannerProgressReceiver();
        this.registerReceiver(mReceiver, filter);

        // Instantiates clients without member variables, as clients are inexpensive to create and
        // won't lose their listeners. (They are cached and shared between GoogleApi instances.)
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);

        // Display views based on if there is a scan running
        if(jobScheduler.getPendingJob(FingerprintScanner.JOB_ID) != null) {
            displayScanView(true);
        } else {
            displayScanView(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister scanner receiver
        if(mReceiver != null) {
            try {
                this.unregisterReceiver(mReceiver);
            } catch (RuntimeException e) {
                Log.e("MainActivity", "Cannot unregister receiver.", e);
            }
        }

        // Remove all listeners for GoogleApis
        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent event) {
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEvents) {
        // Check all dataEvents if there are some to be handled
        for (DataEvent event : dataEvents) {
            // Working only with changed dataEvents
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Run scan only if proper dataEvent was changed
                String path = event.getDataItem().getUri().getPath();   // Get path of the event
                if (DataLayerListenerService.SCAN_PATH.equals(path)) {  // If the path is scan path
                    // Load data from dataMap
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());    // Get data map from event
                    // Load fingerprint data from DataMap via Parcel
                    Fingerprint fingerprint = ParcelableUtils.getParcelable(dataMapItem.getDataMap(),
                            DataLayerListenerService.SCAN_DATA,
                            Fingerprint.CREATOR);
                    // Set device entry to this fingerprint
                    fingerprint.setDeviceEntry(DeviceEntry.createInstance(this));

                    // Schedule new Fingerprint scan
                    runFingerprintScanner(fingerprint);
                } else {
                    Log.i(TAG, "Unrecognized path in onDataChanged(): " + path);
                }
            }
        }
    }

    /**
     * Builds and runs the FingerprintScanner job.
     * Creates Fingerprint based on position in the map and sends it into the scanner.
     *
     * @param fingerprint to save scan data to
     */
    private void runFingerprintScanner(Fingerprint fingerprint) {
        // Check if job is already running or scheduled (do not run a new one)
        if(jobScheduler.getPendingJob(FingerprintScanner.JOB_ID) != null) {
            Toast.makeText(this, R.string.am_scan_already_running, Toast.LENGTH_SHORT).show();
            return;
        }

        fingerprint.setId(UUID.randomUUID());   // Creating new id for this scan

        // Vibrate to notify user
        Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); // Get instance of Vibrator from current Context
        if(vib != null) {
            vib.vibrate(100);     // Vibrate for 100 milliseconds
        }
        // Show status of the scan
        displayScanView(true);

        // Getting last knows location from Network
        double[] lastKnownLocation = {0, 0};
        if (locationManager != null &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(location != null) {
                lastKnownLocation[0] = location.getLatitude();
                lastKnownLocation[1] = location.getLongitude();
            }
        }

        // Create instance of scanner and start it with execute
        String jsonFinger = gson.toJson(fingerprint);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(FingerprintScanner.PARAM_FINGERPRINT, jsonFinger);
        bundle.putDoubleArray(FingerprintScanner.PARAM_LOCATION, lastKnownLocation);

        // Run the job
        jobBuilder.setExtras(bundle);                   // Set extra bundle data
        jobScheduler.schedule(jobBuilder.build());      // Schedule job to run
    }

    /**
     * Changes display or hide based on if the scan us running or not.
     *
     * @param show true/false
     */
    private void displayScanView(boolean show) {
        if(show) {
            // Hide Intro and show Progress
            mIntro.setVisibility(View.GONE);
            mProgressContent.setVisibility(View.VISIBLE);
        } else {
            // Hide Progress and show Intro
            mIntro.setVisibility(View.VISIBLE);
            mProgressContent.setVisibility(View.GONE);

            // Reset values to default
            mProgressStatus.setText(R.string.am_status_creating);
            mProgressBar.setMax(100);
            mProgressBar.setProgress(0);

            // Reset progress views
            mBlCount.setText(String.valueOf(0));
            mWCount.setText(String.valueOf(0));
            mCCount.setText(String.valueOf(0));
            mSCount.setText(String.valueOf(0));
        }
    }

    /**
     * This receiver gets information from currently running Fingerprint scan and displays it.
     */
    public class ScannerProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Check action if it's progress
            String action = intent.getAction();
            if(action != null && action.equals(FingerprintScanner.ACTION_POST_PROGRESS)) {
                if(intent.getExtras() != null) {
                    // Get scan progress from intent
                    ScanProgress scanProgress = intent.getExtras().getParcelable(FingerprintScanner.ACTION_DATA);

                    // If scan progress is not empty we can work with it
                    if (scanProgress != null) {
                        // Calculate display numbers
                        int progressMax = scanProgress.getScanLength();
                        int progressCurrent = scanProgress.getCurrentTime();
                        // Load display progress numbers for all types
                        int blCount = scanProgress.getBeaconCount();
                        int wCount = scanProgress.getWirelessCount();
                        int cCount = scanProgress.getCellularCount();
                        int sCount = scanProgress.getSensorCount();

                        // Display new status information
                        mProgressStatus.setText(scanProgress.getStateString());
                        mProgressBar.setMax(progressMax);
                        mProgressAnimation.setProgress(progressCurrent);

                        // Reset progress views
                        mBlCount.setText(String.valueOf(blCount));
                        mWCount.setText(String.valueOf(wCount));
                        mCCount.setText(String.valueOf(cCount));
                        mSCount.setText(String.valueOf(sCount));

                        // Hide this view after completion (5 seconds)
                        if(scanProgress.getState() == FingerprintScanner.TASK_STATE_DONE
                                || scanProgress.getState() == FingerprintScanner.TASK_STATE_FAILED) {
                            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); // Get instance of Vibrator from current Context
                            if(vib != null) {
                                vib.vibrate(100);     // Vibrate for 100 milliseconds
                            }

                            // Rest view for next scan
                            Handler hideHandler = new Handler();
                            hideHandler.postDelayed(() -> {
                                displayScanView(false);      // Reset view
                                finish();                         // Close the activity
                            }, 3000);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checking if the device has bluetooth and if it is enabled.
     */
    protected void checkBluetooth() {
        // Loading bluetooth adapter to figure out if the device has bluetooth
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.am_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Check if the Bluetooth is enabled
        if(bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                checkBle();
            }
        }
    }

    /**
     * Checking to determine whether BLE is supported on the device.
     * - If it is not then display message and quit
     */
    protected void checkBle() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.am_bluetooth_le_support, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Asks for permission to access location and telephone state
     */
    protected void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PERMISSIONS);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode != RESULT_OK) {
                    Toast.makeText(this, R.string.am_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    checkBle();
                }
                break;
        }
    }
}
