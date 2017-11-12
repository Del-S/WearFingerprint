package kikm.fim.uhk.cz.wearnavigationsimple.model.adapters;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kikm.fim.uhk.cz.wearnavigationsimple.R;
import kikm.fim.uhk.cz.wearnavigationsimple.WearApplication;
import kikm.fim.uhk.cz.wearnavigationsimple.activities.devices.BluetoothDevicesFragment;
import kikm.fim.uhk.cz.wearnavigationsimple.activities.devices.ShowDevicesActivity;

import static android.support.v7.widget.RecyclerView.*;

public class BlDevicesAdapter extends Adapter {

    // Layout variables
    private LayoutInflater mInflater;
    // List of devices
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    // Interface to communicate with fragment
    private BlDevicesInterface mInterface;
    // Information if this list shows bonded devices
    private boolean mBonded = false;

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
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BluetoothDevice device = mDevices.get(position);
        final DeviceViewHolder deviceViewHolder = (DeviceViewHolder) holder;
        // Insert data into view holder
        deviceViewHolder.name.setText(device.getName());
        deviceViewHolder.status.setText(device.getAddress());
        deviceViewHolder.action.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceViewHolder.action.setEnabled(false);

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(deviceViewHolder.content);
                constraintSet.clear(R.id.id_name, ConstraintSet.BOTTOM);
                constraintSet.applyTo(deviceViewHolder.content);

                deviceViewHolder.status.setVisibility(VISIBLE);

                if(mBonded) {
                    deviceViewHolder.status.setText(R.string.id_action_connecting);
                } else {
                    deviceViewHolder.status.setText(R.string.id_action_pairing);
                }

                mInterface.connectDevice(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public void addDevice(BluetoothDevice device) {
        if(!mDevices.contains(device)) {
            mDevices.add(device);
            notifyDataSetChanged();
        }
    }

    public void removeDevice(BluetoothDevice device) {
        if(mDevices.contains(device)) {
            mDevices.remove(device);
            notifyDataSetChanged();
        }
    }

    /**
     * Header view holder shows time and time type for my orders and trading orders
     * - Its same for both my orders and trading
     */
    class DeviceViewHolder extends RecyclerView.ViewHolder  {
        ConstraintLayout content;
        TextView name, status;
        Button action;

        DeviceViewHolder(View itemView) {
            super(itemView);

            content = (ConstraintLayout) itemView.findViewById(R.id.id_content);
            name = (TextView) itemView.findViewById(R.id.id_name);
            status = (TextView) itemView.findViewById(R.id.id_status);
            action = (Button) itemView.findViewById(R.id.id_action);
        }
    }

    public interface BlDevicesInterface {
        void connectDevice(BluetoothDevice device);
    }

}
