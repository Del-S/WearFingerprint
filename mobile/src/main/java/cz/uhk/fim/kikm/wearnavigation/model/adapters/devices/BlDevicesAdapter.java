package cz.uhk.fim.kikm.wearnavigation.model.adapters.devices;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.R;

import static android.support.v7.widget.RecyclerView.*;

public class BlDevicesAdapter extends RecyclerView.Adapter {

    // Layout variables
    private LayoutInflater mInflater;
    // List of devices
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    // List of found devices that will be enabled to connect (used for bonded list)
    private List<BluetoothDevice> mDevicesFound = new ArrayList<>();
    // Interface to communicate with fragment
    private BlDevicesInterface mInterface;
    // Information if this list shows bonded devices
    private boolean mBonded = false;
    // Position of item that was last activated
    private BluetoothDevice activeDevice;
    // Connected device
    private BluetoothDevice connectedDevice;
    // Position to reset view
    private int positionToReset = -1;

    public BlDevicesAdapter(Context context, BlDevicesInterface mInterface, boolean bonded, List<BluetoothDevice> devices) {
        // Set interface to communicate through
        this.mInterface = mInterface;
        // Check if this adapter handles Bonded devices or not
        mBonded = bonded;
        // Load inflater to create layout
        mInflater = LayoutInflater.from(context);
        if(devices != null) {
            // Add device to the list
            mDevices.addAll(devices);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Creating single item view
        View view = mInflater.inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // Get device and view to input data into
        final BluetoothDevice device = mDevices.get(position);
        final DeviceViewHolder deviceViewHolder = (DeviceViewHolder) holder;

        // Display pairing/connecting status information
        if(device.equals(activeDevice)) {
            displayStatus(device, deviceViewHolder);
        }

        // Change layout for connected button
        if(device.equals(connectedDevice)) {
            displayConnected(deviceViewHolder);
        }

        // Reset design and active device on current position
        if(positionToReset == position) {
            resetDeviceDisplay(device, deviceViewHolder);
        }

        // Set name or mac address to display
        String name = device.getName();
        if(name == null || name.equals("") || name.isEmpty()) {
            name = device.getAddress();
        }
        // Insert name into view holder
        deviceViewHolder.name.setText(name);

        // Set connection button action
        deviceViewHolder.action.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cancel bonding request when other device is clicked
                if( (activeDevice != null) && (activeDevice.getBondState() != BluetoothDevice.BOND_BONDED) ) {
                    mInterface.cancelBonding(activeDevice);
                    resetDevice(device);
                }

                // Reset connected device view
                if(connectedDevice != null) {
                    resetDevice(connectedDevice);
                }

                // Displays status information for this view holder
                displayStatus(device, deviceViewHolder);

                // Call connect to device
                mInterface.connectDevice(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    /**
     * Displays status information text so user sees what is happening (pairing/connecting).
     * Sets device as active.
     *
     * @param device to set as active
     * @param deviceViewHolder to display status in
     */
    private void displayStatus(BluetoothDevice device, DeviceViewHolder deviceViewHolder) {
        // Set device position that is active
        activeDevice = device;

        // Disable button to prevent multiple clicks
        deviceViewHolder.action.setEnabled(false);
        // Make status information visible
        deviceViewHolder.status.setVisibility(VISIBLE);

        // Change status test based on bond state
        if (mBonded) {
            deviceViewHolder.status.setText(R.string.id_action_connecting);
            activeDevice = null;
        } else {
            deviceViewHolder.status.setText(R.string.id_action_pairing);
        }
    }

    /**
     * Change layout to mark device as connected.
     *
     * @param deviceViewHolder to change layout
     */
    private void displayConnected(DeviceViewHolder deviceViewHolder) {
        // Disable button to prevent multiple clicks
        deviceViewHolder.action.setEnabled(false);
        // Change test
        deviceViewHolder.action.setText(R.string.id_action_connected);
        // Make status information visible
        deviceViewHolder.status.setVisibility(GONE);
    }

    /**
     * Resets device display to default (active) and resets active/connected
     * devices.
     *
     * @param device to reset
     * @param deviceViewHolder to reset
     */
    private void resetDeviceDisplay(BluetoothDevice device, DeviceViewHolder deviceViewHolder) {
        // To prevent crashes
        if(device == null) return;

        // Disable button to prevent multiple clicks
        deviceViewHolder.action.setEnabled(true);
        // Reset action test
        deviceViewHolder.action.setText(R.string.id_action_connect);
        // Make status information visible
        deviceViewHolder.status.setVisibility(GONE);
        // Position was reset so we can reset identification
        positionToReset = -1;
        // Also reset active device if this device was reset
        if(device.equals(activeDevice)) {
            activeDevice = null;
        }
        // Also reset connected device if this device was reset
        if(device.equals(connectedDevice)) {
            connectedDevice = null;
        }
    }

    /**
     * Add device to the list if it does not exist already
     *
     * @param device to add to the list
     * @param active is this device view active
     */
    public void addDevice(BluetoothDevice device, boolean active) {
        // Don't add null device
        if(device == null) return;

        // Add or replace device to/in the list
        if(!mDevices.contains(device)) {
            mDevices.add(device);
        } else {
            replaceDevice(device);
        }

        // If this device is active mark it as such
        if(active) {
            activeDevice = device;
        }

        // Inform adapter that list has changed
        notifyDataSetChanged();
    }

    /**
     * Add device to the list if it does not exist already
     *
     * @param device to add to the list
     */
    public void removeDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {

            // If removed device was active we reset the view
            if(device.equals(activeDevice) || device.equals(connectedDevice)) {
                positionToReset = mDevices.indexOf(device);
            }

            // Deletes device from the list
            mDevices.remove(device);
            // Inform adapter that list has changed
            notifyDataSetChanged();
        }
    }

    /**
     * Reset specific device view
     *
     * @param device to reset view
     */
    public void resetDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {
            // Get reset position of the device
            positionToReset = mDevices.indexOf(device);
            // Inform adapter that list has changed
            notifyItemChanged(positionToReset);
        }
    }

    /**
     * Clears the list of all devices.
     */
    public void clearDeviceList() {
        // Sets all device states to null
        activeDevice = null;
        connectedDevice = null;
        mDevicesFound.clear();

        // Clears all the devices
        mDevices.clear();
        // Inform adapter that list has changed
        notifyDataSetChanged();
    }

    /**
     * Clears list of found devices.
     */
    public void clearFoundDeviceList() {
        mDevicesFound.clear();
        notifyDataSetChanged();
    }

    /**
     * Replace device on specific location
     *
     * @param device to replace
     */
    private void replaceDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {
            // Get device position and replace it
            int position = mDevices.indexOf(device);
            mDevices.set(position, device);
            notifyItemChanged(position);
        }
    }

