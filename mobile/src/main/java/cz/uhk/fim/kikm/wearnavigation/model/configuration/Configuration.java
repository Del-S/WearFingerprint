package cz.uhk.fim.kikm.wearnavigation.model.configuration;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class Configuration {
    // Preferences identification
    private static SharedPreferences sharedPref;
    // Parses class to json
    private static Gson gson;
    // Preferences name file
    private static final String PREFERENCES_NAME = "WearData";
    // App configuration identification in the file
    private static final String PREFERENCES_KEY = "Configuration";

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
}
