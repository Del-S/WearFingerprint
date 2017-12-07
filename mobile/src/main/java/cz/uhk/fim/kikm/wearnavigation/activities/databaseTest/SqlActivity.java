package cz.uhk.fim.kikm.wearnavigation.activities.databaseTest;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDividerItemDecoration;

public class SqlActivity extends AppCompatActivity {

    // Messages adapter
    private FingerprintAdapter mAdapter;
    // List of messages
    private RecyclerView mList;
    private DatabaseCRUD mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sql);

        mDatabase = new DatabaseCRUD(this);

        // Divider for row items
        Drawable divider = ContextCompat.getDrawable(this, R.drawable.row_with_divider);
        // Load adapter instance
        mAdapter = new FingerprintAdapter(this);
        // List of send messages
        mList = findViewById(R.id.as_list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(mAdapter);
        mList.addItemDecoration(new SimpleDividerItemDecoration(divider));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFingerprints();
    }

    private void loadFingerprints() {
        long startTime = System.currentTimeMillis();

        List<Fingerprint> fingerprints = mDatabase.getAllFingerprints();
        mAdapter.addFingerprints(fingerprints);

        Log.d("LoadingTime", "Time difference: " + (System.currentTimeMillis() - startTime));
    }
}
