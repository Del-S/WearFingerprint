/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.uhk.fim.kikm.wearnavigation;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Listens to DataItems and Messages from the local node.
 */
public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerService";   // Logging tag

    // All communication data keys
    private static final String START_ACTIVITY_PATH = "/start-activity";              // Path to start wear activity
    private static final String ACTIVITY_STARTED_PATH = "/start-activity-complete";   // Path to check if wear activity was started
    public static final String SCAN_PATH = "/scan";                                   // Path to start new scan in wear
    public static final String SCAN_PATH_COMPLETE = "/scan-complete";                 // Path to get the data from wear
    public static final String SCAN_STATUS_KEY = "scanStatus";                        // Data key to check the status of wear scan
    public static final String SCAN_DATA = "scanData";                                // Data key to send/get fingerprint data

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // Not used
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Check to see if the message is to start an activity
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            // Create intent and start activity
            Intent startIntent = new Intent(this, MainActivity.class);  // Intent to main activity
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);   // Does not start new Activity just brings it to front
            startActivity(startIntent);                            // Start activity

            // Send confirmation message back to the node
            Wearable.getMessageClient(this).sendMessage(messageEvent.getSourceNodeId(), ACTIVITY_STARTED_PATH,
                    null);
        }
    }
}