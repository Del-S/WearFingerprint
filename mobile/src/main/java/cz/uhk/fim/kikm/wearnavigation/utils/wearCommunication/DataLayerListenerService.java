package cz.uhk.fim.kikm.wearnavigation.utils.wearCommunication;

import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import cz.uhk.fim.kikm.wearnavigation.model.configuration.Configuration;
import cz.uhk.fim.kikm.wearnavigation.model.database.DeviceEntry;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.helpers.DatabaseCRUD;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.FingerprintScanner;

/**
 * Listens to DataItems and Messages from the local node.
 * Handled saving of Fingerprint from Wear device.
 */
public class DataLayerListenerService extends WearableListenerService {

    // All communication data keys
    public static final String START_ACTIVITY_PATH = "/start-activity";             // Path to start wear activity
    public static final String ACTIVITY_STARTED_PATH = "/start-activity-complete";  // Path to check if wear activity was started
    public static final String SCAN_PATH = "/scan";                                 // Path to start new scan in wear
    public static final String SCAN_PATH_COMPLETE = "/scan-complete";               // Path to get the data from wear
    public static final String SCAN_STATUS_KEY = "scanStatus";                      // Data key to check the status of wear scan
    public static final String SCAN_DATA = "scanData";                              // Data key to send/get fingerprint data

    private DatabaseCRUD mDatabase;  // Database to save fingerprint to
    private DeviceEntry mDevice;     // DeviceEntry instance to get telephone

    @Override
    public void onCreate() {
        super.onCreate();

        // Initiate database connection
        mDatabase = new DatabaseCRUD(this);
        mDevice = Configuration.getConfiguration(this).getDevice(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // Check all the data events
        for (DataEvent event : dataEvents) {
            // If the data event was changed
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Get path of the event and check if its scan complete path
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.SCAN_PATH_COMPLETE.equals(path)) {
                    // Load data from event
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    DataMap data = dataMapItem.getDataMap();

                    // Check if scan was completed before data was send
                    if( (data.getInt(DataLayerListenerService.SCAN_STATUS_KEY) == FingerprintScanner.TASK_STATE_DONE)
                            && (data.getByteArray(DataLayerListenerService.SCAN_DATA) != null) ){
                        // Load fingerprint from parcel
                        Fingerprint fingerprint = ParcelableUtils.getParcelable(data,
                                DataLayerListenerService.SCAN_DATA,
                                Fingerprint.CREATOR);

                        // If fingerprint is loaded it is saved into the database
                        if(fingerprint != null) {
                            // Set deviceId to this fingerprint to enable querying and save
                            if(mDevice != null) {
                                fingerprint.getDeviceEntry().setTelephone(mDevice.getTelephone());
                            }
                            mDatabase.saveFingerprint(fingerprint, null, true);
                            // TODO: Modify
                            // Create a handler to post messages to the main thread
                            int wirelessCount = fingerprint.getWirelessEntries().size();
                            Handler mHandler = new Handler(getMainLooper());
                            mHandler.post(() -> Toast.makeText(getApplicationContext(), "WearFingerprint was saved. W:"+wirelessCount, Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Not used
    }
}