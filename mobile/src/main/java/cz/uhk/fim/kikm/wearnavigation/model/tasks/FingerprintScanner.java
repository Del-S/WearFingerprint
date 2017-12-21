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
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.google.gson.Gson;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.database.BeaconEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.CellularEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.SensorEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.WirelessEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;

// TODO: github issue #23
public class FingerprintScanner extends JobService {

    // Broadcast data Bundle keys
    public static final String ACTION_POST_PROGRESS = "scanProgress";   // Broadcast intent information
    public static final String ACTION_STATE = "state";                  // Broadcast current state
    public static final String ACTION_DATA = "data";                    // Broadcast count data for every device
    public static final String ACTION_TIME_LENGTH = "timeLength";       // Broadcast length of current scan
    public static final String ACTION_TIME_CURRENT = "timeCurrent";     // Broadcast current time in scan

    // Parameters send to this job as JobParameters
    public static final String PARAM_FINGERPRINT = "fingerprint";   // Bundle parameter name for fingerprint
    public static final String PARAM_LOCATION = "lastLocation";     // Bundle parameter name for last known location
    public static final String PARAM_SCAN_LENGTH = "mScanLength";   // Bundle parameter name for length of the scan

    private Gson mGson = new Gson();                                // Json to Class parser
    private ScannerTask mScannerTask;                               // Task that will run in this job

