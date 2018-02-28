package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
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
    private Context context;

    public DatabaseCRUD(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Saves multiple fingerprints from the list.
     * Has two options to save:
     * - Save all fingerprint in a single transaction.
     * - Save each fingerprint in single transaction.
     * If saving in a single transaction fails then it moves to
     * single transactions.
     *
     * @param fingerprints List to save
     * @return int number of saved fingerprints
     */
    public int saveMultipleFingerprints(List<Fingerprint> fingerprints) {
        int savedCount = 0;

        if(fingerprints != null && !fingerprints.isEmpty()) {
            SQLiteDatabase db = dbHelper.getWritableDatabase(); // Load sql database via helper

            savedCount = saveSingleTransaction(fingerprints, db);
            if( savedCount <= 0 ) {
                savedCount = saveMultipleTransaction(fingerprints, db);
            }

            db.close();
        }

        return savedCount;
    }

    /**
     * Saves all fingerprint in single transaction.
     * Thanks to single transaction it will be faster.
     * If one of the saves fails this transaction is not successful.
     *
     * @param fingerprints to save into database
     * @param db to save data into
     * @return count of saved fingerprints
     */
    private int saveSingleTransaction(List<Fingerprint> fingerprints, SQLiteDatabase db) {
        int savedCount = 0;
        boolean transactionSuccess = true;

        db.beginTransaction();  // Starts transaction

        // Tries to save multiple fingerprints
        for (Fingerprint fingerprint : fingerprints) {
            if(!saveFingerprint(fingerprint, db, false)) {
                transactionSuccess = false;
                break;
            }
            savedCount++;
        }

        // Mark transaction as successful or failed
        if(transactionSuccess)
            db.setTransactionSuccessful();
        else
            savedCount = -1;

        db.endTransaction();    // End transaction

        return savedCount;
    }

    /**
     * This option will save fingerprints with multiple transactions.
     * Each fingerprint is one transaction.
     *
     * @param fingerprints to save into the database
     * @param db database to save
     * @return int count saved
     */
    private int saveMultipleTransaction(List<Fingerprint> fingerprints, SQLiteDatabase db) {
        int savedCount = 0;

        // Runs save with multiple transactions
        for (Fingerprint fingerprint : fingerprints) {
            if(saveFingerprint(fingerprint, db, true))
                savedCount++;
        }

        return savedCount;
    }

    /**
     * Saves fingerprint all containing data like devices and location.
     * This is main function for saving.
     *
     * @param fingerprint to save into the database
     * @param db database to save into. Can be null.
     * @param useTransaction if this code should use transactions
     * @return boolean if fingerprint was saved
     */
    public boolean saveFingerprint(Fingerprint fingerprint, SQLiteDatabase db, boolean useTransaction) {
        boolean saved = false;
        boolean close = false;
        if(db == null) {
            db = dbHelper.getWritableDatabase(); // Load sql database via helper
            close = true;                        // We should also close connection at the end of this function
        }

        if(db != null && fingerprint != null) {
            try {
                if(useTransaction) {
                    db.beginTransaction();  // Start transaction
                }
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
                        Log.i(TAG,"Fingerprint saved successfully.");
                        if(useTransaction) {
                            db.setTransactionSuccessful();
                        }
                        saved = true;
                    } else {
                        Log.e(TAG, "Error in saveFingerprint().");
                    }
                } else {
                    Log.e(TAG, "Fingerprint already exists in saveFingerprint().");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in saveFingerprint().", e);
            } finally {
                // Close up connection transaction if it should
                if(useTransaction) {
                    db.endTransaction();
                }
                if(close) {
                    db.close();
                }
            }
        }

        return saved;
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
        values.put(Fingerprint.DB_X, fingerprint.getX());
        values.put(Fingerprint.DB_Y, fingerprint.getY());
        values.put(Fingerprint.DB_SCAN_LENGTH, fingerprint.getScanLength());
        values.put(Fingerprint.DB_SCAN_START, fingerprint.getScanStart());
        values.put(Fingerprint.DB_SCAN_END, fingerprint.getScanEnd());
        values.put(Fingerprint.DB_UPDATE_TIME, fingerprint.getUpdateTime());
        values.put(Fingerprint.DB_LEVEL, fingerprint.getLevel());
        values.put(Fingerprint.DB_LOCATION_ID, locationId);
        values.put(Fingerprint.DB_DEVICE_ID, deviceId);

        // Set scan ID (only with new fingerprints)
        UUID scanID = fingerprint.getScanID();
        if(scanID != null) {
            values.put(Fingerprint.DB_FINGERPRINT_SCAN_ID, scanID.toString());
        }
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
        // Check if location exists
        long result = doesLocationExist(db, location);

        // If the location does not exist we create it
        if(result == -1) {
            // Create content values for SQL
            ContentValues values = new ContentValues();
            values.put(LocationEntry.DB_BUILDING, location.getBuilding());
            values.put(LocationEntry.DB_FLOOR, location.getFloor());
            values.put(LocationEntry.DB_LEVEL, location.getLevel());

            // Insert values into the database
            result = db.insert(LocationEntry.DB_TABLE, null, values);
        }

        return result;
    }

    /**
     * Checks if location already exists in the database
     *
     * @param db to check in
     * @param location to search in the database
     * @return long with location id or -1
     */
    private long doesLocationExist(SQLiteDatabase db, LocationEntry location) {
        // Default value for non-existent location
        long result = -1;

        // SQL columns to select
        String[] columns = { LocationEntry.DB_ID };
        // SQL WHERE clause for location data
        String selection = LocationEntry.DB_BUILDING + " = ?"
                + " AND " + LocationEntry.DB_FLOOR + " = ?"
                + " AND " + LocationEntry.DB_LEVEL + " = ?";
        // WHERE clause parameters
        String[] selectionArgs = new String[] {location.getBuilding(),
                String.valueOf(location.getFloor()),
                location.getLevel()};
        String limit = "1";  // We can only select one row

        // Try to load location from database
        Cursor cursor = db.query(LocationEntry.DB_TABLE, columns, selection, selectionArgs, null, null, null, limit);
        if(cursor.getCount() > 0 && cursor.moveToNext()) {
            // Try to load location id
            result = cursor.getLong(cursor.getColumnIndex(LocationEntry.DB_ID));
        }
        cursor.close();   // Close the cursor because we don't need it anymore

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
        // Check if device exists
        long result = doesDeviceExist(db, device);

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
            values.put(DeviceEntry.DB_SERIAL_NUMBER, device.getSerialNumber());
            values.put(DeviceEntry.DB_TELEPHONE, device.getTelephone());
            values.put(DeviceEntry.DB_DEVICE_FINGERPRINT, device.getDeviceFingerprint());
            values.put(DeviceEntry.DB_OS, device.getOs());
            values.put(DeviceEntry.DB_API, device.getApi());

            // Insert values into the database
            result = db.insert(DeviceEntry.DB_TABLE, null, values);
        }

        return result;
    }

    /**
     * Checks if device already exists in the database.
     * This cannot check based on telephone because that is
     * the same for mobile and wear device.
     *
     * @param db to check in
     * @param device to find in the database
     * @return long with device id or -1
     */
    private long doesDeviceExist(SQLiteDatabase db, DeviceEntry device) {
        // Default value for non-existent device
        long result = -1;

        // SQL columns to select
        String[] columns = { DeviceEntry.DB_ID };
        // SQL WHERE clause for location data
        String selection = DeviceEntry.DB_SERIAL_NUMBER + " = ?"
                + " AND " + DeviceEntry.DB_DEVICE_FINGERPRINT + " = ?"
                + " AND " + DeviceEntry.DB_OS + " = ?"
                + " AND " + DeviceEntry.DB_API + " = ?";
        // WHERE clause parameters
        String[] selectionArgs = new String[] {device.getSerialNumber(),
                device.getDeviceFingerprint(),
                device.getOs(),
                String.valueOf( device.getApi() )};
        String limit = "1";  // We can only select one row

        // Try to load device from database
        Cursor cursor = db.query(DeviceEntry.DB_TABLE, columns, selection, selectionArgs, null, null, null, limit);
        if(cursor.getCount() > 0 && cursor.moveToNext()) {
            // Try to load device id
            result = cursor.getLong(cursor.getColumnIndex(DeviceEntry.DB_ID));
        }
        cursor.close();   // Close the cursor because we don't need it anymore

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

    /**
     * Loads all fingerprints from the database
     *
     * @param loadChildren load beacons, wireless, cellular and sensor data
     * @return List of Fingerprints
     */
    public List<Fingerprint> getAllFingerprints(boolean loadChildren) {
        List<Fingerprint> fingerprints = new ArrayList<>();

        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Select specific columns
        String[] fingerprintColumns = {Fingerprint.DB_ID, Fingerprint.DB_FINGERPRINT_ID, Fingerprint.DB_FINGERPRINT_SCAN_ID, Fingerprint.DB_X,
                Fingerprint.DB_Y, Fingerprint.DB_SCAN_LENGTH, Fingerprint.DB_SCAN_START, Fingerprint.DB_SCAN_END, Fingerprint.DB_UPDATE_TIME,
                Fingerprint.DB_LEVEL, Fingerprint.DB_LOCATION_ID, Fingerprint.DB_DEVICE_ID };

        // Initiate cursor and query data
        Cursor cursor = db.query(Fingerprint.DB_TABLE, fingerprintColumns, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Fingerprint fingerprint = new Fingerprint(context);
                fingerprint.setDbId(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_ID)));
                fingerprint.setId(UUID.fromString( cursor.getString(cursor.getColumnIndex(Fingerprint.DB_FINGERPRINT_ID))));
                fingerprint.setX(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_X)));
                fingerprint.setY(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_Y)));
                fingerprint.setScanLength(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_LENGTH)));
                fingerprint.setScanStart(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_START)));
                fingerprint.setScanEnd(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_END)));
                fingerprint.setUpdateTime(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_UPDATE_TIME)));
                fingerprint.setLevel(cursor.getString(cursor.getColumnIndex(Fingerprint.DB_LEVEL)));
                fingerprint.setLocationDbId(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_LOCATION_ID)));
                fingerprint.setDeviceDbId(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_DEVICE_ID)));

                // Set scan id (only for new fingerprints)
                String scanID = cursor.getString(cursor.getColumnIndex(Fingerprint.DB_FINGERPRINT_SCAN_ID));
                if(scanID != null) {
                    fingerprint.setScanID(UUID.fromString( scanID ));
                }

                fingerprints.add(fingerprint);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Location and device is always loaded
        for (Fingerprint fingerprint : fingerprints) {
            fingerprint.setDeviceEntry(getDeviceById(db, fingerprint.getDeviceDbId()));
            fingerprint.setLocationEntry(getLocationById(db, fingerprint.getLocationDbId()));
        }

        // Loads all the sub-entries only if we want it to
        if(loadChildren) {
            for (Fingerprint fingerprint : fingerprints) {
                fingerprint.setBeaconEntries(getBeaconsByFingerprintId(db, fingerprint.getDbId()));
                fingerprint.setWirelessEntries(getWirelessByFingerprintId(db, fingerprint.getDbId()));
                fingerprint.setCellularEntries(getCellularByFingerprintId(db, fingerprint.getDbId()));
                fingerprint.setSensorEntries(getSensorByFingerprintId(db, fingerprint.getDbId()));
            }
        }

        db.close();

        return fingerprints;
    }

    /**
     * Loads specific device by id
     *
     * @param db database to load data from
     * @param deviceId load device with this id
     * @return DeviceEntry of specific device
     */
    public DeviceEntry getDeviceById(SQLiteDatabase db, long deviceId) {
        boolean close = false;
        if(db == null) {
            db = dbHelper.getReadableDatabase();
            close = true;
        }

        // Selection columns
        String[] columns = {DeviceEntry.DB_ID, DeviceEntry.DB_TYPE, DeviceEntry.DB_DEVICE_ID, DeviceEntry.DB_DEVICE_NAME,
                DeviceEntry.DB_MODEL, DeviceEntry.DB_BRAND, DeviceEntry.DB_MANUFACTURER, DeviceEntry.DB_DISPLAY,
                DeviceEntry.DB_HARDWARE, DeviceEntry.DB_SERIAL_NUMBER, DeviceEntry.DB_TELEPHONE,
                DeviceEntry.DB_DEVICE_FINGERPRINT, DeviceEntry.DB_OS, DeviceEntry.DB_API };
        // Where clause with parameters
        String selection = DeviceEntry.DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( deviceId )};
        // Limit to only one row
        String limit = "1";

        // Initiate and run database query
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
            deviceEntry.setSerialNumber(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_SERIAL_NUMBER)));
            deviceEntry.setTelephone(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_TELEPHONE)));
            deviceEntry.setDeviceFingerprint(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_DEVICE_FINGERPRINT)));
            deviceEntry.setOs(cursor.getString(cursor.getColumnIndex(DeviceEntry.DB_OS)));
            deviceEntry.setApi(cursor.getInt(cursor.getColumnIndex(DeviceEntry.DB_API)));
        }
        cursor.close();

        if(close) {
            db.close();
        }

        return deviceEntry;
    }

    /**
     * Get location by id from database
     *
     * @param db to load data from
     * @param locationId find this location id
     * @return LocationEntry of specific location
     */
    private LocationEntry getLocationById(SQLiteDatabase db, long locationId) {
        // Selection columns
        String[] columns = {LocationEntry.DB_ID, LocationEntry.DB_BUILDING, LocationEntry.DB_FLOOR,
                            LocationEntry.DB_LEVEL};
        // Where clause with parameters
        String selection = LocationEntry.DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( locationId )};
        // Limit to only one row
        String limit = "1";

        // Initiate and run database query
        LocationEntry locationEntry = new LocationEntry();
        Cursor cursor = db.query(LocationEntry.DB_TABLE, columns, selection, selectionArgs, null, null, limit);
        if (cursor.moveToFirst()) {
            locationEntry.setId(cursor.getInt(cursor.getColumnIndex(LocationEntry.DB_ID)));
            locationEntry.setBuilding(cursor.getString(cursor.getColumnIndex(LocationEntry.DB_BUILDING)));
            locationEntry.setFloor(cursor.getInt(cursor.getColumnIndex(LocationEntry.DB_FLOOR)));
            locationEntry.setLevel(cursor.getString(cursor.getColumnIndex(LocationEntry.DB_LEVEL)));
        }
        cursor.close();

        return locationEntry;
    }

    /**
     * Load all BeaconEntries by fingerprint id
     *
     * @param db to load data from
     * @param fingerprintId to find in the database
     * @return List of BeaconEntries
     */
    private List<BeaconEntry> getBeaconsByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        // Selection columns
        String[] columns = {BeaconEntry.DB_ID, BeaconEntry.DB_FINGERPRINT_DB_ID, BeaconEntry.DB_BSSID, BeaconEntry.DB_DISTANCE,
                BeaconEntry.DB_RSSI, BeaconEntry.DB_TIMESTAMP, BeaconEntry.DB_SCAN_TIME, BeaconEntry.DB_SCAN_DIFFERENCE};
        // Where clause with parameters
        String selection = BeaconEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        // Initiate and run database query
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

    /**
     * Load all WirelessEntries for specific Fingerprint
     *
     * @param db to load data from
     * @param fingerprintId to find in the database
     * @return List of WirelessEntries
     */
    private List<WirelessEntry> getWirelessByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        // Selection columns
        String[] columns = {WirelessEntry.DB_ID, WirelessEntry.DB_FINGERPRINT_DB_ID, WirelessEntry.DB_SSID, WirelessEntry.DB_BSSID, WirelessEntry.DB_DISTANCE,
                WirelessEntry.DB_RSSI, WirelessEntry.DB_FREQUENCY, WirelessEntry.DB_CHANNEL, WirelessEntry.DB_TIMESTAMP,
                WirelessEntry.DB_SCAN_TIME, WirelessEntry.DB_SCAN_DIFFERENCE};
        // Where clause with parameters
        String selection = WirelessEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        // Initiate and run database query
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

    /**
     * Get CellularEntries for specific Fingerprint
     *
     * @param db to load data from
     * @param fingerprintId to find in the database
     * @return List of CellularEntries
     */
    private List<CellularEntry> getCellularByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        // Selection columns
        String[] columns = {CellularEntry.DB_ID, CellularEntry.DB_FINGERPRINT_DB_ID, CellularEntry.DB_BSIC, CellularEntry.DB_CID, CellularEntry.DB_LAC,
                CellularEntry.DB_RSSI, CellularEntry.DB_DISTANCE, CellularEntry.DB_TIMESTAMP, CellularEntry.DB_SCAN_TIME, CellularEntry.DB_SCAN_DIFFERENCE};
        // Where clause with parameters
        String selection = CellularEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        // Initiate and run database query
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

    /**
     * Get SensorEntries for specific Fingerprint
     *
     * @param db to load data from
     * @param fingerprintId to find in the database
     * @return List of SensorEntries
     */
    private List<SensorEntry> getSensorByFingerprintId(SQLiteDatabase db, int fingerprintId) {
        // Selection columns
        String[] columns = {SensorEntry.DB_ID, SensorEntry.DB_FINGERPRINT_DB_ID, SensorEntry.DB_TYPE, SensorEntry.DB_TYPE_STRING, SensorEntry.DB_X,
                SensorEntry.DB_Y, SensorEntry.DB_Z, SensorEntry.DB_TIMESTAMP, SensorEntry.DB_SCAN_TIME, SensorEntry.DB_SCAN_DIFFERENCE};
        // Where clause with parameters
        String selection = SensorEntry.DB_FINGERPRINT_DB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf( fingerprintId )};

        // Initiate and run database query
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
     * Load fingerprints to upload from this device or paired wear device only to
     * the beacon server.
     *
     * @param scanStart limit fingerprint by start timestamp
     * @param deviceId find fingerprint only from this device
     * @param limit for the data to not overflow the server
     * @param offset move the within the specified limit
     * @return List of Fingerprints
     */
    public List<Fingerprint> getFingerprintsForUpload(Long scanStart, String deviceId, int limit, long offset) {
        List<Fingerprint> fingerprints = new ArrayList<>();

        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Select specific columns
        String rawQuery = "SELECT f.* FROM " + Fingerprint.DB_TABLE + " f INNER JOIN " + DeviceEntry.DB_TABLE + " d"
                + " ON f." + Fingerprint.DB_DEVICE_ID + " = d." + DeviceEntry.DB_ID
                + " WHERE f." + Fingerprint.DB_SCAN_START + " > ? AND d." + DeviceEntry.DB_TELEPHONE + " = ?"
                + " LIMIT ? OFFSET ?";
        String[] parameters = { String.valueOf(scanStart),
                deviceId,
                String.valueOf(limit),
                String.valueOf(offset)
        };

        // Initiate cursor and query data
        Cursor cursor = db.rawQuery(rawQuery, parameters);
        if (cursor.moveToFirst()) {
            do {
                Fingerprint fingerprint = new Fingerprint(context);
                fingerprint.setDbId(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_ID)));
                fingerprint.setId(UUID.fromString( cursor.getString(cursor.getColumnIndex(Fingerprint.DB_FINGERPRINT_ID))));
                fingerprint.setX(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_X)));
                fingerprint.setY(cursor.getInt(cursor.getColumnIndex(Fingerprint.DB_Y)));
                fingerprint.setScanLength(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_LENGTH)));
                fingerprint.setScanStart(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_START)));
                fingerprint.setScanEnd(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_SCAN_END)));
                fingerprint.setUpdateTime(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_UPDATE_TIME)));
                fingerprint.setLevel(cursor.getString(cursor.getColumnIndex(Fingerprint.DB_LEVEL)));
                fingerprint.setLocationDbId(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_LOCATION_ID)));
                fingerprint.setDeviceDbId(cursor.getLong(cursor.getColumnIndex(Fingerprint.DB_DEVICE_ID)));

                // Set scan id (only for new fingerprints)
                String scanID = cursor.getString(cursor.getColumnIndex(Fingerprint.DB_FINGERPRINT_SCAN_ID));
                if(scanID != null) {
                    fingerprint.setScanID(UUID.fromString( scanID ));
                }

                fingerprints.add(fingerprint);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Loads all the sub-entries
        for (Fingerprint fingerprint : fingerprints) {
            fingerprint.setDeviceEntry(getDeviceById(db, fingerprint.getDeviceDbId()));
            fingerprint.setLocationEntry(getLocationById(db, fingerprint.getLocationDbId()));
            fingerprint.setBeaconEntries(getBeaconsByFingerprintId(db, fingerprint.getDbId()));
            fingerprint.setWirelessEntries(getWirelessByFingerprintId(db, fingerprint.getDbId()));
            fingerprint.setCellularEntries(getCellularByFingerprintId(db, fingerprint.getDbId()));
            fingerprint.setSensorEntries(getSensorByFingerprintId(db, fingerprint.getDbId()));
        }

        db.close();

        return fingerprints;
    }

    /**
     * Get MAX value of update time (timestamp) of all fingerprints.
     * Upload time is last time fingerprint was updated on the server.
     * Based on this variable we can sort and filter fingerprints.
     *
     * @return Long max update timestamp
     */
    public Long getMaxUpdateTime() {
        SQLiteDatabase db = dbHelper.getReadableDatabase(); // Init database connection

        // Load MAX timestamp
        final SQLiteStatement timestampStatement = db
                .compileStatement("SELECT MAX("+Fingerprint.DB_UPDATE_TIME+") FROM "+Fingerprint.DB_TABLE);
        Long result = timestampStatement.simpleQueryForLong();

        // Just to be sure we close the database connection
        if(db.isOpen()) {
            db.close();
        }

        return result;
    }

    /**
     * Get count of not uploaded fingerprints.
     *
     * @param scanStart max timestamp on the server
     * @param deviceId fingerprints only this device has found
     * @return Long count of not uploaded fingerprints
     */
    public Long getUploadCount(Long scanStart, String deviceId) {
        // Build query parameters
        String rawQuery = "SELECT COUNT(*) FROM " + Fingerprint.DB_TABLE + " f INNER JOIN " + DeviceEntry.DB_TABLE + " d"
                + " ON f." + Fingerprint.DB_DEVICE_ID + " = d." + DeviceEntry.DB_ID
                + " WHERE f." + Fingerprint.DB_SCAN_START + " > ? AND d." + DeviceEntry.DB_TELEPHONE + " = ?";
        String[] parameters = { String.valueOf(scanStart), deviceId };

        // Run query to figure out upload count
        SQLiteDatabase db = dbHelper.getReadableDatabase();                     // Init database connection
        final SQLiteStatement countStatement = db.compileStatement(rawQuery);   // Init SQL statement
        countStatement.bindAllArgsAsStrings(parameters);                        // Bind statement parameters
        Long uploadCount = countStatement.simpleQueryForLong();                 // Run statement
        db.close();                                                             // Close database connection

        return uploadCount;
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
