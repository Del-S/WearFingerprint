package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;

public class BLEScannerManager implements BeaconConsumer {

    private static final String TAG = "BLEScannerManager";     // Tag used for debug and error

    private BeaconManager beaconManager;        // Load beacon manager for application
    private Region region = new Region("UHK", null, null, null);        // Scan specific region for beacons
    private Context mApplicationContext;        // Hold application context variable
    private boolean mIsScanning = false;        // Is searching check
    private boolean mIsPriority = false;        // Is this a priority scan
    private final Handler cancelHandler;

    // Broadcast data Bundle keys
    public static final String ACTION_SCANNER_BOUND = "scannerBound";   // Broadcast intent action for scanner bound
    public static final String ACTION_SCAN_STATE_CHANGE = "scanState";  // Broadcast intent action and data for changing of scanning state
    public static final String ACTION_BEACONS_FOUND = "beaconFound";    // Broadcast intent action for new beacons found
    public static final String ACTION_BEACONS_DATA = "beaconData";      // Broadcast data of new found beacons

    private RangeNotifier mRangeNotifier;

    /**
     * Singleton instance and its getter
     */
    @SuppressLint("StaticFieldLeak")
    private static BLEScannerManager sInstance = null;
    public static BLEScannerManager getInstance(Context context) {
        if(sInstance == null) {
            sInstance = new BLEScannerManager(context.getApplicationContext());
        }

        sInstance.bindScanner();    // Rebind this scanner
        return sInstance;
    }

    /**
     * Singleton constructor that initiates the instance and binds the service.
     *
     * @param context to save and use in the future
     */
    private BLEScannerManager(Context context) {
        mApplicationContext = context;

        // Get beacon manager instance for application
        beaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);

        // Bind consumer to beacon manager
        if(!beaconManager.isBound(this)) {
            beaconManager.bind(this);
        }

        // Create cancel handler
        cancelHandler = new Handler();
        // Build range notifier
        mRangeNotifier = (beacons, region) -> {
            if (beacons.size() > 0) {
                // Send broadcast with data only if there is application context
                if(mApplicationContext != null) {
                    // Create ArrayList because it implements Serializable
                    ArrayList<Beacon> foundBeacons = new ArrayList<>(beacons);

                    Intent intent = new Intent();                       // Create broadcast intent
                    intent.setAction(ACTION_BEACONS_FOUND);             // Set intent action to get in BroadcastReceiver
                    intent.putParcelableArrayListExtra(ACTION_BEACONS_DATA, foundBeacons); // Set beacons data into intent
                    mApplicationContext.sendBroadcast(intent);          // Send broadcast
                }
            }
        };
    }

    @Override
    public void onBeaconServiceConnect() {
        // Add range notifier
        if(mRangeNotifier != null && !beaconManager.getRangingNotifiers().contains(mRangeNotifier)) {
            beaconManager.addRangeNotifier(mRangeNotifier);
        }

        // Sends on bound Broadcast
        if(mApplicationContext != null) {
            Intent intent = new Intent();                // Create broadcast intent
            intent.setAction(ACTION_SCANNER_BOUND);      // Set intent action to get in BroadcastReceiver
            mApplicationContext.sendBroadcast(intent);   // Send broadcast
        }
    }

    @Override
    public Context getApplicationContext() {
        return mApplicationContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        changeScanState(false);
        mApplicationContext.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return mApplicationContext.bindService(intent, serviceConnection, i);
    }

    /**
     * Starts scan with duration timer and priority
     * If there is scan running and new scan is scheduled
     * it check priority and tries to start the scan.
     *
     * @param duration time to scan
     * @param priority is this priority scan
     * @return true/false if scan started or not
     */
    public boolean startScan(long duration, boolean priority) {
        // If there is a scan running and there is priority scan started we cancel current scan
        if (isScanning() && priority && !mIsPriority) {
            cancelScan();
        }

        // Try to start a new scan
        if(!isScanning()) {
            mIsPriority = priority;
            return startScan(duration);
        } else {
            return false;
        }
    }

    /**
     * Starts scan with duration timer.
     * Checks if there is a scan running or not.
     *
     * @param duration time to scan
     * @return true/false if scan started or not
     */
    private boolean startScan(long duration) {
        // Start the scan only if one is not running
        if(!isScanning()) {
            // Start searching and ranging devices
            try {
                // Starting scan
                beaconManager.startRangingBeaconsInRegion(region);
                changeScanState(true);
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot start beacon ranging.", e);
                return false;
            }

            // Stop ranging handler
            cancelHandler.postDelayed(this::cancelScan, duration);

            return true;
        }
        return false;
    }

    /**
     * Cancels running scan
     */
    public void cancelScan() {
        // Check if the beacon manager is bound
        if( beaconManager.isBound(this) ) {
            // If it is try to stop itt
            try {
                // Stopping scan
                beaconManager.stopRangingBeaconsInRegion(region);
                changeScanState(false);

                // Disable canceling of current scanning (only used if it was canceled prematurely)
                cancelHandler.removeCallbacksAndMessages(null);
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot stop beacon ranging.", e);
            }
        }
    }

    /**
     * When scan state changes it will send information Broadcast.
     *
     * @param currentScanState state to change to
     */
    private void changeScanState(boolean currentScanState) {
        if(mIsScanning != currentScanState) {
            if(mApplicationContext != null) {
                Intent intent = new Intent();                // Create broadcast intent
                intent.setAction(ACTION_SCAN_STATE_CHANGE);  // Set intent action to get in BroadcastReceiver
                mApplicationContext.sendBroadcast(intent);   // Send broadcast
            }
        }

        mIsScanning = currentScanState;     // Change state
    }

    /**
     * Check if scan for LE devices is running
     *
     * @return true/false
     */
    public boolean isScanning() {
        return mIsScanning;
    }

    /**
     * Checks if there is a priority scan running.
     *
     * @return true/false
     */
    public boolean isPriorityScan() {
        return mIsPriority;
    }

    /**
     * Checks if manager is bound the context
     *
     * @return is bound or not
     */
    public boolean isBound() {
        return beaconManager.isBound(this);
    }

    /**
     * Try to bind scanner service.
     */
    public void bindScanner() {
        if(!isBound()) {
            beaconManager.bind(this);
        }
    }

    /**
     * Handles activity destroy function
     */
    public void handleDestroy() {
        if (isBound()) {
            beaconManager.unbind(this);
        }
    }

    /**
     * Handles activity pause function
     */
    public void handlePause() {
        if (isBound()) beaconManager.setBackgroundMode(true);
    }

    /**
     * Handles activity resume function
     */
    public void handleResume() {
        if (isBound()) beaconManager.setBackgroundMode(false);
        else beaconManager.bind(this);
    }

    public void setScanPeriods(long foregroundPeriod, long backgroundPeriod) {
        beaconManager.setForegroundScanPeriod(foregroundPeriod);
        beaconManager.setBackgroundScanPeriod(backgroundPeriod);
    }
}
