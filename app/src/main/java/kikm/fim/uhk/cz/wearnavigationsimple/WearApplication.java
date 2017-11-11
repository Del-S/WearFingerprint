package kikm.fim.uhk.cz.wearnavigationsimple;

import android.app.Application;

import kikm.fim.uhk.cz.wearnavigationsimple.model.configuration.Configuration;

public class WearApplication extends Application {

    private Configuration configuration;

    @Override
    public void onCreate() {
        configuration = Configuration.getConfiguration(this);
        super.onCreate();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        Configuration.saveConfiguration(configuration);
    }
}
