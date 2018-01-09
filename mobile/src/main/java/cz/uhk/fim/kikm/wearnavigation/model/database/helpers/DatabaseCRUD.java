package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.uhk.fim.kikm.wearnavigation.model.database.BeaconEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.CellularEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.LocationEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.SensorEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.WirelessEntry;

public class DatabaseCRUD {

    private final static String TAG = "DatabaseCRUD";
    private final DatabaseHelper dbHelper;
    private Context c;

    public DatabaseCRUD(Context context) {
        this.c = context;
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Saves multiple fingerprints from the list.
     * Each one is saved in separate transaction.
     *
     * @param fingerprints List to save
     */
    public void saveMultipleFingerprints(List<Fingerprint> fingerprints) {
        if(fingerprints != null && !fingerprints.isEmpty()) {
            SQLiteDatabase db = dbHelper.getWritableDatabase(); // Load sql database via helper
            for (Fingerprint fingerprint : fingerprints) {
                saveFingerprint(fingerprint, db);
            }
            db.close();
        }
    }

    /**
     * Saves fingerprint all containing data like devices and location.
     * This is main function for saving.
     *
     * @param fingerprint to save into the database
     */
    public void saveFingerprint(Fingerprint fingerprint, SQLiteDatabase db) {
        boolean close = false;
        if(db == null) {
            db = dbHelper.getWritableDatabase(); // Load sql database via helper
            close = true;                        // We should also close connection at the end of this function
        }

        if(db != null && fingerprint != null) {
            try {
                db.beginTransaction();  // Start transaction
                // Check if fingerprint already exists in the database by its UUID
                if (fingerprint.getId() != null && !doesFingerprintExist(db, fingerprint.getId().toString())) {
                    boolean error = false;  // Check if there was some error

                    long locationId = saveLocationEntry(db, fingerprint.getLocationEntry());    // Save location and get its id
                    long deviceId = saveDeviceEntry(db, fingerprint.getDeviceEntry());          // Save device and get its id

                    // If location was not saves we should not continue
                    if (locationId >= 0) {
                        long fingerprintId = saveFingerprintEntry(db, fingerprint, locationId, deviceId);   // Save fingerprint and get its id
                        if (fingerprintId >= 0) {
                            saveBeaconEntries(db, fingerprint.getBeaconEntries(), fingerprintId);       // Save list of beaconEntries
                            saveWirelessEntries(db, fingerprint.getWirelessEntries(), fingerprintId);   // Save list of wirelessEntries
                            saveCellularEntries(db, fingerprint.getCellularEntries(), fingerprintId);   // Save list of cellularEntries
                            saveSensorEntries(db, fingerprint.getSensorEntries(), fingerprintId);       // Save list of sensorEntries
                        } else {
                            error = true;           // There was an error in transaction
                        }
                    } else {
                        error = true;               // There was an error in transaction
                    }

                    // If there was no error we mark transaction as successful
                    if (!error) {
                        Log.i(TAG,"Transaction successful in saveFingerprint().");
                        db.setTransactionSuccessful();
                    } else {
                        Log.e(TAG, "Transaction error in saveFingerprint().");
                    }
                } else {
                    Log.e(TAG, "Error in saveFingerprint(). Fingerprint already exists.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in saveFingerprint().", e);
            } finally {
                db.endTransaction();
                if(close) {
                    db.close();
                }
            }
        }
    }

    /**
     * Checks if the fingerprint exists by it's UUID
     *
     * @param db to check in
     * @param fingerprintId to find in the database
     * @return boolean if exits
     */
    private boolean doesFingerprintExist(SQLiteDatabase db, String fingerprintId) {
        // SQL parameters
        String[] columns = { Fingerprint.DB_FINGERPRINT_ID };           // We can select only one column
        String selection = Fingerprint.DB_FINGERPRINT_ID + " = ?";      // SQL WHERE clause for fingerprint id
        String[] selectionArgs = new String[] {fingerprintId};          // Which id it should look for
        String limit = "1";                                             // We can only select one row

        // Try to load row from database using query
        Cursor cursor = db.query(Fingerprint.DB_TABLE, columns, selection, selectionArgs, null, null, null, limit);
        boolean exists = (cursor.getCount() > 0);           // If count is higher then 0 there is a fingerprint in the database
        cursor.close();                                     // Close the curson because we dont need it anymore

        return exists;
    }

    /**
     * Saves only the fingerprint into the database.
     * Does not care about other variables.
     * This is only to save raw fingerprint.
     *
     * @param db to save to
     * @param fingerprint to save
     * @param locationId fingerprint has this location
     * @param deviceId fingerprint has this device
     * @return id of saved fingerprint
     */
    private long saveFingerprintEntry(SQLiteDatabase db, Fingerprint fingerprint, long locationId, long deviceId) {
        // Creating db values
        ContentValues values = new ContentValues();
        values.put(Fingerprint.DB_FINGERPRINT_ID, fingerprint.getId().toString());
        values.put(Fingerprint.DB_FINGERPRINT_SCAN_ID, fingerprint.getScanID().toString());
        values.put(Fingerprint.DB_X, fingerprint.getX());
        values.put(Fingerprint.DB_Y, fingerprint.getY());
        values.put(Fingerprint.DB_SCAN_LENGTH, fingerprint.getScanLength());
        values.put(Fingerprint.DB_SCAN_START, fingerprint.getScanStart());
        values.put(Fingerprint.DB_SCAN_END, fingerprint.getScanEnd());
        values.put(Fingerprint.DB_LEVEL, fingerprint.getLevel());
        values.put(Fingerprint.DB_LOCATION_ID, locationId);
        values.put(Fingerprint.DB_DEVICE_ID, deviceId);
        return db.insert(Fingerprint.DB_TABLE, null, values);
    }

    /**
     * Saves LocationEntry into the SQLite database.
     * First checks if the location exists or not.
     *
     * @param db database to save to
     * @param location to save
     * @return long location id
     */
    private long saveLocationEntry(SQLiteDatabase db, LocationEntry location) {
        long result = -1;

        // Check if the location already exists in the database
        String selectQuery = "SELECT  " +
                LocationEntry.DB_ID + ',' +
                LocationEntry.DB_BUILDING + ',' +
                LocationEntry.DB_FLOOR +
                " FROM " + LocationEntry.DB_TABLE;

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            LocationEntry locationEntry = new LocationEntry();
            locationEntry.setId( cursor.getInt(cursor.getColumnIndex(LocationEntry.DB_ID)) );
            locationEntry.setBuilding( cursor.getString(cursor.getColumnIndex(LocationEntry.DB_BUILDING)) );
            locationEntry.setFloor( cursor.getInt(cursor.getColumnIndex(LocationEntry.DB_FLOOR)) );
            if( locationEntry.equals(location) ) {
                result = locationEntry.getId();
            }
        }
        cursor.close();

        // If the device does not exist we create it
        if(result == -1) {
            // Create content values for SQL
            ContentValues values = new ContentValues();
            values.put(LocationEntry.DB_BUILDING, location.getBuilding());
            values.put(LocationEntry.DB_FLOOR, location.getFloor());

            // Insert values into the database
            result = db.insert(LocationEntry.DB_TABLE, null, values);
        }

        return result;
    }

    /**
     * Saves DeviceEntry into the SQLite database.
     * First checks if the device exists or not.
     *
     * @param db database to save to
     * @param device to save
     * @return long device id
     */
    private long saveDeviceEntry(SQLiteDatabase db, DeviceEntry device) {
        long result = -1;

        // Check if the device already exists in the database
        String selectQuery = "SELECT  " +
                DeviceEntry.DB_DEVICE_ID +
                " FROM " + DeviceEntry.DB_TABLE;

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            result = cursor.getLong(cursor.getColumnIndex(DeviceEntry.DB_DEVICE_ID));
        }
        cursor.close();

        // If the device does not exist we create it
        if(result == -1) {
            // Create content values for SQL
            ContentValues values = new ContentValues();
            values.put(DeviceEntry.DB_TYPE, device.getType());
            values.put(DeviceEntry.DB_DEVICE_ID, device.getDeviceId());
            values.put(DeviceEntry.DB_DEVICE_NAME, device.getDeviceName());
            values.put(DeviceEntry.DB_MODEL, device.getModel());
            values.put(DeviceEntry.DB_BRAND, device.getBrand());
            values.put(DeviceEntry.DB_MANUFACTURER, device.getManufacturer());
            values.put(DeviceEntry.DB_DISPLAY, device.getDisplay());
            values.put(DeviceEntry.DB_HARDWARE, device.getHardware());
            values.put(DeviceEntry.DB_SERUAL_NUMBER, device.getSerialNumber());
            values.put(DeviceEntry.DB_DEVIDE_FINGERPRINT, device.getDeviceFingerprint());
            values.put(DeviceEntry.DB_OS, device.getOs());
            values.put(DeviceEntry.DB_API, device.getApi());

            // Insert values into the database
            result = db.insert(DeviceEntry.DB_TABLE, null, values);
        }

        return result;
    }

