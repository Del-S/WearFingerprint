package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.ReplicationFilter;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.support.LazyJsonObject;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;

public class ApiConnection extends AsyncTask<Void, Void, Void> {

    private Database database;
    private Replication replication;
    private long startTime, endTime;
    private ObjectMapper mapper;
    private int limit = 30;
    private DatabaseCRUD sqlDatabase;

    public ApiConnection(Context context) {
        sqlDatabase = new DatabaseCRUD(context);
        mapper = new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        try {
            this.database = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS).getDatabase("fingerprint");
            this.database.getView("fingerprint").setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    emitter.emit(document.get("timestamp"), document);
                }
            }, "1");

            // Test delete
            /*Query query = database.getView("fingerprint").createQuery();
            query.setLimit(20);
            Iterator<QueryRow> iterator = query.run();
            while (iterator.hasNext()) {
                QueryRow row = iterator.next();
                Log.d("svdsvsdvsdv", "Deliting document: " + row.getDocumentId());
                try {
                    database.deleteLocalDocument(row.getDocumentId());
                } catch (CouchbaseLiteException e) {

                }
            }*/
        } catch (IOException | CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        startTime = System.currentTimeMillis();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        downloadDatabase();
        return null;
    }

    private void downloadDatabase() {
        try {
            replication = database.createPullReplication(new URL("http://beacon.uhk.cz/fingerprintgw"));
            replication.addChangeListener(new Replication.ChangeListener() {
                @Override
                public void changed(Replication.ChangeEvent changeEvent) {
                    if(replication.getStatus() != Replication.ReplicationStatus.REPLICATION_ACTIVE) {
                        int documentCount = replication.getChangesCount();
                        //int documentCount = database.getDocumentCount();
                        Log.d("DocCount", "Document count: " + documentCount);
                        for (int i = 0; i < documentCount; i += limit) {
                            List<Fingerprint> fingerprints = queryDocuments(i);
                            sqlDatabase.saveMultipleFingerprints(fingerprints);
                        }

                        endTime = System.currentTimeMillis();
                        Log.d("Tasdvest", "Fingerprint count: " + sqlDatabase.getFingerprintCount());
                        Log.d("TImeDiff", "Equals: " + (endTime - startTime));
                    }
                }
            });
            replication.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Fingerprint> queryDocuments(int skip) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        try {
            Query query = database.getView("fingerprint").createQuery();
            query.setLimit(limit);
            query.setSkip(skip);
            QueryEnumerator result = query.run();
            Iterator<QueryRow> iterator = query.run();
            while (iterator.hasNext()) {
                Map<String, Object> propertires = iterator.next().getDocument().getProperties();
                final Fingerprint pojo = mapper.convertValue(propertires, Fingerprint.class);
                fingerprints.add(pojo);
                //fingerprints.add(new Fingerprint((LazyJsonObject) row.getValue());
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return fingerprints;
    }
}