    /**
     * Marks device as connected for display (to change layout).
     *
     * @param device to mark as connected
     */
    public void markConnectedDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {
            // Marks device as connected
            connectedDevice = device;
            notifyDataSetChanged();
        }
    }

    /**
     * Marks device as active to show status message.
     *
     * @param device to mark as active
     */
    public void markActiveDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {
            // Marks device as connected
            activeDevice = device;
            notifyDataSetChanged();
        }
    }

    /**
     * Marks device as found. Meaning that bound device is in range.
     * If it is in range it can be connected to.
     *
     * @param device to mark as found
     */
    public void markFoundDevice(BluetoothDevice device) {
        if(!mDevicesFound.contains(device)) {
            mDevicesFound.add(device);
            notifyDataSetChanged();
        }
    }

    /**
     * Header view holder shows time and time type for my orders and trading orders
     * - Its same for both my orders and trading
     */
    class DeviceViewHolder extends RecyclerView.ViewHolder  {
        TextView name, status;
        Button action;

        DeviceViewHolder(View itemView) {
            super(itemView);

            // Find item views
            name = itemView.findViewById(R.id.id_name);
            status = itemView.findViewById(R.id.id_status);
            action = itemView.findViewById(R.id.id_action);
        }
    }

    /**
     * Interface for communication with Fragment
     */
    public interface BlDevicesInterface {
        /**
         * Function that is called when device should be connected to.
         * Implementation should handles pairing and connecting to the device.
         * Only one device can be connected to at given time.
         *
         * @param device to connect to
         */
        void connectDevice(BluetoothDevice device);

        /**
         * Cancel bonding (pairing) with the device to enable pairing with
         * another device.
         *
         * @param device to cancel bond to
         */
        void cancelBonding(BluetoothDevice device);
    }

}
