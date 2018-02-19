package cz.uhk.fim.kikm.wearnavigation.activities.devices;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.gigamole.navigationtabstrip.NavigationTabStrip;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.WearApplication;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;

public class ShowDevicesActivity extends BaseActivity implements BluetoothDevicesFragment.ActivityConnection {

    // Bluetooth check request code
    private final int REQUEST_LOCATION_PERMISSION = 1;
    // ApiConfiguration instance
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

                    // Checks if the permissions were granted
                    if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                // If they were not ask for them again.
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
                    // Bluetooth fragment
                    return BluetoothDevicesFragment.newInstance();
                case 1:
                    // Bluetooth LE fragment
                    return BluetoothLEDevicesFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }
}
