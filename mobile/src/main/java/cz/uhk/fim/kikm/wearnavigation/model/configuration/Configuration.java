package cz.uhk.fim.kikm.wearnavigation.model.configuration;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.gson.Gson;

import cz.uhk.fim.kikm.wearnavigation.model.api.FingerprintMeta;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;

public class Configuration {

    private static SharedPreferences sharedPref;    // Preferences identification
    private static Gson gson;                       // Parses class to json
    private static final String PREFERENCES_NAME = "WearData";          // Preferences name file
    private static final String PREFERENCES_KEY = "ApiConfiguration";   // App configuration identification in the file

    public static final String API_URL = "http://beacon.uhk.cz/fingerprint-api/";    // URL of fingerprint api

    @JsonIgnore
    private DeviceEntry mDevice;             // This device object

    private FingerprintMeta mMeta;           // Fingerprint metadata
    private int mSynchronizationInterval;    // Api data synchronization interval
    private long mLastSynchronizationTime;   // Last time api data were synchronized
    private long mLastDownloadTime;          // Last download time

    // TODO: get rid of this
    private BluetoothDevice bondedDevice;

    private Configuration() {
    }

    public synchronized static Configuration getConfiguration(Context context) {
        // Get user data from SharedPreferences (by gson)
        sharedPref = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        String dataConfiguration = sharedPref.getString(PREFERENCES_KEY, "");

        // Check if User exists in user preferences
        Configuration config = gson.fromJson(dataConfiguration, Configuration.class);
        if(config == null) {
            config = new Configuration();
            config.setDevice(context);
        }
        return config;
    }

    public synchronized static void saveConfiguration(Configuration config) {
        // Load SharedPreferences editor
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        String dataConfiguration = gson.toJson(config);

        // Saves data into shared pref
        sharedPrefEditor.putString(PREFERENCES_KEY, dataConfiguration);
        sharedPrefEditor.apply();
    }

    public BluetoothDevice getBondedDevice() {
        return bondedDevice;
    }

    public void setBondedDevice(BluetoothDevice bondedDevice) {
        this.bondedDevice = bondedDevice;
    }

    private void setDevice(Context context) {
        mDevice = DeviceEntry.createInstance(context);
    }

    public DeviceEntry getDevice(Context context) {
        if(mDevice != null) {
            setDevice(context);
        }
        return mDevice;
    }

    public FingerprintMeta getMeta() {
        return mMeta;
    }

    public void setMeta(FingerprintMeta mMeta) {
        this.mMeta = mMeta;
    }

    public int getSynchronizationInterval() {
        return mSynchronizationInterval;
    }

    public void setSynchronizationInterval(int mSynchronizationInterval) {
        this.mSynchronizationInterval = mSynchronizationInterval;
    }

    public long getLastSynchronizationTime() {
        return mLastSynchronizationTime;
    }

    public void setLastSynchronizationTime(long mLastSynchronizationTime) {
        this.mLastSynchronizationTime = mLastSynchronizationTime;
    }

    public long getLastDownloadTime() {
        return mLastDownloadTime;
    }

    public void setLastDownloadTime(long mLastDownloadTime) {
        this.mLastDownloadTime = mLastDownloadTime;
    }
}
