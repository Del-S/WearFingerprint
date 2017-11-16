package kikm.fim.uhk.cz.wearnavigationsimple.activities.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import kikm.fim.uhk.cz.wearnavigationsimple.R;
import kikm.fim.uhk.cz.wearnavigationsimple.model.adapters.BlDevicesAdapter;
import kikm.fim.uhk.cz.wearnavigationsimple.model.configuration.Configuration;
import kikm.fim.uhk.cz.wearnavigationsimple.utils.SimpleDividerItemDecoration;

public class BluetoothDevicesFragment extends Fragment implements BlDevicesAdapter.BlDevicesInterface {

    // Error and debug tag
    private final String TAG = "BlDevicesFragment";

    // App configuration
    private Configuration mConfiguration;

    // Adapter to get devices from
    private BluetoothAdapter mBluetoothAdapter;
    // TextView informing about discovering devices
    private TextView mDiscoverDevices;
    // Handler that cancels search
    private Handler cancelSearchHandler = new Handler();

    // Adapters to show devices
    private BlDevicesAdapter mBlDeviceAdapterBonded, mBlDeviceAdapter;
    // Global list of bounded devices
    private List<BluetoothDevice> bondedDevices = new ArrayList<>();

    /**
     * Create instance of BluetoothDevicesFragment and pass variable to it to change functions
     *
     * @return BluetoothDevicesFragment instance
     */
    public static BluetoothDevicesFragment newInstance() {
        return new BluetoothDevicesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityConnection mInterface;

        // Check if startup activity implements listener
        try {
            mInterface = (ActivityConnection) getActivity();
        } catch (Exception e) {
            throw new ClassCastException(TAG
                    + " must implement ActivityConnection");
        }

        // Load data from Activity using interface
        mBluetoothAdapter = mInterface.getBluetoothAdapter();
        mConfiguration = mInterface.getConfiguration();

        // Load bluetooth bound devices
        bondedDevices.addAll(mBluetoothAdapter.getBondedDevices());

        registerReceiver();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices_bluetooth, container, false);

        // Find view for discovering devices
        mDiscoverDevices = rootView.findViewById(R.id.fdb_title_bl_search);

        // Divider for row items
        Drawable divider = ContextCompat.getDrawable(getActivity(), R.drawable.row_with_divider);

        // Create adapter for bonded devices
        mBlDeviceAdapterBonded = new BlDevicesAdapter(getActivity(),
                this,
                true,
                bondedDevices);

        // Recycler view for bonded devices
        RecyclerView recyclerViewBonded = rootView.findViewById(R.id.fdb_bluetooth_list_bonded);
        recyclerViewBonded.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerViewBonded.setAdapter(mBlDeviceAdapterBonded);
        recyclerViewBonded.addItemDecoration(new SimpleDividerItemDecoration(divider));

        // Create adapter for new found devices
        mBlDeviceAdapter = new BlDevicesAdapter(getActivity(),
                this,
                false,
                null);

