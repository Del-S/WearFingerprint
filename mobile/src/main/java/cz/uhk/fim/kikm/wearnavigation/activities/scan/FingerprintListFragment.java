package cz.uhk.fim.kikm.wearnavigation.activities.scan;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.adapters.scan.FingerprintAdapter;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDividerItemDecoration;

public class FingerprintListFragment extends Fragment {

    public static final String ARG_POS_X = "posX";
    public static final String ARG_POS_Y = "posY";

    private DatabaseCRUD mDatabase;        // Database to get fingerprint data from
    private RecyclerView mFingerprintList; // List to display fingerprints
    private TextView mEmptyMessage;        // Message with notifying no fingerprints found
    private FingerprintAdapter mAdapter;   // Adapter that displays Fingerprints in the list
    private int posX = 0;
    private int posY = 0;

    /**
     * Creates instance of this Fragment.
     * Adds position to the arguments to display specific fragments.
     *
     * @param posX to save into args
     * @param posY to save into args
     * @return instance of this fragment
     */
    public static FingerprintListFragment newInstance(int posX, int posY) {
        // Create arguments
        Bundle args = new Bundle();
        args.putInt(ARG_POS_X, posX);
        args.putInt(ARG_POS_Y, posY);

        FingerprintListFragment fragment = new FingerprintListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View listView = inflater.inflate(R.layout.fragment_list_fingerprint, container, false);
        if(getArguments() != null) {
            posX = getArguments().getInt(ARG_POS_X);
            posY = getArguments().getInt(ARG_POS_Y);
        }

        if(getActivity() != null) {
            TextView title = listView.findViewById(R.id.flf_title);
            title.setText(String.format(
                    getActivity().getResources().getString(R.string.flf_title),
                    posX,
                    posY));
        }

        // Get data and initiate list adapter
        initDatabase();
        mAdapter = new FingerprintAdapter(getActivity());

        // Recycler view for fingerprints
        mFingerprintList = listView.findViewById(R.id.flf_list_fingerprints);
        if(getActivity() != null) {
            Drawable divider = ContextCompat.getDrawable(getActivity(), R.drawable.row_with_divider);    // Divider for row items
            mFingerprintList.addItemDecoration(new SimpleDividerItemDecoration(divider));
        }
        mFingerprintList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFingerprintList.setAdapter(mAdapter);

        mEmptyMessage = listView.findViewById(R.id.flf_empty_list);

        updateUI();

        return listView;
    }

    /**
     * Updates list and hides progressBar after data are loaded.
     */
    public void updateUI() {
        initDatabase();

        // Load fingerprint data and put them into adapter
        List<Fingerprint> list = mDatabase.getFingerprintsByPosition(posX, posY, false);
        mAdapter.addFingerprints(list);

        // Change visibility for recyclerView and progressBar
        if(list.isEmpty()) {
            mEmptyMessage.setVisibility(View.VISIBLE);
            mFingerprintList.setVisibility(View.GONE);
        } else {
            mEmptyMessage.setVisibility(View.GONE);
            mFingerprintList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Initiates database if it was not done yet.
     */
    private void initDatabase() {
        if(mDatabase == null)
            mDatabase = new DatabaseCRUD(getActivity());
    }
}
