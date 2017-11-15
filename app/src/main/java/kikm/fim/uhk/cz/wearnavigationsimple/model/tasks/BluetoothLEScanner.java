package kikm.fim.uhk.cz.wearnavigationsimple.model.tasks;

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

import java.util.Collection;

public class BluetoothLEScanner implements BeaconConsumer {
    // Tag used for debug and error
    private static final String TAG = "BluetoothLEScanner";
    // Load beacon manager for application
    private BeaconManager beaconManager;
    // Scan specific region for beacons
    private Region region = new Region("UHK", null, null, null);
    // Interface to communicate with starting class
    private BluetoothLEScannerInterface mInterface;
    // Hold application context variable
    private Context mApplicationContext;
    // Is searching check
    private boolean mIsScanning = false;

    public BluetoothLEScanner(Context applicationContext, BluetoothLEScannerInterface bleInterface) {
        mApplicationContext = applicationContext;
        mInterface = bleInterface;

        // Get beacon manager instance for application
        beaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);

        // Bind consumer to beacon manager
        beaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        //beaconManager.setForegroundBetweenScanPeriod(100L);

        // Add range notifier
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    if(beacons.size() == 1) {
                        mInterface.foundBeacon(beacons.iterator().next());
                    } else {
                        mInterface.foundMultipleBeacons(beacons);
                    }
                }
            }

        });

        mInterface.serviceConnected();
    }

    @Override
    public Context getApplicationContext() {
        return mApplicationContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        mIsScanning = false;
        mApplicationContext.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return mApplicationContext.bindService(intent, serviceConnection, i);
    }

    /**
     * Starts scan with duration timer
     *
     * @param duration time to scan
     * @return true/false if scan started or not
     */
    public boolean startScan(long duration) {
        // Start searching and ranging devices
        try {
            beaconManager.startRangingBeaconsInRegion(region);
            mIsScanning = true;
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot start beacon ranging.", e);
            return false;
        }

        // Disable LE scan after set duration
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start ranging
                try {
                    beaconManager.stopRangingBeaconsInRegion(region);
                    mIsScanning = false;
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot stop beacon ranging.", e);
                }
            }
        }, duration);

        return true;
    }

    /**
     * Cancels running scan
     *
     * @return true/false if scanning was canceled
     */
    public boolean cancelScan() {
        // Start ranging
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
            mIsScanning = false;
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot stop beacon ranging.", e);
            return false;
        }
    }

    /**
     * Check if scan for LE devices is running
     *
     * @return true/false
     */
    public boolean isScanning() {
        return mIsScanning;
    }
}
