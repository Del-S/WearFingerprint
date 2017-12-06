package cz.uhk.fim.kikm.wearnavigation.activities.devices;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.Collection;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.adapters.BleBeaconsAdapter;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothLEScanner;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothLEScannerInterface;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDividerItemDecoration;

public class BluetoothLEDevicesFragment extends Fragment implements BluetoothLEScannerInterface {

    // Error and debug tag
    private final String TAG = "BleDevicesFragment";

    // Bluetooth scanner class that handles connection to AltBeacon scanning
    private BluetoothLEScanner mBluetoothLEScanner;
    // TextView informing about discovering devices
    private TextView mScanForDevices;
    // Handler that cancels search
    private Handler cancelSearchHandler = new Handler();

    // Bluetooth LE devices adapter for RecyclerView
    private BleBeaconsAdapter mBleBeaconsAdapter;

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

        // Initiate scanner
        mBluetoothLEScanner = new BluetoothLEScanner(getActivity().getApplicationContext(), this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices_bluetooth_low_energy, container, false);

        // Find view for scanning devices
        mScanForDevices = rootView.findViewById(R.id.fdble_title_search);

        // Divider for row items
        Drawable divider = ContextCompat.getDrawable(getActivity(), R.drawable.row_with_divider);

        // Load adapter for the Bluetooth LE devices
        mBleBeaconsAdapter = new BleBeaconsAdapter(getActivity());

        // Recycler view for bonded devices
        RecyclerView devicesView = rootView.findViewById(R.id.fdble_list);
        devicesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        devicesView.setAdapter(mBleBeaconsAdapter);
        devicesView.addItemDecoration(new SimpleDividerItemDecoration(divider));

        return rootView;
    }


    /**
     * Handles Fragment destroy function to prevent crashes
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Cancel LE scan
        cancelScanning();
        // Send LE scanner the information that Fragment is destroyed
        mBluetoothLEScanner.handleDestroy();
    }

    /**
     * Handles Fragment pause function to prevent crashes
     */
    @Override
    public void onPause() {
        super.onPause();

        // Cancel LE scan
        cancelScanning();
        // Send LE scanner the information that Fragment is paused
        mBluetoothLEScanner.handlePause();
    }

    /**
     * Handles Fragment resume function to bind all functions back
     */
    @Override
    public void onResume() {
        super.onResume();

        // Inform scanner that Fragment was resumed
        mBluetoothLEScanner.handleResume();
    }

    /**
     * Start scanning
     * We can start scanning only when service is connected
     */
    @Override
    public void serviceConnected() {
        // Start beacon scanning
        startScanning();
    }

    /**
     * Save found Beacons into the Adapter for display
     *
     * @param beacon to save and display
     */
    @Override
    public void foundBeacon(final Beacon beacon) {
        FragmentActivity activity = getActivity();
        if(activity != null) {
            // This function called from another Thread so we need to run it in main thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBleBeaconsAdapter.addBeacon(null);
                    mBleBeaconsAdapter.addBeacon(beacon);
                }
            });
        }
    }

    /**
     * Save multiple found Beacons into the Adapter for display
     *
     * @param beacons to save and display
     */
    @Override
    public void foundMultipleBeacons(final Collection<Beacon> beacons) {
        FragmentActivity activity = getActivity();
        if(activity != null) {
            // This function called from another Thread so we need to run it in main thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBleBeaconsAdapter.addAllBeacons(beacons);
                }
            });
        }
    }

    /**
     * Starts classic bluetooth discovery for limited amount of time
     */
    private void startScanning() {
        if( !mBluetoothLEScanner.isScanning() ) {
            // Clear adapter on new search
            mBleBeaconsAdapter.clearBeaconList();

            // Starting bluetooth discovery
            mBluetoothLEScanner.startScan(60000);
        }

        // Change discovering view
        mScanForDevices.setText(R.string.fdb_title_bl_searching);
        mScanForDevices.setTextColor(getResources().getColor( android.R.color.tab_indicator_text ));
        mScanForDevices.setOnClickListener(null);

        // After 15 seconds disable discovery
        cancelSearchHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cancelScanning();
                }
            }, 15000);
    }

    /**
     * Cancel bluetooth discovery and change button to enable next search
     */
    private void cancelScanning() {
        // Call cancel scanning on the scanner
        if( mBluetoothLEScanner.isScanning() ) {
            mBluetoothLEScanner.cancelScan();
        }

        // Disable canceling of current scanning (only used if it was canceled prematurely)
        cancelSearchHandler.removeCallbacksAndMessages(null);

        // Add scan button function and change design
        mScanForDevices.setText(R.string.fdb_title_bl_search);
        mScanForDevices.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
        mScanForDevices.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startScanning();
                }
            });
    }
}