    /**
     * Save multiple beaconEntries to the database.
     * Should be run from the single transaction and not use without it.
     *
     * @param db to save to
     * @param beaconEntries list to save
     * @param fingerprintId to bind single beaconEntry to
     */
    private void saveBeaconEntries(SQLiteDatabase db, List<BeaconEntry> beaconEntries, long fingerprintId) {
        if(beaconEntries != null && !beaconEntries.isEmpty())
            for(BeaconEntry beaconEntry : beaconEntries) {

                // Create content values for SQL
                ContentValues values = new ContentValues();
                values.put(BeaconEntry.DB_FINGERPRINT_DB_ID, fingerprintId);
                values.put(BeaconEntry.DB_BSSID, beaconEntry.getBssid());
                values.put(BeaconEntry.DB_DISTANCE, beaconEntry.getDistance());
                values.put(BeaconEntry.DB_RSSI, beaconEntry.getRssi());
                values.put(BeaconEntry.DB_TIMESTAMP, beaconEntry.getTimestamp());
                values.put(BeaconEntry.DB_SCAN_TIME, beaconEntry.getScanTime());
                values.put(BeaconEntry.DB_SCAN_DIFFERENCE, beaconEntry.getScanDifference());

                // Insert values into the database
                db.insert(BeaconEntry.DB_TABLE, null, values);
            }
    }

