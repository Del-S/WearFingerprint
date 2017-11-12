package kikm.fim.uhk.cz.wearnavigationsimple.model.configuration;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.UUID;

public class Configuration {
    // Preferences identification
    private static SharedPreferences sharedPref;
    private static Gson gson;
    private static final String PREFERENCES_NAME = "Configuration";
    private static final String PREFERENCES_KEY = "UserData";

    private UUID appUUID;
    private final String serviceName = "WearBluetoothService";
    private String bondedDeviceMac;

    private Configuration() {
        // Create app UUID if it does not exist just yet
        if(appUUID == null) {
            appUUID = UUID.randomUUID();
            saveConfiguration(this);
        }
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
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        String dataConfiguration = gson.toJson(config);

        // Saves data into shared pref
        sharedPrefEditor.putString(PREFERENCES_KEY, dataConfiguration);
        sharedPrefEditor.apply();
    }

    public UUID getAppUUID() {
        return appUUID;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getBondedDeviceMac() {
        return bondedDeviceMac;
    }

    public void setBondedDeviceMac(String bondedDeviceMac) {
        this.bondedDeviceMac = bondedDeviceMac;
    }
}
