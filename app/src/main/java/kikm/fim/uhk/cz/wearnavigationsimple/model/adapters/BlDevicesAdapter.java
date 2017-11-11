package kikm.fim.uhk.cz.wearnavigationsimple.model.adapters;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
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
import kikm.fim.uhk.cz.wearnavigationsimple.activities.devices.ShowDevicesActivity;

import static android.support.v7.widget.RecyclerView.*;

public class BlDevicesAdapter extends Adapter {

    // Layout variables
    LayoutInflater mInflater;
    // List of devices
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private UUID appUUID;
    private Context context;

    public BlDevicesAdapter(Context context, UUID appUUID, List<BluetoothDevice> devices) {
        this.appUUID = appUUID;
        this.context = context;
        mInflater = LayoutInflater.from(context);
        if(devices != null) {
            mDevices.addAll(devices);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        view = mInflater.inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BluetoothDevice device = mDevices.get(position);
        DeviceViewHolder deviceViewHolder = (DeviceViewHolder) holder;
        // Insert data into view holder
        deviceViewHolder.name.setText(device.getName());
        deviceViewHolder.address.setText(device.getAddress());
        deviceViewHolder.connect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShowDevicesActivity) context).connectDevice(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public void addDevice(BluetoothDevice device) {
        mDevices.add(device);
    }

    /**
     * Header view holder shows time and time type for my orders and trading orders
     * - Its same for both my orders and trading
     */
    class DeviceViewHolder extends RecyclerView.ViewHolder  {
        TextView name, address;
        Button connect;

        DeviceViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.id_name);
            address = (TextView) itemView.findViewById(R.id.id_address);
            connect = (Button) itemView.findViewById(R.id.id_connect);
        }
    }

}
