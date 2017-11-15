package kikm.fim.uhk.cz.wearnavigationsimple.model.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
        this.mInterface = mInterface;
        mBonded = bonded;
        mInflater = LayoutInflater.from(context);
        if(devices != null) {
            mDevices.addAll(devices);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
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
        if(!mDevices.contains(device)) {
            mDevices.add(device);
            notifyDataSetChanged();
        }
    }

    /**
     * Add device to the list if it does not exist already
     *
     * @param device to add to the list
     */
    public void removeDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {
            mDevices.remove(device);
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
            positionToReset = mDevices.indexOf(device);
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

            name = itemView.findViewById(R.id.id_name);
            status = itemView.findViewById(R.id.id_status);
            action = itemView.findViewById(R.id.id_action);
        }
    }

    /**
     * Interface for communication with Fragment
     */
    public interface BlDevicesInterface {
        void connectDevice(BluetoothDevice device);
        void cancelBonding(BluetoothDevice device);
    }

}
