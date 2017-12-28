package cz.uhk.fim.kikm.wearnavigation;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import cz.uhk.fim.kikm.wearnavigation.model.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.LocationEntry;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.FingerprintScanner;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.ScanProgress;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection.BluetoothConnectionHandler;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection.BluetoothConnectionInterface;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection.BluetoothConnectionService;

public class MainActivity extends WearableActivity implements BluetoothConnectionInterface {

    // Request permissions parameters
    private final int REQUEST_ENABLE_BT = 1000;         // Bluetooth check request code
    private final int REQUEST_ACCESS_LOCATION = 1001;   // Request access to coarse location

    // Bluetooth device communication
    private final Handler mHandler = new BluetoothConnectionHandler(this);  // Handler for Bluetooth connection service using this as interface
    private BluetoothConnectionService mService = null;

    // Scanner count to display
    private TextView mBluetoothCount;       // Display Bluetooth device count
    private TextView mWirelessCount;        // Display Wireless device count
    private TextView mCellularCount;        // Display Cellular tower count
    private Button mRunScan;                // Runs a new fingerprint scan

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

        // Load count widgets
        mBluetoothCount = findViewById(R.id.am_bluetooth_count);
        mWirelessCount = findViewById(R.id.am_wireless_count);
        mCellularCount = findViewById(R.id.am_cellular_count);

        // Run fingerprint scanner job
        jobBuilder = ((WearApplication) getApplicationContext()).getFingerprintJob();   // Load scanner job from Application
        mRunScan = findViewById(R.id.am_run_scan);      // Find button to trigger the scan
        mRunScan.setOnClickListener(v -> {              // Add onClick listener to run the scan
            runFingerprintScanner(0,0);      // Trigger the scan with dummy values
            mRunScan.setEnabled(false);                 // Disable multiple scan runs
        });

        // Load system services
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);    // Load location manager
        jobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );        // Load JobScheduler

        // Enables Always-on
        setAmbientEnabled();

        checkBluetooth();           // Bluetooth check
        checkLocationPermissions(); // Location permission check
    }

    /**
     * Asks for permission to access Coarse and Fine location
     */
    protected void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_LOCATION);
        }
    }

    /**
     * Builds and runs the FingerprintScanner job.
     * Creates Fingerprint based on position in the map and sends it into the scanner.
     *
     * @param posX in the map
     * @param posY in the map
     */
    private void runFingerprintScanner(int posX, int posY) {
        // Create fingerprint for scanning
        Fingerprint fingerprint = new Fingerprint();
        fingerprint.setLocationEntry(new LocationEntry("J3NP"));
        fingerprint.setX(posX);
        fingerprint.setY(posY);

        // Getting last knows location from Network
        double[] lastKnownLocation = {0, 0};
        if (locationManager != null &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            lastKnownLocation[0] = location.getLatitude();
            lastKnownLocation[1] = location.getLongitude();
        }

        long scanLength = 60000;    // Length of the scan

        // Create instance of scanner and start it with execute
        String jsonFinger = gson.toJson(fingerprint);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(FingerprintScanner.PARAM_FINGERPRINT, jsonFinger);
        bundle.putDoubleArray(FingerprintScanner.PARAM_LOCATION, lastKnownLocation);
        bundle.putLong(FingerprintScanner.PARAM_SCAN_LENGTH, scanLength);

        // Run the job
        jobBuilder.setExtras(bundle);                   // Set extra bundle data
        jobScheduler.schedule(jobBuilder.build());      // Schedule job to run
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Bind to BluetoothConnectionService
        Intent intent = new Intent(this, BluetoothConnectionService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Register scanner receiver to display state of the scan
        IntentFilter filter = new IntentFilter(FingerprintScanner.ACTION_POST_PROGRESS);
        mReceiver = new ScannerProgressReceiver();
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);

        // Unregister scanner receiver
        if(mReceiver != null) {
            try {
                this.unregisterReceiver(mReceiver);
            } catch (RuntimeException e) {
                Log.e("MainActivity", "Cannot unregister receiver.", e);
            }
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

                    if (scanProgress != null) {
                        // Display count numbers
                        mBluetoothCount.setText(String.valueOf(scanProgress.getBeaconCount()));
                        mWirelessCount.setText(String.valueOf(scanProgress.getWirelessCount()));
                        mCellularCount.setText(String.valueOf(scanProgress.getCellularCount()));

                        // Enable new scan if current one is done
                        if(scanProgress.getState() == FingerprintScanner.TASK_STATE_DONE) {
                            mRunScan.setEnabled(true);
                        }
                    }
                }
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((BluetoothConnectionService.LocalBinder)iBinder).getInstance();
            mService.setHandler(mHandler);

            // Connect to the device
            BluetoothDevice device = null;
            if(device != null) {
                mService.connect(device);
            } else {
                mService.start();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };

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

    @Override
    public void deviceConnected(BluetoothDevice device) {
        Toast.makeText(this, "Connected device: " + device.getAddress(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void connectionFailed(BluetoothDevice device) {
        Toast.makeText(this, "Connection failed device: " + device.getAddress(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void connectionFailed() {
        Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void messageReceived(String message) {
        Toast.makeText(this, "You got a message: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void messageSend(String message) {
        Toast.makeText(this, "You send a message: " + message, Toast.LENGTH_SHORT).show();
    }
}
