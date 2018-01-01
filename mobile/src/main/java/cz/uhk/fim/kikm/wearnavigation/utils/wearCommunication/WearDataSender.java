package cz.uhk.fim.kikm.wearnavigation.utils.wearCommunication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cz.uhk.fim.kikm.wearnavigation.activities.scan.ScanActivity;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;

public class WearDataSender {
    private static final String TAG = "WearDataSender";
    private Context mContext;
    private Fingerprint mFingerprint;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String SCAN_PATH = "/scan";
    private static final String SCAN_STATUS_KEY = "scanStatus";
    private static final String SCAN_DATA = "scanData";

    private static final int STATUS_START = 0;

    public WearDataSender(Context context) {
        mContext = context.getApplicationContext();
    }

    public void initiateScanStart(Fingerprint fingerprint) {
        mFingerprint = fingerprint;

        // Starts wear activity
        new StartWearableActivityTask().execute();
    }

    private void sendScanStart() {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(SCAN_PATH);

        DataMap dataMap = dataMapRequest.getDataMap();
        dataMap.putInt(SCAN_STATUS_KEY, STATUS_START);
        dataMap.putLong("time", new Date().getTime());
        ParcelableUtils.putParcelable(dataMap, SCAN_DATA, mFingerprint);

        PutDataRequest request = dataMapRequest.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(mContext).putDataItem(request);

        dataItemTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                LOGD(TAG, "Sending scan data was successful: " + dataItem);
            }
        });
    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        private final int STATUS_COMPLETED = 0;
        private final int STATUS_FAILED = 1;
        private int status = STATUS_COMPLETED;

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(status != STATUS_FAILED) {
                sendScanStart();
            }
        }

        @WorkerThread
        private void sendStartActivityMessage(String node) {

            Task<Integer> sendMessageTask =
                    Wearable.getMessageClient(mContext).sendMessage(node, START_ACTIVITY_PATH, new byte[0]);

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                Integer result = Tasks.await(sendMessageTask);
                LOGD(TAG, "Message sent: " + result);
                status = STATUS_COMPLETED;
            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);
                status = STATUS_FAILED;

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
                status = STATUS_FAILED;
            }
        }

        @WorkerThread
        private Collection<String> getNodes() {
            HashSet<String> results = new HashSet<>();

            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(mContext).getConnectedNodes();

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                List<Node> nodes = Tasks.await(nodeListTask);

                for (Node node : nodes) {
                    results.add(node.getId());
                }

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);
                status = STATUS_FAILED;

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
                status = STATUS_FAILED;
            }

            return results;
        }
    }
}
