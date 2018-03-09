package cz.uhk.fim.kikm.wearnavigation.activities.scan;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;

/**
 * Displays map with fingerprints and enables different actions with them.
 * - See information about selected fingerprints (based on location)
 * - Create a new fingerprint
 */
public class ScanActivity extends BaseActivity {

    private FragmentManager mFragmentManager;   // Used to change fragments on the screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initiate fragment manager
        // And on the fist create show ListFragment and  run Synchronization
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
                fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    @Override
    protected void updateUI() {
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_scan;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_scan;
    }
}
