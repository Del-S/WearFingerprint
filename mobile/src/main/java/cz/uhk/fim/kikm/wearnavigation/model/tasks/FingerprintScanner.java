package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.database.BeaconEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.SensorEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.WirelessEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;

// TODO: github issue #18
// TODO: add cellular scanner
public class FingerprintScanner extends AsyncTask<Void, Void, Fingerprint> implements BluetoothLEScannerInterface {

    private BluetoothLEScanner mBluetoothLEScanner;
    private DatabaseCRUD mDatabase;
    private final int TIME = 30000;
    private long mStartTime;
    private boolean mServiceBound = false;
    private Fingerprint mFingerprint;

    private TelephonyManager mCellularManager;
    private Context mContext;

    private WifiManager mWifiManager;
    private WifiScanner mWifiScanner;

    private SensorManager mSensorManager;
    private SensorScanner mSensorScanner;
    private List<Sensor> sensors;

    private final int STATE_NONE = 0;
    private final int STATE_STARTTING = 1;
    private final int STATE_RUNNING = 2;
    private final int STATE_DONE = 3;
    private int mState = STATE_NONE;

    private int temDelay = 0;

    public FingerprintScanner(Context context, Fingerprint fingerprint) {
        mContext = context;
        mFingerprint = fingerprint;
        mBluetoothLEScanner = new BluetoothLEScanner(context, this);
        mDatabase = new DatabaseCRUD(context);

        mCellularManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiScanner = new WifiScanner();
        context.registerReceiver(mWifiScanner, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<>();
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
        mSensorScanner = new SensorScanner(sensors);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mState = STATE_STARTTING;
        publishProgress();
    }

    @Override
    protected Fingerprint doInBackground(Void... voids) {
        // Checking if ble scanner service was bound or not (3 tries)
        int connectionTry = 3;
        while(!mServiceBound && connectionTry > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            connectionTry--;
        }

        // Service is not bound
        if(!mServiceBound) {
            return null;
        }

        // Start scanning
        mState = STATE_RUNNING;
        mWifiManager.startScan();
        for (Sensor sensor : sensors) {
            mSensorManager.registerListener(mSensorScanner, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Until the scan time is up the thread will be put to sleep
        while (System.currentTimeMillis() < mStartTime + TIME) {
            if (!isCancelled()) {
                try {
                    publishProgress();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("FingerprintScanner", "Cannot run sleep() in interrupted thread", e);
                    return null;
                }
            } else {
                mBluetoothLEScanner.cancelScan();
                return null;
            }
        }

        // Return calculated fingerprint
        return mFingerprint;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        if(mState == STATE_STARTTING) {
            Toast.makeText(mContext, "Starting scanner", Toast.LENGTH_SHORT).show();
            return;
        }

        int beaconCount = mFingerprint.getBeaconEntries().size();
        int wirelessCount = mFingerprint.getWirelessEntries().size();
        int cellularCount = mFingerprint.getCellularEntries().size();
        int sensorCOunt = mFingerprint.getSensorEntries().size();

        if(mState == STATE_RUNNING && temDelay <= 0) {
            Toast.makeText(mContext, "Scanning... Found " + beaconCount + ", " + wirelessCount + ", " + cellularCount + ", " + sensorCOunt + ", " +  "(b,w,c,s).", Toast.LENGTH_SHORT).show();
            temDelay = 6;
        }
        temDelay--;

        if(mState == STATE_DONE) {
            Toast.makeText(mContext, "Scan done. Found " + beaconCount + ", " + wirelessCount + ", " + cellularCount + ", " + sensorCOunt + ", " +  "(b,w,c,s).", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPostExecute(Fingerprint fingerprint) {
        if(fingerprint != null) {
            mDatabase.saveFingerprint(fingerprint, null);

            Log.d("Scanner", "Beacon count: " + fingerprint.getBeaconEntries().size());
            Log.d("Scanner", "Wireless count: " + fingerprint.getWirelessEntries().size());
            Log.d("Scanner", "Cellular count: " + fingerprint.getCellularEntries().size());
            Log.d("Scanner", "Sensor count: " + fingerprint.getSensorEntries().size());
        }

        // Unbinding the scanner service
        mContext.unregisterReceiver(mWifiScanner);
        mSensorManager.unregisterListener(mSensorScanner);
        mBluetoothLEScanner.handleDestroy();

        mState = STATE_DONE;
        publishProgress();
    }

    @Override
    public void serviceConnected() {
        mBluetoothLEScanner.setScanPeriods(100L, 1000L);
        if(mBluetoothLEScanner.startScan(TIME)) {
            mServiceBound = true;
            // Set start time after service was connected
            mStartTime = System.currentTimeMillis();
        }
    }

    @Override
    public void foundBeacon(Beacon beacon) {
        List<BeaconEntry> beaconEntries = mFingerprint.getBeaconEntries();           // Load list of current beacons
        beaconEntries.add( createBeaconEntry(beacon, System.currentTimeMillis()) );   // Add new calculated BeaconEntry
    }

    @Override
    public void foundMultipleBeacons(Collection<Beacon> beacons) {
        List<BeaconEntry> beaconEntries = mFingerprint.getBeaconEntries();    // Load list of current beacons
        long currentMillis = System.currentTimeMillis();                      // Get current milliseconds for calculation of times

        // Create BeaconEntries from found beacons and add them to the list
        for (Beacon beacon : beacons) {
            beaconEntries.add( createBeaconEntry(beacon, currentMillis) );
        }
    }

    /**
     * Create BeaconEntry from beacon.
     *
     * @param beacon found beacon to get data from
     * @param currentMillis to calculate scanTime
     * @return BeaconEntry
     */
    private BeaconEntry createBeaconEntry(Beacon beacon, long currentMillis) {
        // Calculate time variables
        String bssid = beacon.getBluetoothAddress();
        long scanTime = currentMillis - mStartTime;
        long scanDifference = calculateBeaconScanDifference(scanTime, bssid);

        // Create new BeaconEntry and set data into it
        BeaconEntry newBeacon = new BeaconEntry();
        newBeacon.setBssid(bssid);
        newBeacon.setDistance((float) beacon.getDistance());
        newBeacon.setRssi(beacon.getRssi());
        newBeacon.setTimestamp(currentMillis);
        newBeacon.setScanTime(scanTime);
        newBeacon.setScanDifference(scanDifference);

        return newBeacon;
    }

    /**
     * Calculates beacon scanDifference based on max scanTime and current scan time.
     *
     * @param scanTime actual scan time
     * @param bssid beacon bssid
     * @return long scanDifference
     */
    private long calculateBeaconScanDifference(long scanTime, String bssid) {
        List<BeaconEntry> beacons = mFingerprint.getBeaconEntries();        // Load list of current beacons
        BeaconEntry tempBeacon = new BeaconEntry(bssid);                    // Create beacon with bssid
        long scanDifference = 0;                                            // Scan time that will be calculated

        // We get data only from a specific beacon
        if(beacons != null && beacons.contains(tempBeacon)) {
            long beaconScanTime = 0;

            for(BeaconEntry beacon : beacons) {
                if(!beacon.equals(tempBeacon)) continue;                // Ignore different beacons
                if(beacon.getScanTime() > beaconScanTime) beaconScanTime = beacon.getScanTime();    // Higher scanTime gets saved
            }

            // Calculate actual scanDifference
            scanDifference = scanTime - beaconScanTime;
        }
        return scanDifference;
    }

    /**
     * Receiver that handles Wifi scanner.
     * Parse Wifi networks into WirelessEntries.
     */
    class WifiScanner extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            List<WirelessEntry> wirelessEntries = mFingerprint.getWirelessEntries();        // Load list of current wireless entries
            // Calculate time variables
            long currentMillis = System.currentTimeMillis();
            long scanTime = currentMillis - mStartTime;

            for (ScanResult scanResult : mWifiManager.getScanResults()) {
                // Create new WirelessEntry and set its data
                WirelessEntry wirelessEntry = new WirelessEntry();
                wirelessEntry.setSsid(scanResult.SSID);                 // Set wireless SSID
                wirelessEntry.setBssid(scanResult.BSSID);               // Set wireless BSSID
                wirelessEntry.setRssi(scanResult.level);                // Set wireless RSSI
                wirelessEntry.setFrequency(scanResult.frequency);       // Set wireless Frequency
                // Set wireless channel width (api 23+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    wirelessEntry.setChannel(scanResult.channelWidth);
                }

                // Calculated distance and times
                wirelessEntry.setDistance((float) (Math.pow(10.0d, (27.55d - 40d * Math.log10(scanResult.frequency) + 6.7d - scanResult.level) / 20.0d) * 1000.0));
                wirelessEntry.setTimestamp(currentMillis);      // Current timestamp set to entry
                wirelessEntry.setScanTime(scanTime);            // Current scan time
                wirelessEntry.setScanDifference(calculateWirelessScanDifference(scanTime, wirelessEntry));  // Calculated scan difference for the entry

                // Add new WirelessEntry to the list
                wirelessEntries.add(wirelessEntry);
            }

            // Start the scan again. Disabled at onPostExecute.
            mWifiManager.startScan();
        }
    }

    /**
     * Calculates wireless scanDifference based on max scanTime and current scan time.
     *
     * @param scanTime actual scan time
     * @param wirelessEntry current wireless entry to calculate difference for
     * @return long scanDifference
     */
    private long calculateWirelessScanDifference(long scanTime, WirelessEntry wirelessEntry) {
        List<WirelessEntry> wirelessEntries = mFingerprint.getWirelessEntries();        // Load list of current wireless entries
        long scanDifference = 0;                                                        // Scan time that will be calculated

        // We get data only from a specific wireless entries
        if(wirelessEntries != null && wirelessEntries.contains(wirelessEntry)) {
            long wirelessScanTime = 0;

            for(WirelessEntry tempWirelessEntry : wirelessEntries) {
                if(!tempWirelessEntry.equals(wirelessEntry)) continue;                // Ignore different entries
                if(tempWirelessEntry.getScanTime() > wirelessScanTime) wirelessScanTime = tempWirelessEntry.getScanTime();    // Higher scanTime gets saved
            }

            // Calculate actual scanDifference
            scanDifference = scanTime - wirelessScanTime;
        }
        return scanDifference;
    }

    /**
     * Receiver that handles Sensor scans.
     * Parse Sensor data into SensorEntries.
     */
    class SensorScanner implements SensorEventListener {

        // Enables time limitations for the onServiceChanged
        SparseIntArray sensorTimer = new SparseIntArray();

        SensorScanner(List<Sensor> sensors) {
            for(Sensor sensor : sensors) {
                sensorTimer.put(sensor.getType(), 0);   // Sets sensorTimer to default value (0) by sensors used
            }
        }

        @Override
        public final void onSensorChanged(SensorEvent event) {
            int sensorType = event.sensor.getType();    // Get sensor type
            if(sensorTimer.get(sensorType) <= 0) {
                // This ensured that new sensor record will be handled every 3seconds
                // onSensorChanged is called every 200ms so 200 * 15 = 3000ms = 3s
                sensorTimer.put(sensorType, 15);

                // Calculate time variables
                long timestamp = System.currentTimeMillis();
                long scanTime = timestamp - mStartTime;

                // Create new SensorEntry and set its data
                List<SensorEntry> sensorEntries = mFingerprint.getSensorEntries();
                SensorEntry sensorEntry = new SensorEntry();
                sensorEntry.setType(sensorType);
                sensorEntry.setX(event.values[0]);
                sensorEntry.setY(event.values[1]);
                sensorEntry.setZ(event.values[2]);
                sensorEntry.setTimestamp(timestamp);
                sensorEntry.setScanTime(scanTime);
                sensorEntry.setScanDifference(calculateSensorScanDifference(scanTime, sensorEntry));

                // Add sensorEntry into the list
                sensorEntries.add(sensorEntry);
            }
            // Reduces timer by one for this sensorType
            sensorTimer.put(sensorType, (sensorTimer.get(sensorType) - 1));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Ignore
        }

        /**
         * Calculates sensor scanDifference based on max scanTime and current scan time.
         *
         * @param scanTime actual scan time
         * @param sensorEntry current sensor entry to calculate difference for
         * @return long scanDifference
         */
        private long calculateSensorScanDifference(long scanTime, SensorEntry sensorEntry) {
            List<SensorEntry> sensorEntries = mFingerprint.getSensorEntries();        // Load list of current wireless entries
            long scanDifference = 0;                                                  // Scan time that will be calculated

            // We get data only from a specific wireless entries
            if(sensorEntries != null && sensorEntries.contains(sensorEntry)) {
                long sensorScanTime = 0;

                for(SensorEntry tempSensorEntry : sensorEntries) {
                    if(!tempSensorEntry.equals(sensorEntry)) continue;                // Ignore different entries
                    if(tempSensorEntry.getScanTime() > sensorScanTime) sensorScanTime = tempSensorEntry.getScanTime();    // Higher scanTime gets saved
                }

                // Calculate actual scanDifference
                scanDifference = scanTime - sensorScanTime;
            }
            return scanDifference;
        }
    }
}
