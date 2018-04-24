package cz.uhk.fim.kikm.wearnavigation.utils.wearCommunication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;

/**
 * Used to send data into Wear device.
 * - Start wear activity.
 * - Start scan in wear.
 */
public class WearDataSender {

    private static final String TAG = "WearDataSender"; // Logging TAG
    private Context mContext;           // Application context to get ApiClients
    private Fingerprint mFingerprint;   // Fingerprint to send

    public WearDataSender(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Initiate scan by starting wear activity.
     * After wear activity is started message is send back to the mobile and scan is started after.
     * Note: scan is started (fingerprint send to wear) in sendScanStart().
     *
     * @param fingerprint to save for start scan
     */
    public void initiateScanStart(Fingerprint fingerprint) {
        mFingerprint = fingerprint;     // Save fingerprint to this instance
        // Start wear activity
        new StartWearableActivityTask().execute();
    }

    /**
     * Sends fingerprint into wear and starts the scan.
     */
    public void sendScanStart() {
        // If there is fingerprint to send
        if(mFingerprint != null) {
            // Reset device entry for Wear device
            mFingerprint.setDeviceEntry(null);

            // Load DataMap to save data into
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(DataLayerListenerService.SCAN_PATH);    // Request data Path
            DataMap dataMap = dataMapRequest.getDataMap();  // Get DataMap from request
            ParcelableUtils.putParcelable(dataMap, DataLayerListenerService.SCAN_DATA, mFingerprint);   // Save fingerprint into the map

            // Send request
            PutDataRequest request = dataMapRequest.asPutDataRequest();     // Convert to request
            request.setUrgent();                                            // Set as urgent
            Wearable.getDataClient(mContext).putDataItem(request);          // Send request into DataClient

            mFingerprint = null;    // Reset fingerprint
        }
    }

    /**
     * AsyncTask to start wear scanning activity.
     */
    @SuppressLint("StaticFieldLeak")
    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();  // Loads all the connected nodes
            for (String node : nodes) {
                sendStartActivityMessage(node);     // Start wear activity intent
            }
            return null;
        }

        /**
         * Sends message to start scan activity in a specific node.
         */
        @WorkerThread
        private void sendStartActivityMessage(String node) {
            // Send message to the specific node
            Task<Integer> sendMessageTask =
                    Wearable.getMessageClient(mContext).sendMessage(node,
                            DataLayerListenerService.START_ACTIVITY_PATH,
                            new byte[0]);

            try {
                // Block on a task and get the result synchronously (because this is on a background thread).
                Integer result = Tasks.await(sendMessageTask);
                Log.i(TAG, "Message sent to node: " + result);
            } catch (ExecutionException exception) {
                Log.e(TAG, "Task to start wear activity failed. ", exception);
            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred in starting wear activity. ", exception);
            }
        }

        /**
         * Gets connected nodes from NodeClient.
         */
        @WorkerThread
        private Collection<String> getNodes() {
            HashSet<String> results = new HashSet<>();  // Initiate result array

            // Get connected nodes from NodeClient
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(mContext).getConnectedNodes();

            try {
                // Block on a task and get the result synchronously (because this is on a background thread).
                List<Node> nodes = Tasks.await(nodeListTask);
                // Add node to the list
                for (Node node : nodes) {
                    results.add(node.getId());
                }
            } catch (ExecutionException exception) {
                Log.e(TAG, "Task add node failed. ", exception);
            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred in getNodes(). ", exception);
            }

            return results;     // Return nodes
        }
    }
}
