package cz.uhk.fim.kikm.wearnavigation.model.adapters.scan;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;

public class FingerprintAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private LayoutInflater mInflater;                             // Inflater to inflate views with
    private List<Fingerprint> mFingerprints = new ArrayList<>();  // List od Employees to display
    private SimpleDateFormat mFormatter;
    private DeviceEntry mDevice;

    public FingerprintAdapter(Context context) {
        // Load required instances
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        mDevice = Configuration.getConfiguration(context).getDevice(context);
    }

    /**
     * Add fingerprints to the list
     *
     * @param fingerprints to add into the Recycler view
     */
    public void addFingerprints(List<Fingerprint> fingerprints) {
        if(fingerprints != null) {
            // Add fingerprints to the list
            mFingerprints.clear();
            mFingerprints.addAll(fingerprints);
            notifyDataSetChanged();
        }
    }

    /**
     * Add single Fingerprint to the list
     *
     * @param fingerprint to add into the Recycler view
     */
    public void addFingerprint(Fingerprint fingerprint) {
        if(fingerprint != null) {
            // Add fingerprint to the list
            mFingerprints.add(fingerprint);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Creating single item view
        View view = mInflater.inflate(R.layout.item_fingerprint, parent, false);
        return new FingerprintHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Fingerprint fingerprint = mFingerprints.get(position);
        final FingerprintHolder fingerprintHolder = (FingerprintHolder) holder;

        if(fingerprint != null) {
            // Set fingerprint name
            fingerprintHolder.name.setText( String.format(
                    mContext.getResources().getString(R.string.if_name),
                    fingerprint.getDbId()));

            // Set fingerprint description if start time is not empty
            if(fingerprint.getScanStart() != 0) {
                fingerprintHolder.description.setText(
                        mFormatter.format(new Date(fingerprint.getScanStart())));
            }

            DeviceEntry fingerprintDevice = fingerprint.getDeviceEntry();
            // Sets image resource to the view
            if(fingerprintDevice != null && fingerprintDevice.getType().equals("wear"))
                fingerprintHolder.image.setImageResource(R.drawable.map_marker_normal_wear);     // Set wear icon
            else
                fingerprintHolder.image.setImageResource(R.drawable.map_marker_normal_phone);    // Set phone icon

            // TODO: add images for wear devices
            // Set image resource based this device
            if(mDevice != null && fingerprintDevice != null && (mDevice.equals(fingerprintDevice) ||
                    (mDevice.getTelephone() != null &&
                            mDevice.getTelephone().equals(fingerprintDevice.getTelephone())))) {
                // Sets image resource to the view
                if( fingerprintDevice.getType().equals("wear"))
                    fingerprintHolder.image.setImageResource(R.drawable.map_marker_own_wear);     // Set wear icon
                else
                    fingerprintHolder.image.setImageResource(R.drawable.map_marker_own_phone);    // Set phone icon
            }
        }
    }

    @Override
    public int getItemCount() {
        return mFingerprints.size();
    }

    /**
     * Fingerprint view holder shows name, description and image.
     * Also enables to delete this fingerprint (only from this device).
     */
    private class FingerprintHolder extends RecyclerView.ViewHolder  {
        TextView name, description;
        ImageView image;
        ImageButton delete;

        FingerprintHolder(View itemView) {
            super(itemView);

            // Find item views
            name = itemView.findViewById(R.id.if_name);
            description = itemView.findViewById(R.id.if_description);
            image = itemView.findViewById(R.id.if_image);
            delete = itemView.findViewById(R.id.if_delete);
        }
    }

}
