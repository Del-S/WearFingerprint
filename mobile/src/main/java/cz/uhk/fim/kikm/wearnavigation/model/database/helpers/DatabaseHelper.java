package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cz.uhk.fim.kikm.wearnavigation.model.database.BeaconEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.CellularEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.LocationEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.SensorEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.WirelessEntry;

class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 9;
    private static final String DATABASE_NAME = "fingerprint.db";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // TODO: add foreign keys and indexes
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create fingerprint table
        String CREATE_TABLE_FINGERPRINT = "CREATE TABLE IF NOT EXISTS " + Fingerprint.DB_TABLE + '('
                + Fingerprint.DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Fingerprint.DB_FINGERPRINT_ID + " TEXT, "
                + Fingerprint.DB_FINGERPRINT_SCAN_ID + " TEXT, "
                + Fingerprint.DB_X + " INTEGER, "
                + Fingerprint.DB_Y + " INTEGER, "
                + Fingerprint.DB_SCAN_LENGTH + " INTEGER, "
                + Fingerprint.DB_SCAN_START + " INTEGER, "
                + Fingerprint.DB_SCAN_END + " INTEGER, "
                + Fingerprint.DB_UPDATE_TIME + " INTEGER, "
                + Fingerprint.DB_LEVEL + " TEXT, "
                + Fingerprint.DB_LOCATION_ID + " INTEGER, "
                + Fingerprint.DB_DEVICE_ID + " INTEGER )";
        db.execSQL(CREATE_TABLE_FINGERPRINT);

        // Create locations table
        String CREATE_TABLE_LOCATION = "CREATE TABLE IF NOT EXISTS " + LocationEntry.DB_TABLE + '('
                + LocationEntry.DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + LocationEntry.DB_BUILDING + " TEXT, "
                + LocationEntry.DB_FLOOR + " INTEGER, "
                + LocationEntry.DB_LEVEL + " TEXT )";
        db.execSQL(CREATE_TABLE_LOCATION);

        // Create devices table
        String CREATE_TABLE_DEVICE = "CREATE TABLE IF NOT EXISTS " + DeviceEntry.DB_TABLE + '('
                + DeviceEntry.DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DeviceEntry.DB_TYPE + " TEXT, "
                + DeviceEntry.DB_DEVICE_ID + " TEXT, "
                + DeviceEntry.DB_DEVICE_NAME + " TEXT, "
                + DeviceEntry.DB_MODEL + " TEXT, "
                + DeviceEntry.DB_BRAND + " TEXT, "
                + DeviceEntry.DB_MANUFACTURER + " TEXT, "
                + DeviceEntry.DB_DISPLAY + " TEXT, "
                + DeviceEntry.DB_HARDWARE + " TEXT, "
                + DeviceEntry.DB_SERIAL_NUMBER + " TEXT, "
                + DeviceEntry.DB_TELEPHONE + " TEXT, "
                + DeviceEntry.DB_DEVICE_FINGERPRINT + " TEXT, "
                + DeviceEntry.DB_OS + " TEXT, "
                + DeviceEntry.DB_API + " INTEGER )";
        db.execSQL(CREATE_TABLE_DEVICE);

        // Create beacons table
        String CREATE_TABLE_BEACON = "CREATE TABLE IF NOT EXISTS " + BeaconEntry.DB_TABLE + '('
                + BeaconEntry.DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + BeaconEntry.DB_FINGERPRINT_DB_ID + " INTEGER, "
                + BeaconEntry.DB_BSSID + " TEXT, "
                + BeaconEntry.DB_DISTANCE + " FLOAT, "
                + BeaconEntry.DB_RSSI + " INTEGER, "
                + BeaconEntry.DB_TIMESTAMP + " INTEGER, "
                + BeaconEntry.DB_SCAN_TIME + " INTEGER, "
                + BeaconEntry.DB_SCAN_DIFFERENCE + " INTEGER )";
        db.execSQL(CREATE_TABLE_BEACON);

        // Create wireless table
        String CREATE_TABLE_WIRELESS = "CREATE TABLE IF NOT EXISTS " + WirelessEntry.DB_TABLE + '('
                + WirelessEntry.DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + WirelessEntry.DB_FINGERPRINT_DB_ID + " INTEGER, "
                + WirelessEntry.DB_SSID + " TEXT, "
                + WirelessEntry.DB_BSSID + " TEXT, "
                + WirelessEntry.DB_RSSI + " INTEGER, "
                + WirelessEntry.DB_FREQUENCY + " INTEGER, "
                + WirelessEntry.DB_CHANNEL + " INTEGER, "
                + WirelessEntry.DB_DISTANCE + " FLOAT, "
                + WirelessEntry.DB_TIMESTAMP + " INTEGER, "
                + WirelessEntry.DB_SCAN_TIME + " INTEGER, "
                + WirelessEntry.DB_SCAN_DIFFERENCE + " INTEGER )";
        db.execSQL(CREATE_TABLE_WIRELESS);

        // Create wireless table
        String CREATE_TABLE_CELLULAR = "CREATE TABLE IF NOT EXISTS " + CellularEntry.DB_TABLE + '('
                + CellularEntry.DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + CellularEntry.DB_FINGERPRINT_DB_ID + " INTEGER, "
                + CellularEntry.DB_BSIC + " INTEGER, "
                + CellularEntry.DB_CID + " INTEGER, "
                + CellularEntry.DB_LAC + " INTEGER, "
                + CellularEntry.DB_RSSI + " INTEGER, "
                + CellularEntry.DB_DISTANCE + " FLOAT, "
                + CellularEntry.DB_TIMESTAMP + " INTEGER, "
                + CellularEntry.DB_SCAN_TIME + " INTEGER, "
                + CellularEntry.DB_SCAN_DIFFERENCE + " INTEGER )";
        db.execSQL(CREATE_TABLE_CELLULAR);

        // Create wireless table
        String CREATE_TABLE_SENSOR = "CREATE TABLE IF NOT EXISTS " + SensorEntry.DB_TABLE + '('
                + SensorEntry.DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SensorEntry.DB_FINGERPRINT_DB_ID + " INTEGER, "
                + SensorEntry.DB_TYPE + " INTEGER, "
                + SensorEntry.DB_TYPE_STRING + " TEXT, "
                + SensorEntry.DB_X + " INTEGER, "
                + SensorEntry.DB_Y + " INTEGER, "
                + SensorEntry.DB_Z + " INTEGER, "
                + SensorEntry.DB_TIMESTAMP + " INTEGER, "
                + SensorEntry.DB_SCAN_TIME + " INTEGER, "
                + SensorEntry.DB_SCAN_DIFFERENCE + " INTEGER )";
        db.execSQL(CREATE_TABLE_SENSOR);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Fingerprint.DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LocationEntry.DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DeviceEntry.DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + BeaconEntry.DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + WirelessEntry.DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CellularEntry.DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + SensorEntry.DB_TABLE);
        onCreate(db);
    }
}
