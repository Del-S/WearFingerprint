package cz.uhk.fim.kikm.wearnavigation.activities.databaseTest;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.activities.bluetoothTest.BltAdapter;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionHandler;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionService;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDividerItemDecoration;

public class CouchebaseActivity extends AppCompatActivity {

    // Messages adapter
    private FingerprintAdapter mAdapter;
    // List of messages
    private RecyclerView mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_couchebase);

        // Divider for row items
        Drawable divider = ContextCompat.getDrawable(this, R.drawable.row_with_divider);
        // Load adapter instance
        mAdapter = new FingerprintAdapter(this);
        // List of send messages
        mList = findViewById(R.id.ac_list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(mAdapter);
        mList.addItemDecoration(new SimpleDividerItemDecoration(divider));

        loadFingerprints();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFingerprints();
    }

    private void loadFingerprints() {
        try {
            long startTime = System.currentTimeMillis();

            List<Fingerprint> fingerprints = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            Database database = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS).getDatabase("fingerprint");
            Query query = database.createAllDocumentsQuery();
            Iterator<QueryRow> iterator = query.run();
            while (iterator.hasNext()) {
                Map<String, Object> properties = iterator.next().getDocument().getProperties();
                final Fingerprint pojo = mapper.convertValue(properties, Fingerprint.class);
                fingerprints.add(pojo);
            }

            mAdapter.addFingerprints(fingerprints);

            Log.d("LoadingTime", "Time difference: " + (System.currentTimeMillis() - startTime));
        } catch (IOException | CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}
