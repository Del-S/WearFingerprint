package kikm.fim.uhk.cz.wearnavigationsimple.model.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kikm.fim.uhk.cz.wearnavigationsimple.R;

public class BleBeaconsAdapter extends RecyclerView.Adapter {

    // Layout variables
    private LayoutInflater mInflater;
    // List of devices
    private List<Beacon> mBeacons = new ArrayList<>();

    public BleBeaconsAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Creating single item view
        View view = mInflater.inflate(R.layout.item_beacon, parent, false);
        return new BeaconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Get beacon and view to input data into
        final Beacon beacon = mBeacons.get(position);
        final BeaconViewHolder beaconViewHolder = (BeaconViewHolder) holder;

        // Set view holder data to display
        String name = beacon.getBluetoothName();
        if(!name.equals("") && !name.isEmpty()) {
            beaconViewHolder.name.setText(beacon.getBluetoothName());
        }
        beaconViewHolder.mac.setText(beacon.getBluetoothAddress());
        beaconViewHolder.rssi.setText(String.valueOf( beacon.getRssi() ) + "dBm");
    }

    @Override
    public int getItemCount() {
        return mBeacons.size();
    }

    /**
     * Add beacon to the list if it does not exist already
     *
     * @param beacon to add to the list
     */
    public void addBeacon(Beacon beacon) {
        // Add or replace beacon to the list
        if(!mBeacons.contains(beacon)) {
            mBeacons.add(beacon);
        } else {
            replaceBeacon(beacon);
        }

        // Inform adapter that list has changed
        notifyDataSetChanged();
    }

    /**
     * Add multiple beacons to the list if it does not exist already
     *
     * @param beacons to add to the list
     */
    public void addAllBeacons(Collection<Beacon> beacons) {
        if(beacons.size() > 0) {
            // Add or replace beacon to the list
            for (Beacon beacon : beacons) {
                if (!mBeacons.contains(beacon)) {
                    mBeacons.add(beacon);
                } else {
                    replaceBeacon(beacon);
                }
            }

            // Inform adapter that list has changed
            notifyDataSetChanged();
        }
    }

    /**
     * Replace beacon on specific location
     *
     * @param beacon to replace
     */
    private void replaceBeacon(Beacon beacon) {
        if(mBeacons.contains(beacon)) {
            // Get beacon position and replace it
            int position = mBeacons.indexOf(beacon);
            mBeacons.set(position, beacon);
        }
    }

    /**
     * Add beacon to the list if it does not exist already
     *
     * @param beacon to add to the list
     */
    public void removeBeacon(Beacon beacon) {
        if(mBeacons.contains(beacon)) {
            // Deletes a beacon from the list
            mBeacons.remove(beacon);
            // Inform adapter that list has changed
            notifyDataSetChanged();
        }
    }

    /**
     * Header view holder shows time and time type for my orders and trading orders
     * - Its same for both my orders and trading
     */
    class BeaconViewHolder extends RecyclerView.ViewHolder  {
        TextView name, mac, rssi;

        BeaconViewHolder(View itemView) {
            super(itemView);

            // Find item views
            name = itemView.findViewById(R.id.ib_name);
            mac = itemView.findViewById(R.id.ib_mac);
            rssi = itemView.findViewById(R.id.ib_rssi);
        }
    }
}
