package cz.uhk.fim.kikm.wearnavigation.activities.scan;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;

/**
 * Displays map with fingerprints and enables different actions with them.
 * - See information about selected fingerprints (based on location)
 * - Create a new fingerprint
 */
public class ScanActivity extends BaseActivity implements MapClickCallback {

    private FragmentManager mFragmentManager;   // Used to change fragments on the screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initiate fragment manager
        // And on the fist create show FingerprintListFragment and  run Synchronization
        mFragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            showFragment(new MapFragment());
        }
    }

    /**
     * Displays specific fragment on the screen.
     *
     * @param fragment to display
     */
    protected void showFragment(Fragment fragment) {
        if(fragment != null) {
            String fragmentTag = fragment.getClass().getSimpleName();
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.as_frame, fragment, fragmentTag);
            // EmployeeListFragment is not added to backStack to ensure app close on back button click
            if( !fragmentTag.equals(MapFragment.class.getSimpleName()) )
                fragmentTransaction.addToBackStack(fragmentTag);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    @Override
    protected void updateUI() {
        // Update MapFragment
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager()
                .findFragmentByTag( MapFragment.class.getSimpleName() );

        if(mapFragment != null) {
            mapFragment.updateUI();
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_scan;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_scan;
    }

    @Override
    public void onPositionClick(int posX, int posY) {
        // Show Fingerprint list fragment based on which position was clicked
        Fragment employeeFragment = FingerprintListFragment.newInstance(posX, posY);
        showFragment(employeeFragment);
    }

    @Override
    public void onBackPressed() {
        // Pop back fragment if it can be done
        int count = mFragmentManager.getBackStackEntryCount();
        if (count > 0) {
            mFragmentManager.popBackStack();
            return;
        }

        super.onBackPressed();
    }
}
