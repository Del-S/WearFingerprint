package cz.uhk.fim.kikm.wearnavigation.activities.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
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

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection.BluetoothConnectionHandler;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection.BluetoothConnectionInterface;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection.BluetoothConnectionService;
import cz.uhk.fim.kikm.wearnavigation.model.adapters.BlDevicesAdapter;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDividerItemDecoration;

public class BluetoothDevicesFragment extends Fragment implements BlDevicesAdapter.BlDevicesInterface, BluetoothConnectionInterface {

    private static final String TAG = "BlDevicesFragment";  // Error and debug tag

    private ActivityConnection mInterface;                  // Interface to communicate with context activity
    private Configuration mConfiguration;                   // App configuration
    private final Handler mHandler = new BluetoothConnectionHandler(this);  // Handler for Bluetooth connection service using this as interface
    private BluetoothConnectionService mService = null;     // Bonded bluetooth connection service

    private BluetoothAdapter mBluetoothAdapter;             // Adapter to get devices from
    private TextView mDiscoverDevices;                      // TextView informing about discovering devices
    private Handler cancelSearchHandler = new Handler();    // Handler that cancels search

    private BlDevicesAdapter mBlDeviceAdapterBonded, mBlDeviceAdapter;      // Adapters to show devices
    private List<BluetoothDevice> bondedDevices = new ArrayList<>();        // Global list of bounded devices

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

        // Check if startup activity implements listener
        try {
            mInterface = (ActivityConnection) getActivity();
        } catch (Exception e) {
            throw new ClassCastException(getActivity().getClass()
                    + " must implement ActivityConnection");
        }

        // Load bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Load bluetooth bound devices
        bondedDevices.addAll(mBluetoothAdapter.getBondedDevices());

        registerReceiver();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices_bluetooth, container, false);

        // Load configuration
        mConfiguration = mInterface.getConfiguration();

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

        return rootView;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Load service instance
            mService = ((BluetoothConnectionService.LocalBinder)iBinder).getInstance();
            // Set handler to communicate with service
            mService.setHandler(mHandler);

            // Try to connect to the device
            BluetoothDevice connectedDevice = mConfiguration.getBondedDevice();
            if(connectedDevice != null && !connectedDevice.getAddress().isEmpty()) {
                // Try to connect to the device
                mService.connect(connectedDevice);

                // Set connecting display for this device
                mBlDeviceAdapter.markActiveDevice(connectedDevice);
                mBlDeviceAdapterBonded.markActiveDevice(connectedDevice);

                // Getting display name or address for the device
                String displayName = connectedDevice.getName();
                if(displayName == null || displayName.isEmpty()) {
                    displayName = connectedDevice.getAddress();
                }

                // Display connecting message to inform user
                FragmentActivity activity = getActivity();
                if(activity != null) {
                    Toast.makeText(activity,
                            String.format(getResources().getString(R.string.fdb_notice_connecting), displayName),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                mService.start();
            }

            // Start bluetooth discovery
            startDiscovery();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };

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
        // Unbind bluetooth service
        FragmentActivity activity = getActivity();
        if(activity != null) {
            activity.unbindService(mConnection);
        }
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

        // Bind bluetooth service
        FragmentActivity activity = getActivity();
        if(activity != null) {
            // Bind to BluetoothConnectionService
            Intent intent = new Intent(activity, BluetoothConnectionService.class);
            activity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
        cancelSearchHandler.postDelayed(this::cancelDiscovery, 15000);
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
        mDiscoverDevices.setOnClickListener(v -> {
            mBlDeviceAdapter.clearDeviceList();
            mBlDeviceAdapterBonded.clearFoundDeviceList();
            startDiscovery();
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
                FragmentActivity activity = getActivity();
                if(activity != null) {
                    Toast.makeText(activity, R.string.fdb_cannot_pair, Toast.LENGTH_SHORT).show();
                }
                mBlDeviceAdapter.resetDevice(device);
            }
        } else {
            // Create communication between both devices
            mService.connect(device);
        }
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
     * Sets device as connected to change the display.
     *
     * @param device that was connected
     */
    @Override
    public void deviceConnected(BluetoothDevice device) {
        // Tell adapters to inform which device is connected
        mBlDeviceAdapter.markConnectedDevice(device);
        mBlDeviceAdapterBonded.markConnectedDevice(device);

        // Save connected device to the configuration
        mConfiguration.setBondedDevice(device);
        Configuration.saveConfiguration(mConfiguration);
    }

    /**
     * Connection to the device failed. Change layout to enable new connection.
     *
     * @param device that was not connected
     */
    @Override
    public void connectionFailed(BluetoothDevice device) {
        // Getting display name or address for the device
        String displayName = device.getName();
        if(displayName == null || displayName.isEmpty()) {
            displayName = device.getAddress();
        }

        // Display connection error message to inform user
        FragmentActivity activity = getActivity();
        if(activity != null) {
            Toast.makeText(activity,
                    String.format(getResources().getString(R.string.fdb_notice_connection_failed), displayName),
                    Toast.LENGTH_SHORT).show();
        }

        // Reset device view to enable connection
        mBlDeviceAdapterBonded.resetDevice(device);
        mBlDeviceAdapter.resetDevice(device);
    }

    @Override
    public void connectionFailed() {

    }

    /**
     * Handles received message from the other device.
     * Not used in here.
     *
     * @param message that was received
     */
    @Override
    public void messageReceived(String message) {
        // Not used
        Log.d(TAG, "Message is: " + message);
    }

    /**
     * Handles send message from current device.
     * Used to confirm that message was sent.
     *
     * @param message that was send
     */
    @Override
    public void messageSend(String message) {
        // Not used
        Log.d(TAG, "Message is: " + message);
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
                    mBlDeviceAdapterBonded.addDevice(device, true);

                    // After the device is bonded we try to connect to it
                    mService.connect(device);
                } else if(device.getBondState() == BluetoothDevice.BOND_NONE) {
                    // Bond was canceled so we need to reset device and start discovery again
                    mBlDeviceAdapter.resetDevice(device);
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // New device found + adding it to the list
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mBlDeviceAdapter.addDevice(device, false);
                } else if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mBlDeviceAdapterBonded.markFoundDevice(device);
                }
            }
        }
    };

    /**
     * Interface to handle connection between Fragment and Activity
     */
    public interface ActivityConnection {

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
