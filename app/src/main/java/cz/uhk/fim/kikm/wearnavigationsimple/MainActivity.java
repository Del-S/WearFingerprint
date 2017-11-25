package cz.uhk.fim.kikm.wearnavigationsimple;

import android.os.Bundle;

import cz.uhk.fim.kikm.wearnavigationsimple.activities.devices.ShowDevicesActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bluetooth check
        checkBluetooth();

        // Test
        showActivity(ShowDevicesActivity.class);
    }

    @Override
    protected void updateUI() {
        // Not used
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_main;
    }
}
