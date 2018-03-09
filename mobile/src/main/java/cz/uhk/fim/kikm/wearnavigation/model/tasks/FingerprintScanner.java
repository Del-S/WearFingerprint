package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.util.SparseIntArray;

import com.google.gson.Gson;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.database.BeaconEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.CellularEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.SensorEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.WirelessEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;

/**
 * Scans for fingerprints and saves the data into the database.
 * Scans multiple sources via BroadcastReceivers:
 * - Bluetooth LE such as iBeacons
 * - Wireless such as access points
 * - Cellular such as cellular towers
 * It also sends scan progress with device counts via Broadcast.
 */
public class FingerprintScanner extends JobService {

    public static final int JOB_ID = 1;     // ID of this job in JobBuilder
    private static final String TAG = "FingerprintScanner"; // TAG for logging

    // Broadcast data Bundle keys
    public static final String ACTION_POST_PROGRESS = "scanProgress";   // Broadcast intent information
    public static final String ACTION_DATA = "data";                    // Broadcast count data for every device

    // Parameters send to this job as JobParameters
    public static final String PARAM_FINGERPRINT = "fingerprint";   // Bundle parameter name for fingerprint
    public static final String PARAM_LOCATION = "lastLocation";     // Bundle parameter name for last known location

    // States of this scanner
    private final int TASK_STATE_NONE = 0;            // Nothing is happening
    private final int TASK_STATE_STARTING = 1;        // Starting scan
    private final int TASK_STATE_RUNNING = 2;         // Scan is running
    public static final int TASK_STATE_DONE = 3;      // Scan finished

    private Gson mGson = new Gson();       // Json to Class parser
    private ScannerTask mScannerTask;      // Task that will run in this job
    private JobParameters mJobParams;      // Saving job params to cancel it in the future