    /**
     * Save multiple wirelessEntries to the database.
     * Should be run from the single transaction and not use without it.
     *
     * @param db to save to
     * @param wirelessEntries list to save
     * @param fingerprintId to bind single wirelessEntry to
     */
    private void saveWirelessEntries(SQLiteDatabase db, List<WirelessEntry> wirelessEntries, long fingerprintId) {
        if(wirelessEntries != null && !wirelessEntries.isEmpty())
            for(WirelessEntry wirelessEntry : wirelessEntries) {

                // Create content values for SQL
                ContentValues values = new ContentValues();
                values.put(WirelessEntry.DB_FINGERPRINT_DB_ID, fingerprintId);
                values.put(WirelessEntry.DB_SSID, wirelessEntry.getSsid());
                values.put(WirelessEntry.DB_BSSID, wirelessEntry.getBssid());
                values.put(WirelessEntry.DB_RSSI, wirelessEntry.getRssi());
                values.put(WirelessEntry.DB_FREQUENCY, wirelessEntry.getFrequency());
                values.put(WirelessEntry.DB_CHANNEL, wirelessEntry.getChannel());
                values.put(WirelessEntry.DB_DISTANCE, wirelessEntry.getDistance());
                values.put(WirelessEntry.DB_TIMESTAMP, wirelessEntry.getTimestamp());
                values.put(WirelessEntry.DB_SCAN_TIME, wirelessEntry.getScanTime());
                values.put(WirelessEntry.DB_SCAN_DIFFERENCE, wirelessEntry.getScanDifference());

                // Insert values into the database
                db.insert(WirelessEntry.DB_TABLE, null, values);
            }
    }

