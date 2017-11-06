package kikm.fim.uhk.cz.wearnavigationsimple.model.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import kikm.fim.uhk.cz.wearnavigationsimple.R;

import static android.support.v7.widget.RecyclerView.*;

public class BlDevicesAdapter extends Adapter {

    // Layout variables
    LayoutInflater mInflater;
    // List of devices
    private List<BluetoothDevice> mDevices = new ArrayList<>();

    public BlDevicesAdapter(Context context, List<BluetoothDevice> devices) {
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
        final BluetoothDevice item = mDevices.get(position);
        DeviceViewHolder deviceViewHolder = (DeviceViewHolder) holder;
        // Insert data into view holder
        deviceViewHolder.name.setText(item.getName());
        deviceViewHolder.address.setText(item.getAddress());
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

        DeviceViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.id_name);
            address = (TextView) itemView.findViewById(R.id.id_address);
        }
    }

}
