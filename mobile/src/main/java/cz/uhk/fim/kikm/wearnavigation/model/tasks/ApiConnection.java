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
    private ObjectMapper mapper;
    private int limit = 50;
    private DatabaseCRUD sqlDatabase;

    public ApiConnection(Context context) {
        sqlDatabase = new DatabaseCRUD(context);
        mapper = new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        try {
            this.database = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS).getDatabase("fingerprint");
        } catch (IOException | CouchbaseLiteException e) {
            e.printStackTrace();
        }
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
                        // TODO: changes count might not be ideal because if there was one change it will find only first document and not the changed one
                        int documentCount = replication.getChangesCount();
                        for (int i = 0; i < documentCount; i += limit) {
                            List<Fingerprint> fingerprints = queryDocuments(i);
                            sqlDatabase.saveMultipleFingerprints(fingerprints);
                        }
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
            Query query = database.createAllDocumentsQuery();
            query.setLimit(limit);
            query.setSkip(skip);
            Iterator<QueryRow> iterator = query.run();
            while (iterator.hasNext()) {
                Map<String, Object> properties = iterator.next().getDocument().getProperties();
                final Fingerprint pojo = mapper.convertValue(properties, Fingerprint.class);
                fingerprints.add(pojo);
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return fingerprints;
    }
}