package cz.uhk.fim.kikm.wearnavigation.activities.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.adapters.BleBeaconsAdapter;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BLEScannerManager;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDividerItemDecoration;

public class BluetoothLEDevicesFragment extends Fragment {

    private BLEScannerManager mBLEScannerManager;           // Bluetooth scanner class that handles connection to AltBeacon scanning
    private BeaconScannerReceiver mReceiver;                // Receiver that parses new beacons and more
    private BleBeaconsAdapter mBLEBeaconsAdapter;           // Bluetooth LE devices adapter for RecyclerView
    private TextView mScanForDevices;                       // TextView informing about discovering devices
    private int mScanTime = 30000;                          // Scan time

    /**
     * Create instance of BluetoothLEDevicesFragment and pass variable to it to change functions
     *
     * @return BluetoothLEDevicesFragment instance
     */
    public static BluetoothLEDevicesFragment newInstance() {
        return new BluetoothLEDevicesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get instance of scanner manager
        FragmentActivity activity = getActivity();
        if(activity != null) {
            mBLEScannerManager = BLEScannerManager.getInstance(activity.getApplicationContext());
        }

        mReceiver = new BeaconScannerReceiver();    // Create new receiver that parses beacons
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices_bluetooth_low_energy, container, false);

        // Find view for scanning devices
        mScanForDevices = rootView.findViewById(R.id.fdble_title_search);

        // Divider for row items
        Drawable divider = null;
        FragmentActivity activity = getActivity();
        if(activity != null) {
            divider = ContextCompat.getDrawable(activity, R.drawable.row_with_divider);
        }

        // Load adapter for the Bluetooth LE devices
        mBLEBeaconsAdapter = new BleBeaconsAdapter(getActivity());

        // Recycler view for bonded devices
        RecyclerView devicesView = rootView.findViewById(R.id.fdble_list);
        devicesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        devicesView.setAdapter(mBLEBeaconsAdapter);
        if(divider != null) {
            devicesView.addItemDecoration(new SimpleDividerItemDecoration(divider));
        }

        changeScanningDesign();     // Change scan widgets design

        return rootView;
    }


    /**
     * Handles Fragment destroy function to prevent crashes
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // We can cancel this only if priority scan is not running
        if(!mBLEScannerManager.isScanning() || !mBLEScannerManager.isPriorityScan()) {
            // Cancel LE scan
            cancelScanning();
            // Send LE scanner the information that Fragment is destroyed
            mBLEScannerManager.handleDestroy();
        }
    }

    /**
     * Handles Fragment pause function to prevent crashes
     */
    @Override
    public void onPause() {
        super.onPause();

        // We can cancel this only if priority scan is not running
        if(!mBLEScannerManager.isScanning() || !mBLEScannerManager.isPriorityScan()) {
            cancelScanning();                   // Cancel LE scan
            mBLEScannerManager.handlePause();   // Send LE scanner the information that Fragment is paused
        }

        // Unregister receiver
        FragmentActivity activity = getActivity();
        if(activity != null) {
            activity.unregisterReceiver(mReceiver);
        }
    }

    /**
     * Handles Fragment resume function to bind all functions back
     */
    @Override
    public void onResume() {
        super.onResume();

        mBLEScannerManager.handleResume();  // Inform scanner that Fragment was resumed
        registerReceiver();     // Register receiver again
        startScanning();        // Tries to start a new scan
    }

    /**
     * Starts classic bluetooth discovery for limited amount of time
     */
    private void startScanning() {
        if( mBLEScannerManager.isBound() && !mBLEScannerManager.isScanning() ) {
            // Clear adapter on new search
            mBLEBeaconsAdapter.clearBeaconList();

            // Starting bluetooth discovery
            mBLEScannerManager.startScan(mScanTime, false);
        }

        changeScanningDesign();     // Change scan widgets design
    }

    /**
     * Cancel bluetooth discovery and change button to enable next search
     */
    private void cancelScanning() {
        // Call cancel scanning on the scanner
        if( mBLEScannerManager.isScanning() ) {
            mBLEScannerManager.cancelScan();
        }

        changeScanningDesign();     // Change scan widgets design
    }

    /**
     * Changes a view texts when scanning and back
     */
    private void changeScanningDesign() {
        if(mBLEScannerManager.isScanning()) {
            // Change discovering view
            mScanForDevices.setText(R.string.fdb_title_bl_searching);       // Change text to "Searching"
            mScanForDevices.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));  // Change text color
            mScanForDevices.setOnClickListener(null);                       // Disable new scan on click
        } else {
            // Add scan button function and change design
            mScanForDevices.setText(R.string.fdb_title_bl_search);          // Change text to "Search"
            mScanForDevices.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));    // Change text color
            mScanForDevices.setOnClickListener(v -> startScanning());       // Enables to start new scan on click
        }
    }

    /**
     * Register receiver that parses beacons to the activity.
     */
    private void registerReceiver() {
        FragmentActivity activity = getActivity();
        if(activity != null) {
            // Register receiver
            IntentFilter filter = new IntentFilter(BLEScannerManager.ACTION_BEACONS_FOUND);     // Create intent filter with single action
            filter.addAction(BLEScannerManager.ACTION_SCAN_STATE_CHANGE);                       // Add more actions to the intent filter
            filter.addAction(BLEScannerManager.ACTION_SCANNER_BOUND);                           // Add more actions to the intent filter
            activity.registerReceiver(mReceiver, filter);    // Register receiver to the context to listen for beacon data
        }
    }

    /**
     * Receiver that handles Beacon scanner.
     * Sets Beacons to the adapter. Changes display state and starts scanning.
     */
    class BeaconScannerReceiver extends BroadcastReceiver {

        /**
         * Receives Broadcast and handles base on action type.
         * - Adds new beacons to the adapter for display.
         * - Starts new scan when scanner is bound.
         * - Changes display of scan views.
         *
         * @param context context that send Broadcast
         * @param intent of the broadcast
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null) {
                switch (action) {
                    case BLEScannerManager.ACTION_BEACONS_FOUND:
                        if(intent.getExtras() != null) {
                            // Adds beacon to the adapter
                            ArrayList<Beacon> beacons = intent.getExtras().getParcelableArrayList(BLEScannerManager.ACTION_BEACONS_DATA);
                            mBLEBeaconsAdapter.addAllBeacons(beacons);
                        }
                        break;
                    case BLEScannerManager.ACTION_SCANNER_BOUND:
                        startScanning();            // Start scanner
                        break;
                    case BLEScannerManager.ACTION_SCAN_STATE_CHANGE:
                        changeScanningDesign();     // Change display base on scanner state
                        break;
                }
            }
        }
    }
}