    @Override
    public boolean onStartJob(JobParameters params) {
        // Parse json data into Fingerprint class
        String json = params.getExtras().getString(PARAM_FINGERPRINT);
        Fingerprint fingerprint = null;
        if (json != null && !json.isEmpty()) {
            fingerprint = mGson.fromJson(json, Fingerprint.class);
        }

        long scanLength = params.getExtras().getLong(PARAM_SCAN_LENGTH);            // Load scan length from parameters
        double[] lastLocation = params.getExtras().getDoubleArray(PARAM_LOCATION);  // Load last known location from parameters

        // If there is some fingerprint data we start the task
        if (fingerprint != null) {
            mScannerTask = new ScannerTask(fingerprint, scanLength, lastLocation);
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
    private class ScannerTask extends AsyncTask<Void, Void, Fingerprint> implements BLEScannerInterface {

        // Global scan variables
        private DatabaseCRUD mDatabase;              // Database helper for inserting data into the database
        private long mScanLength = 60000;            // Length of the current scan
        private long mStartTime;                     // Timestamp when scan was started
        private Fingerprint mFingerprint;            // Fingerprint data that will be saved into the database

        // Bluetooth scanner variables
        private BLEScanner mBLEScanner;              // Bluetooth scanner to scan for LE
        private boolean mServiceBound = false;       // Check if service is bound or not

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

        // States of this scanner
        private final int STATE_NONE = 0;            // Nothing is happening
        private final int STATE_STARTING = 1;        // Starting scan
        private final int STATE_RUNNING = 2;         // Scan is running
        private final int STATE_DONE = 3;            // Scan finished
        private int mState = STATE_NONE;             // Current state variable

        ScannerTask(Fingerprint fingerprint, long scanLength, double[] location) {
            Context context = getApplicationContext();      // Load application context to bind listeners, get managers, etc.

            // Global scan variables
            mDatabase = new DatabaseCRUD(context);      // Initiate database connection using context
            mScanLength = scanLength;                   // Set scan length
            mFingerprint = fingerprint;                 // Set fingerprint information

            // Initiate bluetooth scanner
            mBLEScanner = new BLEScanner(context, this);

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
            mState = STATE_STARTING;    // Set state to starting
            publishProgress();          // Update progress to change state
        }

        @Override
        protected Fingerprint doInBackground(Void... voids) {
            // Checking if ble scanner service was bound or not (3 tries)
            int connectionTry = 3;
            while (!mServiceBound && connectionTry > 0) {
                try {
                    // 200 millisecond to wait for next try
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connectionTry--;
            }

            // Service is not bound then we finish the scan
            if (!mServiceBound) {
                return null;
            }

            // Start scanning
            mState = STATE_RUNNING;         // Change state to running
            mWifiManager.startScan();       // Start wifi scanner
            // Bind sensor scanner
            for (Sensor sensor : sensors) {
                mSensorManager.registerListener(mSensorScanner, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

            // Until the scan time is up the thread will be put to sleep
            while (System.currentTimeMillis() < mStartTime + mScanLength) {
                if (!isCancelled()) {               // If this task is cancelled
                    try {
                        publishProgress();          // Update progress information
                        Thread.sleep(1000);   // Pause thread for a second
                    } catch (InterruptedException e) {
                        Log.e("FingerprintScanner", "Cannot run sleep() in interrupted thread", e);
                        return null;
                    }
                } else {
                    mBLEScanner.cancelScan();   // Cancel scan if the task was canceled
                    return null;
                }
            }

            return mFingerprint;    // Return calculated fingerprint
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // Create broadcast intent
            Intent intent = new Intent();
            intent.setAction(ACTION_POST_PROGRESS);             // Set intent action to get in BroadcastReceiver

            long currentTime = System.currentTimeMillis() - mStartTime; // Calculate and set current time
            intent.putExtra(ACTION_STATE, mState);              // Set current state to intent extra data
            intent.putExtra(ACTION_TIME_LENGTH, mScanLength);   // Set scan length to extra
            intent.putExtra(ACTION_TIME_CURRENT, currentTime);  // Set current time into extra


            // Load counts of devices found
            int beaconCount = mFingerprint.getBeaconEntries().size();       // Beacon count in fingerprint
            int wirelessCount = mFingerprint.getWirelessEntries().size();   // Wireless count in fingerprint
            int cellularCount = mFingerprint.getCellularEntries().size();   // Cellular count in fingerprint
            int sensorCount = mFingerprint.getSensorEntries().size();       // Sensor count in fingerprint
            // Make array of counts
            int[] data = {beaconCount, wirelessCount, cellularCount, sensorCount};
            intent.putExtra(ACTION_DATA, data);         // Set array into the intent extra

            // Send broadcast
            sendBroadcast(intent);
        }

        @Override
        protected void onPostExecute(Fingerprint fingerprint) {
            if (fingerprint != null) {
                // TODO: enable this when done
                //mDatabase.saveFingerprint(fingerprint, null);
            }

            // Unbinding the scanner service
            Context context = getApplicationContext();          // Load context to unregister received
            context.unregisterReceiver(mWifiScanner);           // Unregister wifi receiver
            mSensorManager.unregisterListener(mSensorScanner);  // Unregister scanner receiver
            mBLEScanner.handleDestroy();                        // Destroy ble scanner

            // Change and publish progress
            mState = STATE_DONE;    // Scan is done
            publishProgress();      // Publish progress after scan is done
        }

        @Override
        public void serviceConnected() {
            // When BLE scanner service is connected we can start the scan
            mBLEScanner.setScanPeriods(1000L, 2000L);   // Change scan periods
            if (mBLEScanner.startScan(mScanLength)) {    // If the scan was started
                mServiceBound = true;                    // Mark service as bound
                mStartTime = System.currentTimeMillis(); // Set start time after service was connected
            }
        }

        @Override
        public void foundBeacons(Collection<Beacon> beacons) {
            List<BeaconEntry> beaconEntries = mFingerprint.getBeaconEntries();    // Load list of current beacons
            long currentMillis = System.currentTimeMillis();                      // Get current milliseconds for calculation of times

            // Create BeaconEntries from found beacons and add them to the list
            for (Beacon beacon : beacons) {
                beaconEntries.add(createBeaconEntry(beacon, currentMillis));
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

                mCellularScanner.scanForCellular(currentMillis, scanTime);      // Trigger scan for Cellular towers

                mWifiManager.startScan();   // Start the scan again. Disabled at onPostExecute.
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
                if (sensorTimer.get(sensorType) <= 0) {
                    // This ensured that new sensor record will be handled every 5seconds
                    // onSensorChanged is called every 200ms so 200 * 25 = 5000ms = 5s
                    sensorTimer.put(sensorType, 25);

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
