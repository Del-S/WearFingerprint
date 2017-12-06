package cz.uhk.fim.kikm.wearnavigation;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import java.lang.reflect.Field;

import cz.uhk.fim.kikm.wearnavigation.activities.bluetoothTest.BluetoothTest;
import cz.uhk.fim.kikm.wearnavigation.activities.devices.ShowDevicesActivity;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDialogHelper;

public abstract class BaseActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    // Global variable for Bottom navigation
    protected BottomNavigationView navigationView;
    // Bluetooth check request code
    private final int REQUEST_ENABLE_BT = 1000;
    // App wide configuration class
    protected Configuration mConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title from the app
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Sets view based on child activity
        setContentView(getContentViewId());

        // Load bottom navigation
        navigationView = findViewById(R.id.bottom_menu);
        disableShiftMode(navigationView);
        navigationView.setOnNavigationItemSelectedListener(this);

        // Notify that menu has changed
        invalidateOptionsMenu();

        // Load configuration from the application
        mConfiguration = ((WearApplication) getApplicationContext()).getConfiguration();

        // Bluetooth check
        checkBluetooth();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Update bottom menu buttons to which one is active
        updateNavigationBarState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disables transitions between activities (for bottom menu)
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        // Shows alert dialog for the whole app.
        // Because with bottom menu implementation you can click back button and kill the app instantly.
        SimpleDialogHelper.dialogLeavingApp(this, null).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        // Starting activities in bottom menu
        navigationView.postDelayed(new Runnable() {
            @Override
            public void run() {
                int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.action_show_main:
                        // Shows main activity
                        showActivity(MainActivity.class);
                        break;
                    case R.id.action_show_devices:
                        // Shows devices activity
                        showActivity(ShowDevicesActivity.class);
                        break;
                    case R.id.action_show_bluetooth_test:
                        // Show bl test activity
                        showActivity(BluetoothTest.class);
                        break;
                    default:
                        // Shows main activity
                        showActivity(MainActivity.class);
                        break;
                }
            }
        }, 100);
        return true;
    }

    /**
     * Starts activity if its not current running activity
     *
     * @param a class of the activity to show
     */
    public void showActivity(Class a) {
        if (!(this.getClass() == a)) {
            Intent i = new Intent(this, a);

            // Pass bundle if there is some (used for Notifications)
            Bundle bundle = getIntent().getExtras();

            // Put Bundle into Intent
            if ((bundle != null) && (!bundle.isEmpty())) {
                i.putExtras(bundle);
            }

            // Start new activity and finish current
            startActivity(i);
            finish();
        }
    }

    /**
     * Checks for action in bottom navigation and tries to highlight current tab
     */
    private void updateNavigationBarState(){
        int actionId = getNavigationMenuItemId();
        if(actionId >= 0) {
            selectBottomNavigationBarItem(actionId);
        }
    }

    /**
     * Disable bottom navigation bar shift mode.
     * https://stackoverflow.com/questions/40176244/how-to-disable-bottomnavigationview-shift-mode
     *
     * @param view Bottom navigation to disable shifting
     */
    public static void disableShiftMode(BottomNavigationView view) {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                //noinspection RestrictedApi
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                //noinspection RestrictedApi
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e) {
            Log.e("BNVHelper", "Unable to get shift mode field", e);
        } catch (IllegalAccessException e) {
            Log.e("BNVHelper", "Unable to change value of shift mode", e);
        }
    }

    /**
     * Highlights current tab in bottom menu
     *
     * @param itemId which item should be highlighted by the ID
     */
    void selectBottomNavigationBarItem(int itemId) {
        if(navigationView != null) {
            Menu menu = navigationView.getMenu();
            for (int i = 0, size = menu.size(); i < size; i++) {
                MenuItem item = menu.getItem(i);
                boolean shouldBeChecked = item.getItemId() == itemId;
                if (shouldBeChecked) {
                    item.setChecked(true);
                    break;
                }
            }
        }
    }


    /**
     * Checking if the device has bluetooth and if it is enabled.
     */
    protected void checkBluetooth() {
        // Loading bluetooth adapter to figure out if the device has bluetooth
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.am_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Check if the Bluetooth is enabled
        if(bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                checkBle();
            }
        }
    }

    /**
     * Checking to determine whether BLE is supported on the device.
     * - If it is not then display message and quit
     */
    protected void checkBle() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.am_bluetooth_le_support, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode != RESULT_OK) {
                    Toast.makeText(this, R.string.am_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    checkBle();
                }
                break;
        }
    }

    /**
     * Update UI function that updates the screen
     * - All child activities have to have this so we can update screen from Menu
     * - Update screen is not the same as new Intent !! You stay on that screen and Update it.
     */
    protected abstract void updateUI();

    /**
     * Gets layout of current activity
     *
     * @return int id layout
     */
    protected abstract int getContentViewId();

    /**
     * Gets position in bottom navigation menu
     *
     * @return int id of item in the menu
     */
    protected abstract int getNavigationMenuItemId();

}
