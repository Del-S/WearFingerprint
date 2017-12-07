package cz.uhk.fim.kikm.wearnavigation.activities.databaseTest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;

public class FingerprintAdapter extends RecyclerView.Adapter {

    // Layout variables
    private LayoutInflater mInflater;
    // Messages list to display
    private List<Fingerprint> mFingerprints = new ArrayList<>();

    public FingerprintAdapter(Context context) {
        // Load inflater to create layout
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Creating single item view
        View view = mInflater.inflate(R.layout.item_fingeprint, parent, false);
        return new FingerprintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Get device and view to input data into
        final Fingerprint fingerprint = mFingerprints.get(position);
        final FingerprintViewHolder fingerprintViewHolder = (FingerprintViewHolder) holder;

        // Set text to display
        fingerprintViewHolder.fid.setText(fingerprint.getId().toString());
        fingerprintViewHolder.blCount.setText(String.valueOf( fingerprint.getBeaconEntries().size() ));
        fingerprintViewHolder.wCount.setText(String.valueOf( fingerprint.getWirelessEntries().size() ));
    }

    @Override
    public int getItemCount() {
        return mFingerprints.size();
    }

    public void addFingerprint(Fingerprint fingerprint) {
        if(!mFingerprints.contains(fingerprint)) {
            mFingerprints.add(fingerprint);
            notifyDataSetChanged();
        }
    }

    public void addFingerprints(List<Fingerprint> fingerprints) {
        for (Fingerprint fingerprint : fingerprints) {
            addFingerprint(fingerprint);
        }
    }

    class FingerprintViewHolder extends RecyclerView.ViewHolder  {
        TextView fid, blCount, wCount;

        FingerprintViewHolder(View itemView) {
            super(itemView);

            // Find item views
            fid = itemView.findViewById(R.id.if_id);
            blCount = itemView.findViewById(R.id.if_blCount);
            wCount = itemView.findViewById(R.id.if_wCount);
        }
    }

}