    @Override
    public boolean onStartJob(JobParameters params) {
        mJobParams = params;        // Save job params

        // Parse json data into Fingerprint class
        String json = mJobParams.getExtras().getString(PARAM_FINGERPRINT);
        Fingerprint fingerprint = null;
        if (json != null && !json.isEmpty()) {
            fingerprint = mGson.fromJson(json, Fingerprint.class);
        }

        double[] lastLocation = mJobParams.getExtras().getDoubleArray(PARAM_LOCATION);  // Load last known location from parameters

        // If there is some fingerprint data we start the task
        if (fingerprint != null) {
            mScannerTask = new ScannerTask(fingerprint, fingerprint.getScanLength(), lastLocation);
            mScannerTask.execute();
            return true;
        }

        return false;   // Task was not started so return false
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mScannerTask.cancel(true);  // Cancel currently running task
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    private class ScannerTask extends AsyncTask<Void, Void, Fingerprint> {

        // Global scan variables
        private DatabaseCRUD mDatabase;              // Database helper for inserting data into the database
        private long mScanLength = 30000;            // Length of the current scan
        private long mStartTime;                     // Timestamp when scan was started
        private Fingerprint mFingerprint;            // Fingerprint data that will be saved into the database
        private ScanProgress mScanProgress;

        // Bluetooth scanner variables
        private BLEScannerManager mBLEScannerManager;      // Bluetooth scanner manager to run BLE scanner service
        private BeaconScanner mBeaconScanner;              // Bluetooth scanner to parse data in this job

        // Wifi scanner variables
        private WifiManager mWifiManager;            // Manager to get wifi data from
        private WifiScanner mWifiScanner;            // Scanner to parse data into WirelessEntries

        // Cellular scanner variables
        private TelephonyManager mCellularManager;   // Manager to get cellular data from
        private CellularScanner mCellularScanner;    // Scanner to parse data into CellularEntries
        private int currentCellularRSSI = 0;         // Cellular RSSI for GsmCellLocation
        private double[] mLastKnowLocation = {0, 0}; // Last known location to calculate distance between Cell tower and device

        // Sensor scanner variables
        private SensorManager mSensorManager;        // Manager to get sensor data from
        private SensorScanner mSensorScanner;        // Scanner to parse data into ScannerEntries
        private List<Sensor> sensors;                // List of collected sensors

        private int mState = TASK_STATE_NONE;          // Current state variable

        private int mThreadUpdateDelay = 1000;      // This job will post scan progress every 1 seconds
        // Delays for Wireless and Sensor scans
        private final int mSensorScanDelay = 5000;     // Every sensor will be added every 5second
        private final int mWirelessDelay = 5000;       // Wireless time delay in ms (5s = 5000ms). Connected to mThreadUpdateDelay.

        ScannerTask(Fingerprint fingerprint, long scanLength, double[] location) {
            Context context = getApplicationContext();      // Load application context to bind listeners, get managers, etc.

            // Global scan variables
            mDatabase = new DatabaseCRUD(context);      // Initiate database connection using context
            mScanLength = scanLength;                   // Set scan length
            mFingerprint = fingerprint;                 // Set fingerprint information

            // Initiate bluetooth scanner
            mBLEScannerManager = BLEScannerManager.getInstance(context);
            mBLEScannerManager.setScanPeriods(200L, 0L);   // Change scan periods
            mBeaconScanner = new BeaconScanner();
            context.registerReceiver(mBeaconScanner, new IntentFilter(BLEScannerManager.ACTION_BEACONS_FOUND));    // Register receiver to the context to listen for beacon data

            // Initiate wireless scanner
            mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mWifiScanner = new WifiScanner();           // Create instance of wifi scanner
            context.registerReceiver(mWifiScanner, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));    // Register receiver to the context to listen for wireless data

            // Initiate cellular scanner
            mLastKnowLocation = location;       // Set last known location
            mCellularManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            mCellularScanner = new CellularScanner();   // Create instance of cellular scanner
            mCellularManager.listen(mCellularScanner, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);  // Update current RSSI for GsmCellLocation

            // Initiate sensor manager and scanner
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            // Set sensor types to track
            sensors = new ArrayList<>();
            sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
            sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
            sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
            // Create instance of sensor scanner with specific sensor types
            mSensorScanner = new SensorScanner(sensors);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mState = TASK_STATE_STARTING;    // Set state to starting
            publishProgress();          // Update progress to change state

            // Try to bound BLE scanner
            if( !mBLEScannerManager.isBound() ) mBLEScannerManager.bindScanner();
        }

        @Override
        protected Fingerprint doInBackground(Void... voids) {
            // Checking if ble scanner service was bound or not (3 tries)
            int connectionTry = 3;
            while (!mBLEScannerManager.isBound() && connectionTry > 0) {
                try {
                    mBLEScannerManager.bindScanner();  // Try to bind scanner
                    Thread.sleep(200);    // 200 millisecond to wait for next try
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connectionTry--;
            }

            // Set time and state
            Log.i(TAG, "Fingerprint scan started.");
            mStartTime = System.currentTimeMillis();            // Set current time as start time
            mState = TASK_STATE_RUNNING;                        // Change state to running
            mFingerprint.setScanStart(mStartTime);              // Set scan start into fingerprint

            // Starting scans
            if (!mBLEScannerManager.isBound()) return null;  // Service is not bound then we finish the scan
            if (!mBLEScannerManager.startScan(mScanLength, true)) return null;   // Try to start BLE scan
            mWifiManager.startScan();                        // Start wifi scan
            // Bind sensor scanner to start sensor scans
            for (Sensor sensor : sensors) {
                mSensorManager.registerListener(mSensorScanner, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

            // Handles Thread sleeps so the job would wait until the scanTime is up
            // Also publishes the progress and start wireless scans in specific intervals
            int currentWirelessDelay = mWirelessDelay + mThreadUpdateDelay;     // Set first run delay
            while (System.currentTimeMillis() < mStartTime + mScanLength) {
                // If this task was not cancelled
                if (!isCancelled()) {
                    try {
                        // Runs a new Wireless scan that should run every X seconds
                        currentWirelessDelay -= mThreadUpdateDelay; // Remove time from current delay
                        if(currentWirelessDelay <= 0) {
                            mWifiManager.startScan();               // Start the scan again. Disabled at onPostExecute.
                            currentWirelessDelay = mWirelessDelay;  // Reset the delay
                        }

                        publishProgress();                  // Update progress information
                        Thread.sleep(mThreadUpdateDelay);   // Pause thread for a second
                    } catch (InterruptedException e) {
                        Log.e("FingerprintScanner", "Cannot run sleep() in interrupted thread", e);
                        mBLEScannerManager.cancelScan();   // Cancel scan if the task was canceled
                        return null;
                    }
                } else {
                    mBLEScannerManager.cancelScan();   // Cancel scan if the task was canceled
                    return null;
                }
            }

            return mFingerprint;    // Return calculated fingerprint
        }

        @Override
        protected void onPostExecute(Fingerprint fingerprint) {
            if (fingerprint != null) {
                // Complete fingerprint with scanEnd and save it into the database
                fingerprint.setScanEnd(System.currentTimeMillis());
                mDatabase.saveFingerprint(fingerprint, null, true);
            }

            // Unbinding the scanner service
            Context context = getApplicationContext();          // Load context to unregister received
            context.unregisterReceiver(mBeaconScanner);         // Unregister beacon receiver
            context.unregisterReceiver(mWifiScanner);           // Unregister wifi receiver
            mSensorManager.unregisterListener(mSensorScanner);  // Unregister scanner receiver
            mBLEScannerManager.handleDestroy();                 // Destroy ble scanner

            // Change and publish progress
            mState = TASK_STATE_DONE;    // Scan is done
            publishProgress();      // Publish progress after scan is done

            // Finish this job
            Log.i(TAG, "Fingerprint scan complete.");
            FingerprintScanner.this.jobFinished(mJobParams, false);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // Get scanLength (ms) as int
            int maxTime = (int) mScanLength;

            // Create ScanProgress instance if it does not exist
            if(mScanProgress == null) {
                mScanProgress = new ScanProgress();
            }

            // Set current variables into the ScanProgress
            mScanProgress.setState(mState);                          // Set current state
            mScanProgress.setStateString( getStateAsString() );      // Set current state (string) of this job
            mScanProgress.setScanLength( maxTime );                  // Set length of the scan (usually stays the same)
            mScanProgress.setCurrentTime( getCurrentTime(maxTime) ); // Set current time in the scan
            // Set entries count
            mScanProgress.setBeaconCount( mFingerprint.getBeaconEntries().size() );         // Sets beacon count
            mScanProgress.setWirelessCount( mFingerprint.getWirelessEntries().size() );     // Sets wireless counts
            mScanProgress.setCellularCount( mFingerprint.getCellularEntries().size() );     // Sets cellular count
            mScanProgress.setSensorCount( mFingerprint.getSensorEntries().size() );         // Sets sensor counts

            Intent intent = new Intent();               // Create broadcast intent
            intent.setAction(ACTION_POST_PROGRESS);     // Set intent action to get in BroadcastReceiver
            intent.putExtra(ACTION_DATA, mScanProgress);// Adds ScanProgress into the intent bundle to send it
            sendBroadcast(intent);                      // Send broadcast with data
        }

        /**
         * Returns current state of this task as a String to be displayed.
         *
         * @return String text of current state
         */
        @NonNull
        private String getStateAsString() {
            // Load context to load string from it
            Context context = getApplicationContext();

            // Return correct text for each state of this task
            switch (mState) {
                case TASK_STATE_NONE:
                    return context.getResources().getText(R.string.spo_status_none).toString();
                case TASK_STATE_STARTING:
                    return context.getResources().getText(R.string.spo_status_starting).toString();
                case TASK_STATE_RUNNING:
                    return context.getResources().getText(R.string.spo_status_running).toString();
                case TASK_STATE_DONE:
                    return context.getResources().getText(R.string.spo_status_done).toString();
                default:
                    return context.getResources().getText(R.string.spo_status_none).toString();
            }
        }

        /**
         * Calculates the current time based on status.
         * Used in progress bar to display progress.
         *
         * @param maxTime so we don't move over it
         * @return int milliseconds of current time
         */
        private int getCurrentTime(int maxTime) {
            switch (mState) {
                case TASK_STATE_DONE:
                    return maxTime;
                case TASK_STATE_RUNNING:
                    int currentTime = (int) (System.currentTimeMillis() - mStartTime);  // Calculate and set current time in milliseconds
                    if(currentTime > maxTime) {
                        currentTime = maxTime;
                    }
                    return currentTime;
                default:
                    return 0;
            }
        }

        /**
         * Receiver that handles Beacon scanner.
         * Parse Beacons into BeaconEntries.
         */
        class BeaconScanner extends BroadcastReceiver {

            /**
             * When Broadcast was received the data is parsed into the BeaconEntries
             *
             * @param context context that send Broadcast
             * @param intent of the broadcast
             */
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action != null && action.equals(BLEScannerManager.ACTION_BEACONS_FOUND)) {
                    if(intent.getExtras() != null) {
                        // Get beacon list from intent extras
                        ArrayList<Beacon> beacons = intent.getExtras().getParcelableArrayList(BLEScannerManager.ACTION_BEACONS_DATA);

                        if(beacons != null) {
                            List<BeaconEntry> beaconEntries = mFingerprint.getBeaconEntries();    // Load list of current beacons
                            long currentMillis = System.currentTimeMillis();                      // Get current milliseconds for calculation of times

                            // Create BeaconEntries from found beacons and add them to the list
                            for (Beacon beacon : beacons) {
                                beaconEntries.add(createBeaconEntry(beacon, currentMillis));
                            }
                        }
                    }
                }
            }

            /**
             * Create BeaconEntry from beacon.
             *
             * @param beacon        found beacon to get data from
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

                // Return new BeaconEntry
                return newBeacon;
            }

            /**
             * Calculates beacon scanDifference based on max scanTime and current scan time.
             *
             * @param scanTime actual scan time
             * @param bssid    beacon bssid
             * @return long scanDifference
             */
            private long calculateBeaconScanDifference(long scanTime, String bssid) {
                List<BeaconEntry> beacons = mFingerprint.getBeaconEntries();        // Load list of current beacons
                BeaconEntry tempBeacon = new BeaconEntry(bssid);                    // Create beacon with bssid
                long scanDifference = 0;                                            // Scan time that will be calculated

                // We get data only from a specific beacon
                if (beacons != null && beacons.contains(tempBeacon)) {
                    long beaconScanTime = 0;

                    for (BeaconEntry beacon : beacons) {
                        if (!beacon.equals(tempBeacon))
                            continue;                                 // Ignore different beacons
                        if (beacon.getScanTime() > beaconScanTime)
                            beaconScanTime = beacon.getScanTime();    // Higher scanTime gets saved
                    }

                    // Calculate actual scanDifference
                    scanDifference = scanTime - beaconScanTime;
                }

                // Return scanTimeDifference
                return scanDifference;
            }
        }

        /**
         * Receiver that handles Wifi scanner.
         * Parse Wifi networks into WirelessEntries.
         */
        class WifiScanner extends BroadcastReceiver {

            /**
             * When Broadcast was received the data is parsed into the WirelessEntries
             *
             * @param c context that send Broadcast
             * @param intent of the broadcast
             */
            public void onReceive(Context c, Intent intent) {
                List<WirelessEntry> wirelessEntries = mFingerprint.getWirelessEntries();        // Load list of current wireless entries
                // Calculate time variables
                long currentMillis = System.currentTimeMillis();
                long scanTime = currentMillis - mStartTime;

                for (ScanResult scanResult : mWifiManager.getScanResults()) {
                    // Create new WirelessEntry and set its data
                    WirelessEntry wirelessEntry = new WirelessEntry();
                    wirelessEntry.setSsid(scanResult.SSID);                   // Set wireless SSID
                    wirelessEntry.setBssid(scanResult.BSSID);                 // Set wireless BSSID
                    wirelessEntry.setRssi(scanResult.level);                  // Set wireless RSSI
                    wirelessEntry.setFrequency(scanResult.frequency);         // Set wireless Frequency
                    wirelessEntry.setChannelByFrequency(scanResult.frequency);// Parses frequency into channel number
                    // Calculated distance and times
                    wirelessEntry.setDistance((float) (Math.pow(10.0d, (27.55d - 40d * Math.log10(scanResult.frequency) + 6.7d - scanResult.level) / 20.0d) * 1000.0));
                    wirelessEntry.setTimestamp(currentMillis);      // Current timestamp set to entry
                    wirelessEntry.setScanTime(scanTime);            // Current scan time
                    wirelessEntry.setScanDifference(calculateWirelessScanDifference(scanTime, wirelessEntry));  // Calculated scan difference for the entry

                    // Add new WirelessEntry to the list
                    wirelessEntries.add(wirelessEntry);
                }

                mCellularScanner.scanForCellular(currentMillis, scanTime);      // Trigger scan for Cellular towers
            }

            /**
             * Calculates wireless scanDifference based on max scanTime and current scan time.
             *
             * @param scanTime      actual scan time
             * @param wirelessEntry current wireless entry to calculate difference for
             * @return long scanDifference
             */
            private long calculateWirelessScanDifference(long scanTime, WirelessEntry wirelessEntry) {
                List<WirelessEntry> wirelessEntries = mFingerprint.getWirelessEntries();        // Load list of current wireless entries
                long scanDifference = 0;                                                        // Scan time that will be calculated

                // We get data only from a specific wireless entries
                if (wirelessEntries != null && wirelessEntries.contains(wirelessEntry)) {
                    long wirelessScanTime = 0;

                    for (WirelessEntry tempWirelessEntry : wirelessEntries) {
                        if (!tempWirelessEntry.equals(wirelessEntry))
                            continue;                                              // Ignore different entries
                        if (tempWirelessEntry.getScanTime() > wirelessScanTime)
                            wirelessScanTime = tempWirelessEntry.getScanTime();    // Higher scanTime gets saved
                    }

                    // Calculate actual scanDifference
                    scanDifference = scanTime - wirelessScanTime;
                }

                // Return scanTimeDifference
                return scanDifference;
            }
        }

        /**
         * Receiver that handles Sensor scans.
         * Parse Sensor data into SensorEntries.
         */
        class SensorScanner implements SensorEventListener {

            SparseIntArray sensorTimer = new SparseIntArray();  // Enables time limitations for the onServiceChanged

            /**
             * Create instance of this class and sets what sensors are listened to.
             * For those sensors there is an array that limits addition to the list.
             * Every sensor is added once in 25 tries
             *
             * @param sensors to listen to
             */
            SensorScanner(List<Sensor> sensors) {
                for (Sensor sensor : sensors) {
                    sensorTimer.put(sensor.getType(), 0);   // Sets sensorTimer to default value (0) by sensors used
                }
            }

            /**
             * Parses sensor data into the SensorEntry.
             *
             * @param event to get sensor data from
             */
            @Override
            public final void onSensorChanged(SensorEvent event) {
                int sensorType = event.sensor.getType();    // Get sensor type

                // Calculate time variables
                long timestamp = System.currentTimeMillis();
                long currentScanTime = timestamp - mStartTime;
                int addedScanTime = sensorTimer.get(sensorType);

                if (addedScanTime == 0 || (currentScanTime - addedScanTime) >= mSensorScanDelay) {
                    // This ensured that new sensor record will be handled every 5seconds
                    sensorTimer.put(sensorType, (int) currentScanTime);

                    // Create new SensorEntry and set its data
                    List<SensorEntry> sensorEntries = mFingerprint.getSensorEntries();
                    SensorEntry sensorEntry = new SensorEntry();
                    sensorEntry.setType(sensorType);
                    sensorEntry.setX(event.values[0]);
                    sensorEntry.setY(event.values[1]);
                    sensorEntry.setZ(event.values[2]);
                    sensorEntry.setTimestamp(timestamp);
                    sensorEntry.setScanTime(currentScanTime);
                    sensorEntry.setScanDifference(calculateSensorScanDifference(currentScanTime, sensorEntry));

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
             * @param scanTime    actual scan time
             * @param sensorEntry current sensor entry to calculate difference for
             * @return long scanDifference
             */
            private long calculateSensorScanDifference(long scanTime, SensorEntry sensorEntry) {
                List<SensorEntry> sensorEntries = mFingerprint.getSensorEntries();        // Load list of current wireless entries
                long scanDifference = 0;                                                  // Scan time that will be calculated

                // We get data only from a specific wireless entries
                if (sensorEntries != null && sensorEntries.contains(sensorEntry)) {
                    long sensorScanTime = 0;

                    for (SensorEntry tempSensorEntry : sensorEntries) {
                        if (!tempSensorEntry.equals(sensorEntry))
                            continue;                                          // Ignore different entries
                        if (tempSensorEntry.getScanTime() > sensorScanTime)
                            sensorScanTime = tempSensorEntry.getScanTime();    // Higher scanTime gets saved
                    }

                    // Calculate actual scanDifference
                    scanDifference = scanTime - sensorScanTime;
                }

                // Return scanTimeDifference
                return scanDifference;
            }
        }

        /**
         * Class that handles Cellular scans.
         * Parse cellular data into CellularEntries.
         */
        class CellularScanner extends PhoneStateListener {

            /**
             * Gets different types of CellularInfo class and creates CellularEntry from it.
             *
             * @param currentMillis current timestamps
             * @param scanTime current scan time
             */
            void scanForCellular(long currentMillis, long scanTime) {
                // Run only if permission was granted
                if (ActivityCompat.checkSelfPermission(FingerprintScanner.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    List<CellularEntry> cellularEntries = mFingerprint.getCellularEntries();        // Load list of cellular entries to add to it

                    // If there are there are observed cell information from all radios.
                    if (mCellularManager.getAllCellInfo() != null) {
                        // Create CellularEntry for every Cell
                        for (CellInfo cellInfo : mCellularManager.getAllCellInfo()) {
                            CellularEntry cellularEntry = CellularEntry.createCellularEntry(cellInfo);  // Creates instance of CellularEntry (identification variables are set in the constructor)

                            // Set time variables
                            cellularEntry.setTimestamp(currentMillis);
                            cellularEntry.setScanTime(scanTime);
                            cellularEntry.setScanDifference(calculateWirelessScanDifference(scanTime, cellularEntry));  // Calculated scan difference for the entry

                            // Add entry to the list
                            cellularEntries.add(cellularEntry);
                        }
                    } else {
                        if (mCellularManager.getNeighboringCellInfo().size() >= 1) {
                            for (NeighboringCellInfo cellInfo : mCellularManager.getNeighboringCellInfo()) {
                                CellularEntry cellularEntry = new CellularEntry(cellInfo);      // Creates instance of CellularEntry (identification variables are set in the constructor)

                                // Set time variables
                                cellularEntry.setTimestamp(currentMillis);
                                cellularEntry.setScanTime(scanTime);
                                cellularEntry.setScanDifference(calculateWirelessScanDifference(scanTime, cellularEntry));  // Calculated scan difference for the entry

                                // Add entry to the list
                                cellularEntries.add(cellularEntry);
                            }
                        } else {
                            CellularEntry cellularEntry = new CellularEntry((GsmCellLocation) mCellularManager.getCellLocation(), currentCellularRSSI);     // Creates instance of CellularEntry (identification variables are set in the constructor)

                            // Set time variables
                            cellularEntry.setTimestamp(currentMillis);
                            cellularEntry.setScanTime(scanTime);
                            cellularEntry.setScanDifference(calculateWirelessScanDifference(scanTime, cellularEntry));  // Calculated scan difference for the entry

                            // Add entry to the list
                            cellularEntries.add(cellularEntry);
                        }
                    }
                }
            }

            /**
             * Calculates cellular scanDifference based on max scanTime and current scan time.
             *
             * @param scanTime      actual scan time
             * @param cellularEntry current cellular entry to calculate difference for
             * @return long scanDifference
             */
            private long calculateWirelessScanDifference(long scanTime, CellularEntry cellularEntry) {
                List<CellularEntry> cellularEntries = mFingerprint.getCellularEntries();        // Load list of current cellular entries
                long scanDifference = 0;                                                        // Scan time that will be calculated

                // We get data only from a specific wireless entries
                if (cellularEntries != null && cellularEntries.contains(cellularEntry)) {
                    long cellularScanTime = 0;

                    for (CellularEntry tempCellularEntry : cellularEntries) {
                        if (!tempCellularEntry.equals(cellularEntry))
                            continue;                                              // Ignore different entries
                        if (tempCellularEntry.getScanTime() > cellularScanTime)
                            cellularScanTime = tempCellularEntry.getScanTime();    // Higher scanTime gets saved
                    }

                    // Calculate actual scanDifference
                    scanDifference = scanTime - cellularScanTime;
                }

                // Return scanTimeDifference
                return scanDifference;
            }

            /**
             * Saves current RSSI for GsmCellLocation.
             *
             * @param signalStrength current RSSI
             */
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                // Calculates RSSI
                currentCellularRSSI = signalStrength.isGsm() ? (signalStrength.getGsmSignalStrength() != 99 ? signalStrength.getGsmSignalStrength() * 2 - 113 : signalStrength.getGsmSignalStrength()) : signalStrength.getCdmaDbm();
            }
        }
    }
}