    /**
     * Save multiple cellularEntries to the database.
     * Should be run from the single transaction and not use without it.
     *
     * @param db to save to
     * @param cellularEntries list to save
     * @param fingerprintId to bind single cellularEntry to
     */
    private void saveCellularEntries(SQLiteDatabase db, List<CellularEntry> cellularEntries, long fingerprintId) {
        if(cellularEntries != null && !cellularEntries.isEmpty())
            for(CellularEntry cellularEntry : cellularEntries) {

                // Create content values for SQL
                ContentValues values = new ContentValues();
                values.put(CellularEntry.DB_FINGERPRINT_DB_ID, fingerprintId);
                values.put(CellularEntry.DB_BSIC, cellularEntry.getBsic());
                values.put(CellularEntry.DB_CID, cellularEntry.getCid());
                values.put(CellularEntry.DB_LAC, cellularEntry.getLac());
                values.put(CellularEntry.DB_RSSI, cellularEntry.getRssi());
                values.put(CellularEntry.DB_DISTANCE, cellularEntry.getDistance());
                values.put(CellularEntry.DB_TIMESTAMP, cellularEntry.getTimestamp());
                values.put(CellularEntry.DB_SCAN_TIME, cellularEntry.getScanTime());
                values.put(CellularEntry.DB_SCAN_DIFFERENCE, cellularEntry.getScanDifference());

                // Insert values into the database
                db.insert(CellularEntry.DB_TABLE, null, values);
            }
    }

    /**
     * Save multiple sensorEntries to the database.
     * Should be run from the single transaction and not use without it.
     *
     * @param db to save to
     * @param sensorEntries list to save
     * @param fingerprintId to bind single sensorEntry to
     */
    private void saveSensorEntries(SQLiteDatabase db, List<SensorEntry> sensorEntries, long fingerprintId) {
        if(sensorEntries != null && !sensorEntries.isEmpty())
            for(SensorEntry sensorEntry : sensorEntries) {

                // Create content values for SQL
                ContentValues values = new ContentValues();
                values.put(SensorEntry.DB_FINGERPRINT_DB_ID, fingerprintId);
                values.put(SensorEntry.DB_TYPE, sensorEntry.getType());
                values.put(SensorEntry.DB_TYPE_STRING, sensorEntry.getTypeString());
                values.put(SensorEntry.DB_X, sensorEntry.getX());
                values.put(SensorEntry.DB_Y, sensorEntry.getY());
                values.put(SensorEntry.DB_Z, sensorEntry.getZ());
                values.put(SensorEntry.DB_TIMESTAMP, sensorEntry.getTimestamp());
                values.put(SensorEntry.DB_SCAN_TIME, sensorEntry.getScanTime());
                values.put(SensorEntry.DB_SCAN_DIFFERENCE, sensorEntry.getScanDifference());

                // Insert values into the database
                db.insert(SensorEntry.DB_TABLE, null, values);
            }
    }

    public List<Fingerprint> getAllFingerprints(boolean loadChildren) {
        List<Fingerprint> fingerprints = new ArrayList<>();

        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] fingerprintColumns = {Fingerprint.DB_ID, Fingerprint.DB_FINGERPRINT_ID, Fingerprint.DB_FINGERPRINT_SCAN_ID, Fingerprint.DB_X,
                Fingerprint.DB_Y, Fingerprint.DB_SCAN_LENGTH, Fingerprint.DB_SCAN_START, Fingerprint.DB_SCAN_END, Fingerprint.DB_LEVEL,
                Fingerprint.DB_LOCATION_ID, Fingerprint.DB_DEVICE_ID };

