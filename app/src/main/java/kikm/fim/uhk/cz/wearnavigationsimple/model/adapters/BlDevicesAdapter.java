package kikm.fim.uhk.cz.wearnavigationsimple.model.adapters;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

import kikm.fim.uhk.cz.wearnavigationsimple.R;

import static android.support.v7.widget.RecyclerView.*;

public class BlDevicesAdapter extends RecyclerView.Adapter {

    // Layout variables
    private LayoutInflater mInflater;
    // List of devices
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    // Interface to communicate with fragment
    private BlDevicesInterface mInterface;
    // Information if this list shows bonded devices
    private boolean mBonded = false;
    // Position of item that was last activated
    private BluetoothDevice activeDevice;
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

        // Reset design and active device on current position
        if(positionToReset == position) {
            // Disable button to prevent multiple clicks
            deviceViewHolder.action.setEnabled(true);
            // Make status information visible
            deviceViewHolder.status.setVisibility(GONE);
            // Position was reset so we can reset identification
            positionToReset = -1;
            // Also reset active device if this device was reset
            if(activeDevice == device) {
                activeDevice = null;
            }
        }

        // Set name or mas address to display
        String name = device.getName();
        if(name.equals("") || name.isEmpty()) {
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

                // Set device position that is active
                activeDevice = device;

                // Disable button to prevent multiple clicks
                deviceViewHolder.action.setEnabled(false);
                // Make status information visible
                deviceViewHolder.status.setVisibility(VISIBLE);

                // Change status test based on bond state
                if (mBonded) {
                    deviceViewHolder.status.setText(R.string.id_action_connecting);
                } else {
                    deviceViewHolder.status.setText(R.string.id_action_pairing);
                }

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
     * Add device to the list if it does not exist already
     *
     * @param device to add to the list
     */
    public void addDevice(BluetoothDevice device) {
        // Add or replace beacon to the list
        if(!mDevices.contains(device)) {
            mDevices.add(device);
        } else {
            replaceDevice(device);
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
            // Deletes a beacon from the list
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
            notifyDataSetChanged();
        }
    }

    /**
     * Replace device on specific location
     *
     * @param device to replace
     */
    private void replaceDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {
            // Get beacon position and replace it
            int position = mDevices.indexOf(device);
            mDevices.set(position, device);
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
