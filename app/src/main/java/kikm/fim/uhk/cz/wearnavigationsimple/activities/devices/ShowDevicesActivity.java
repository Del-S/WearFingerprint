package kikm.fim.uhk.cz.wearnavigationsimple.activities.devices;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.gigamole.navigationtabstrip.NavigationTabStrip;

import kikm.fim.uhk.cz.wearnavigationsimple.BaseActivity;
import kikm.fim.uhk.cz.wearnavigationsimple.R;
import kikm.fim.uhk.cz.wearnavigationsimple.WearApplication;
import kikm.fim.uhk.cz.wearnavigationsimple.model.configuration.Configuration;

public class ShowDevicesActivity extends BaseActivity implements BluetoothDevicesFragment.ActivityConnection {

    // Bluetooth check request code
    private final int REQUEST_LOCATION_PERMISSION = 1;
    // Configuration instance
    private Configuration mConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConfiguration = ((WearApplication) getApplicationContext()).getConfiguration();

        // Bluetooth check
        checkBluetooth();

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.asd_container);
        //mViewPager.setPageTransformer(false, new AnimationsHelper.FadeTransformer(isMyOrders));
        mViewPager.setAdapter(mSectionsPagerAdapter);

        NavigationTabStrip tabLayout = findViewById(R.id.asd_tabs);
        tabLayout.setViewPager(mViewPager);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];

                    if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                            }
                        }
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    protected void updateUI() {
        // Not used
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_show_devices;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_devices;
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    // Buy fragment
                    return BluetoothDevicesFragment.newInstance();
                case 1:
                    // Sell fragment
                    return BluetoothDevicesFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     *\/
    private static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                default:
                    break;
            }
        }
    };*/
}