        Cursor cursor = db.query(Fingerprint.DB_TABLE, fingerprintColumns, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Fingerprint fingerprint = new Fingerprint();
                fingerprint.setDbId(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_ID)));
                fingerprint.setId(UUID.fromString( cursor.getString(cursor.getColumnIndex(Fingerprint.DB_FINGERPRINT_ID))));
                fingerprint.setScanID(UUID.fromString( cursor.getString(cursor.getColumnIndex(Fingerprint.DB_FINGERPRINT_SCAN_ID))));
                fingerprint.setX(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_X)));
                fingerprint.setY(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_Y)));
                fingerprint.setScanLength(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_LENGTH)));
                fingerprint.setScanStart(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_START)));
                fingerprint.setScanEnd(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_END)));
                fingerprint.setLevel(cursor.getString(cursor.getColumnIndex(Fingerprint.DB_LEVEL)));
                fingerprint.setLocation_id(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_LOCATION_ID)));
                fingerprint.setDevice_id(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_DEVICE_ID)));

                fingerprints.add(fingerprint);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Loads all the sub-entries only if we want it to
        if(loadChildren) {
            for (Fingerprint fingerprint : fingerprints) {
                fingerprint.setDeviceEntry(getDeviceById(db, fingerprint.getDevice_id()));
                fingerprint.setLocationEntry(getLocationById(db, fingerprint.getLocation_id()));
                fingerprint.setBeaconEntries(getBeaconsByFingerprintId(db, fingerprint.getDbId()));
                fingerprint.setWirelessEntries(getWirelessByFingerprintId(db, fingerprint.getDbId()));
                fingerprint.setCellularEntries(getCellularByFingerprintId(db, fingerprint.getDbId()));
                fingerprint.setSensorEntries(getSensorByFingerprintId(db, fingerprint.getDbId()));
            }
        }

        db.close();

        return fingerprints;
    }

    public DeviceEntry getDeviceById(SQLiteDatabase db, long deviceId) {
        String[] columns = {DeviceEntry.DB_ID, DeviceEntry.DB_TYPE, DeviceEntry.DB_DEVICE_ID, DeviceEntry.DB_DEVICE_NAME,
                DeviceEntry.DB_MODEL, DeviceEntry.DB_BRAND, DeviceEntry.DB_MANUFACTURER, DeviceEntry.DB_DISPLAY,
                DeviceEntry.DB_HARDWARE, DeviceEntry.DB_SERUAL_NUMBER, DeviceEntry.DB_DEVIDE_FINGERPRINT, DeviceEntry.DB_OS, DeviceEntry.DB_API };
        String selection = DeviceEntry.DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( deviceId )};
        String limit = "1";

        DeviceEntry deviceEntry = new DeviceEntry();
        Cursor cursor = db.query(DeviceEntry.DB_TABLE, columns, selection, selectionArgs, null, null, limit);
        if (cursor.moveToFirst()) {
            deviceEntry.setDbId(cursor.getInt(cursor.getColumnIndex(DeviceEntry.DB_ID)));
            deviceEntry.setType(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_TYPE)));
            deviceEntry.setDeviceId(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_DEVICE_ID)));
            deviceEntry.setDeviceName(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_DEVICE_NAME)));
            deviceEntry.setModel(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_MODEL)));
            deviceEntry.setBrand(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_BRAND)));
            deviceEntry.setManufacturer(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_MANUFACTURER)));
            deviceEntry.setDisplay(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_DISPLAY)));
            deviceEntry.setHardware(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_HARDWARE)));
            deviceEntry.setSerialNumber(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_SERUAL_NUMBER)));
            deviceEntry.setDeviceFingerprint(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_DEVIDE_FINGERPRINT)));
            deviceEntry.setOs(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_OS)));
            deviceEntry.setApi(cursor.getInt(cursor.getColumnIndex(DeviceEntry.DB_API)));
        }
        cursor.close();

        return deviceEntry;
    }

    public LocationEntry getLocationById(SQLiteDatabase db, long locationId) {
        String[] columns = {LocationEntry.DB_ID, LocationEntry.DB_BUILDING, LocationEntry.DB_FLOOR};
        String selection = LocationEntry.DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( locationId )};
        String limit = "1";

        LocationEntry locationEntry = new LocationEntry();
        Cursor cursor = db.query(LocationEntry.DB_TABLE, columns, selection, selectionArgs, null, null, limit);
        if (cursor.moveToFirst()) {
            locationEntry.setId(cursor.getInt(cursor.getColumnIndex(LocationEntry.DB_ID)));
            locationEntry.setBuilding(cursor.getString(cursor.getColumnIndex(LocationEntry.DB_BUILDING)));
            locationEntry.setFloor(cursor.getInt(cursor.getColumnIndex(LocationEntry.DB_FLOOR)));
        }
        cursor.close();

        return locationEntry;
    }

    public List<BeaconEntry> getBeaconsByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        String[] columns = {BeaconEntry.DB_ID, BeaconEntry.DB_FINGERPRINT_DB_ID, BeaconEntry.DB_BSSID, BeaconEntry.DB_DISTANCE,
                BeaconEntry.DB_RSSI, BeaconEntry.DB_TIMESTAMP, BeaconEntry.DB_SCAN_TIME, BeaconEntry.DB_SCAN_DIFFERENCE};
        String selection = BeaconEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        List<BeaconEntry> beacons = new ArrayList<>();
        Cursor cursor = db.query(BeaconEntry.DB_TABLE, columns, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                BeaconEntry beaconEntry = new BeaconEntry();

                beaconEntry.setId(cursor.getLong(cursor.getColumnIndex(BeaconEntry.DB_ID)));
                beaconEntry.setFingerprintId(cursor.getInt(cursor.getColumnIndex(BeaconEntry.DB_FINGERPRINT_DB_ID)));
                beaconEntry.setBssid(cursor.getString(cursor.getColumnIndex(BeaconEntry.DB_BSSID)));
                beaconEntry.setDistance(cursor.getFloat(cursor.getColumnIndex(BeaconEntry.DB_DISTANCE)));
                beaconEntry.setRssi(cursor.getInt(cursor.getColumnIndex(BeaconEntry.DB_RSSI)));
                beaconEntry.setTimestamp(cursor.getLong(cursor.getColumnIndex(BeaconEntry.DB_TIMESTAMP)));
                beaconEntry.setScanTime(cursor.getLong(cursor.getColumnIndex(BeaconEntry.DB_SCAN_TIME)));
                beaconEntry.setScanDifference(cursor.getLong(cursor.getColumnIndex(BeaconEntry.DB_SCAN_DIFFERENCE)));

                beacons.add(beaconEntry);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return beacons;
    }

    public List<WirelessEntry> getWirelessByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        String[] columns = {WirelessEntry.DB_ID, WirelessEntry.DB_FINGERPRINT_DB_ID, WirelessEntry.DB_SSID, WirelessEntry.DB_BSSID, WirelessEntry.DB_DISTANCE,
                WirelessEntry.DB_RSSI, WirelessEntry.DB_FREQUENCY, WirelessEntry.DB_CHANNEL, WirelessEntry.DB_TIMESTAMP,
                WirelessEntry.DB_SCAN_TIME, WirelessEntry.DB_SCAN_DIFFERENCE};
        String selection = WirelessEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        List<WirelessEntry> wireless = new ArrayList<>();
        Cursor cursor = db.query(WirelessEntry.DB_TABLE, columns, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                WirelessEntry wirelessEntry = new WirelessEntry();

                wirelessEntry.setId(cursor.getInt(cursor.getColumnIndex(WirelessEntry.DB_ID)));
                wirelessEntry.setFingerprintId(cursor.getInt(cursor.getColumnIndex(WirelessEntry.DB_FINGERPRINT_DB_ID)));
                wirelessEntry.setSsid(cursor.getString(cursor.getColumnIndex(WirelessEntry.DB_SSID)));
                wirelessEntry.setBssid(cursor.getString(cursor.getColumnIndex(WirelessEntry.DB_BSSID)));
                wirelessEntry.setDistance(cursor.getFloat(cursor.getColumnIndex(WirelessEntry.DB_DISTANCE)));
                wirelessEntry.setRssi(cursor.getInt(cursor.getColumnIndex(WirelessEntry.DB_RSSI)));
                wirelessEntry.setFrequency(cursor.getInt(cursor.getColumnIndex(WirelessEntry.DB_FREQUENCY)));
                wirelessEntry.setChannel(cursor.getInt(cursor.getColumnIndex(WirelessEntry.DB_CHANNEL)));
                wirelessEntry.setTimestamp(cursor.getLong(cursor.getColumnIndex(WirelessEntry.DB_TIMESTAMP)));
                wirelessEntry.setScanTime(cursor.getLong(cursor.getColumnIndex(WirelessEntry.DB_SCAN_TIME)));
                wirelessEntry.setScanDifference(cursor.getLong(cursor.getColumnIndex(WirelessEntry.DB_SCAN_DIFFERENCE)));

                wireless.add(wirelessEntry);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return wireless;
    }

    public List<CellularEntry> getCellularByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        String[] columns = {CellularEntry.DB_ID, CellularEntry.DB_FINGERPRINT_DB_ID, CellularEntry.DB_BSIC, CellularEntry.DB_CID, CellularEntry.DB_LAC,
                CellularEntry.DB_RSSI, CellularEntry.DB_DISTANCE, CellularEntry.DB_TIMESTAMP, CellularEntry.DB_SCAN_TIME, CellularEntry.DB_SCAN_DIFFERENCE};
        String selection = CellularEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        List<CellularEntry> cellular = new ArrayList<>();
        Cursor cursor = db.query(CellularEntry.DB_TABLE, columns, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                CellularEntry cellularEntry = new CellularEntry();

                cellularEntry.setId(cursor.getInt(cursor.getColumnIndex(CellularEntry.DB_ID)));
                cellularEntry.setFingerprintId(cursor.getInt(cursor.getColumnIndex(CellularEntry.DB_FINGERPRINT_DB_ID)));
                cellularEntry.setBsic(cursor.getInt(cursor.getColumnIndex(CellularEntry.DB_BSIC)));
                cellularEntry.setCid(cursor.getInt(cursor.getColumnIndex(CellularEntry.DB_CID)));
                cellularEntry.setLac(cursor.getInt(cursor.getColumnIndex(CellularEntry.DB_LAC)));
                cellularEntry.setRssi(cursor.getInt(cursor.getColumnIndex(CellularEntry.DB_RSSI)));
                cellularEntry.setDistance(cursor.getFloat(cursor.getColumnIndex(CellularEntry.DB_DISTANCE)));
                cellularEntry.setTimestamp(cursor.getLong(cursor.getColumnIndex(CellularEntry.DB_TIMESTAMP)));
                cellularEntry.setScanTime(cursor.getLong(cursor.getColumnIndex(CellularEntry.DB_SCAN_TIME)));
                cellularEntry.setScanDifference(cursor.getLong(cursor.getColumnIndex(CellularEntry.DB_SCAN_DIFFERENCE)));

                cellular.add(cellularEntry);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return cellular;
    }

    public List<SensorEntry> getSensorByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        String[] columns = {SensorEntry.DB_ID, SensorEntry.DB_FINGERPRINT_DB_ID, SensorEntry.DB_TYPE, SensorEntry.DB_TYPE_STRING, SensorEntry.DB_X,
                SensorEntry.DB_Y, SensorEntry.DB_Z, SensorEntry.DB_TIMESTAMP, SensorEntry.DB_SCAN_TIME, SensorEntry.DB_SCAN_DIFFERENCE};
        String selection = SensorEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        List<SensorEntry> sensor = new ArrayList<>();
        Cursor cursor = db.query(SensorEntry.DB_TABLE, columns, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                SensorEntry sensorEntry = new SensorEntry();

                sensorEntry.setId(cursor.getInt(cursor.getColumnIndex(SensorEntry.DB_ID)));
                sensorEntry.setFingerprintId(cursor.getInt(cursor.getColumnIndex(SensorEntry.DB_FINGERPRINT_DB_ID)));
                sensorEntry.setType(cursor.getInt(cursor.getColumnIndex(SensorEntry.DB_TYPE)));
                sensorEntry.setTypeString(cursor.getString(cursor.getColumnIndex(SensorEntry.DB_TYPE_STRING)));
                sensorEntry.setX(cursor.getDouble(cursor.getColumnIndex(SensorEntry.DB_X)));
                sensorEntry.setY(cursor.getDouble(cursor.getColumnIndex(SensorEntry.DB_Y)));
                sensorEntry.setZ(cursor.getDouble(cursor.getColumnIndex(SensorEntry.DB_Z)));
                sensorEntry.setTimestamp(cursor.getLong(cursor.getColumnIndex(SensorEntry.DB_TIMESTAMP)));
                sensorEntry.setScanTime(cursor.getLong(cursor.getColumnIndex(SensorEntry.DB_SCAN_TIME)));
                sensorEntry.setScanDifference(cursor.getLong(cursor.getColumnIndex(SensorEntry.DB_SCAN_DIFFERENCE)));

                sensor.add(sensorEntry);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return sensor;
    }

    /**
     * Deletes fingerprint from the database based on its database id.
     *
     * @param fingerprint to delete
     */
    public void deleteFingerprint(Fingerprint fingerprint) {
        String fingerprintId = String.valueOf( fingerprint.getDbId() );
        if(!fingerprintId.isEmpty()) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // Delete Fingerprint data only from specific tables (ignore location and device)
            db.delete(BeaconEntry.DB_TABLE, BeaconEntry.DB_FINGERPRINT_DB_ID + "= ?", new String[]{fingerprintId});
            db.delete(WirelessEntry.DB_TABLE, WirelessEntry.DB_FINGERPRINT_DB_ID + "= ?", new String[]{fingerprintId});
            db.delete(CellularEntry.DB_TABLE, CellularEntry.DB_FINGERPRINT_DB_ID + "= ?", new String[]{fingerprintId});
            db.delete(SensorEntry.DB_TABLE, SensorEntry.DB_FINGERPRINT_DB_ID + "= ?", new String[]{fingerprintId});
            db.delete(Fingerprint.DB_TABLE, Fingerprint.DB_ID + "= ?", new String[]{fingerprintId});
            db.close(); // Closing database connection
        }
    }

    /**
     * Deletes all fingerprints and cleans the whole database.
     */
    public void deleteAllFingerprints() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Delete from data from all the tables
        db.delete(Fingerprint.DB_TABLE, null, null);
        db.delete(LocationEntry.DB_TABLE, null, null);
        db.delete(DeviceEntry.DB_TABLE, null, null);
        db.delete(BeaconEntry.DB_TABLE, null, null);
        db.delete(WirelessEntry.DB_TABLE, null, null);
        db.delete(CellularEntry.DB_TABLE, null, null);
        db.delete(SensorEntry.DB_TABLE, null, null);
        db.close(); // Closing database connection
    }

}
