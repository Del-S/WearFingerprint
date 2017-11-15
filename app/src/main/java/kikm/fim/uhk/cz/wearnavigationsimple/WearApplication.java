package kikm.fim.uhk.cz.wearnavigationsimple;

import android.app.Application;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import kikm.fim.uhk.cz.wearnavigationsimple.model.configuration.Configuration;

public class WearApplication extends Application {

    // Configuration for the whole app
    private Configuration configuration;

    // Power saver for BeaconLibrary
    private BackgroundPowerSaver backgroundPowerSaver;

    @Override
    public void onCreate() {
        configuration = Configuration.getConfiguration(this);
        super.onCreate();

        BeaconManager.setDebug(true);

        // Load beacon manager instance to enable settings change
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

        // Enable beacon
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        // This reduces bluetooth power usage by about 60% when application is not visible
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    /**
     * Get instance of current configuration
     *
     * @return Configuration instance
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Set configuration to the application and save it to the SharedPreferences
     *
     * @param configuration to save into the app and sp
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        Configuration.saveConfiguration(configuration);
    }
}