        // Recycler view for newly found devices
        RecyclerView recyclerView = rootView.findViewById(R.id.fdb_bluetooth_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mBlDeviceAdapter);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(divider));

        // Start bluetooth discovery
        startDiscovery();

        return rootView;
    }

    /**
     * Handles Fragment pause function to prevent crashes
     */
    @Override
    public void onPause() {
        super.onPause();

        // Unregister receiver
        unRegisterReceiver();
        // Cancel discovery of devices
        cancelDiscovery();
    }

    /**
     * Handles Fragment resume function to bind all functions back
     */
    @Override
    public void onResume() {
        super.onResume();

        // Re-register receiver
        registerReceiver();
        // Start discovery on resume
        startDiscovery();
    }

    /**
     * Register receiver for binding and discovery
     */
    private void registerReceiver() {
        // Register for broadcasts for device discovery and binding
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, filter);
    }

    /**
     * Unregister receiver
     */
    private void unRegisterReceiver() {
        // Check if fragment is still connected to the activity
        FragmentActivity activity = getActivity();
        if(activity != null) {
            // If the receiver is not registered then un-registering crashes the app
            try {
                activity.unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Cannot unregister receiver. Receiver not registered.");
            }
        }
    }

    /**
     * Starts classic bluetooth discovery for limited amount of time
     */
    private void startDiscovery() {
        if( !mBluetoothAdapter.isDiscovering() ) {
            // Starting bluetooth discovery
            mBluetoothAdapter.startDiscovery();
        }

        // Change discovering view
        mDiscoverDevices.setText(R.string.fdb_title_bl_searching);
        mDiscoverDevices.setTextColor(getResources().getColor( android.R.color.tab_indicator_text ));
        mDiscoverDevices.setOnClickListener(null);

        // After 15 seconds disable discovery
        cancelSearchHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cancelDiscovery();
            }
        }, 15000);
    }

    /**
     * Cancel bluetooth discovery and change button to enable next search
     */
    private void cancelDiscovery() {
        // Call cancel scanning on the scanner
        if( mBluetoothAdapter.isDiscovering() ) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Disable canceling of current scanning (only used if it was canceled prematurely)
        cancelSearchHandler.removeCallbacksAndMessages(null);

        // Add scan button function and change design
        mDiscoverDevices.setText(R.string.fdb_title_bl_search);
        mDiscoverDevices.setTextColor(getResources().getColor( R.color.colorTextPrimaryDark ));
        mDiscoverDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovery();
            }
        });
    }

    /**
     * Pair and connect to the specific selected device
     *
     * @param device to connect to
     */
    @Override
    public void connectDevice(BluetoothDevice device) {
        cancelDiscovery();

        // Check if the device is bonded (paired) already
        boolean bonded = false;
        for(BluetoothDevice bondedDevice : bondedDevices) {
            if( bondedDevice.getAddress().equals(device.getAddress()) ) {
                bonded = true;
                break;
            }
        }

        if(!bonded) {
            // Try to create bond with a specific device
            if(!device.createBond()) {
                // Show error when binding and reset the device view
                Toast.makeText(getActivity(), R.string.fdb_cannot_pair, Toast.LENGTH_SHORT).show();
                mBlDeviceAdapter.resetDevice(device);
            }
        }
        /*/ Create communication between both devices
        BluetoothConnectionService mConnectionService = new BluetoothConnectionService(this,
                mHandler,
                mConfiguration.getServiceName(),
                mConfiguration.getAppUUID());
        mConnectionService.connect(device, true);
        String str = "sdvsvsv";
        mConnectionService.write(str.getBytes()); */
    }

    /**
     * Cancel current bonding request to enable bond with another device
     *
     * @param device to cancel bonding
     */
    @Override
    public void cancelBonding(BluetoothDevice device) {
        if(device.getBondState() == BluetoothDevice.BOND_BONDING) {
            try {
                // CancelBondProcess function is not available so we call it like this.
                Method m = device.getClass()
                        .getMethod("cancelBondProcess", (Class[]) null);
                m.invoke(device, (Object[]) null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * BroadcastReceiver to inform this Fragment when Bluetooth device is found or
     * bond state has changed.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    // Save bound device and move it to bounded list
                    mBlDeviceAdapter.removeDevice(device);
                    mBlDeviceAdapterBonded.addDevice(device);
                } else if(device.getBondState() == BluetoothDevice.BOND_NONE) {
                    // Bond was canceled so we need to reset device and start discovery again
                    mBlDeviceAdapter.resetDevice(device);
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // New device found + adding it to the list
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mBlDeviceAdapter.addDevice(device);
                }
            }
        }
    };

    /**
     * Saves device to the application configuration
     *
     * @param device bound to save to configuration
     */
    private void saveDeviceToConfiguration(BluetoothDevice device) {
        mConfiguration.setBondedDeviceMac(device.getAddress());
        Configuration.saveConfiguration(mConfiguration);
    }

    /**
     * Interface to handle connection between Fragment and Activity
     */
    public interface ActivityConnection {
        /**
         * Get bluetooth adapter from context Activity to enable scanning.
         *
         * @return BluetoothAdapter
         */
        BluetoothAdapter getBluetoothAdapter();

        /**
         * Returns an instance of app wide Configuration class.
         * It is used to keep track of the single Bluetooth connected device.
         * To this device BLE search results will be posted to save.
         *
         * @return Configuration instance
         */
        Configuration getConfiguration();
    }
}
