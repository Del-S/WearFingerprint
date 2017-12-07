package cz.uhk.fim.kikm.wearnavigation.activities.bluetoothTest;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import cz.uhk.fim.kikm.wearnavigation.BaseActivity;
import cz.uhk.fim.kikm.wearnavigation.R;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.ApiConnection;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionHandler;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionInterface;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionService;
import cz.uhk.fim.kikm.wearnavigation.utils.SimpleDividerItemDecoration;

public class BluetoothTest extends BaseActivity implements BluetoothConnectionInterface {

    // Handler for Bluetooth connection service using this as interface
    private final Handler mHandler = new BluetoothConnectionHandler(this);
    // Messages adapter
    private BltAdapter mAdapter;
    // List of messages
    private RecyclerView mList;
    // Button to send message
    private Button sendMessage;

    private BluetoothConnectionService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Divider for row items
        Drawable divider = ContextCompat.getDrawable(this, R.drawable.row_with_divider);
        // Load adapter instance
        mAdapter = new BltAdapter(this);
        // List of send messages
        mList = findViewById(R.id.abt_message_list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(mAdapter);
        mList.addItemDecoration(new SimpleDividerItemDecoration(divider));

        final EditText message = findViewById(R.id.abt_message);
        sendMessage = findViewById(R.id.abt_send);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] writeMessage = message.getText().toString().getBytes();
                message.setText("");
                mService.write(writeMessage);
            }
        });

        ApiConnection apiConnection = new ApiConnection(this);
        apiConnection.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Bind to BluetoothConnectionService
        //Intent intent = new Intent(this, BluetoothConnectionService.class);
        //bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unbindService(mConnection);
    }

    /*private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((BluetoothConnectionService.LocalBinder)iBinder).getInstance();
            mService.setHandler(mHandler);

            // Connect to the device
            BluetoothDevice device = mConfiguration.getBondedDevice();
            if(device != null) {
                mService.connect(device);
            } else {
                mService.start();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };*/

    @Override
    protected void updateUI() {
        // Not used
    }

    @Override
    public void deviceConnected(BluetoothDevice device) {
        // Getting display name or address for the device
        String displayName = device.getName();
        if(displayName == null || displayName.isEmpty()) {
            displayName = device.getAddress();
        }

        // Display connection error message to inform user
        Toast.makeText(this,
                "Connected to the device: " + displayName,
                Toast.LENGTH_SHORT).show();

        // Disable message sending
        sendMessage.setEnabled(true);
    }

    @Override
    public void connectionFailed(BluetoothDevice device) {
        // Getting display name or address for the device
        String displayName = device.getName();
        if(displayName == null || displayName.isEmpty()) {
            displayName = device.getAddress();
        }

        // Display connection error message to inform user
        Toast.makeText(this,
                String.format(getResources().getString(R.string.fdb_notice_connection_failed), displayName),
                Toast.LENGTH_SHORT).show();

        // Disable message sending
        sendMessage.setEnabled(false);
    }

    @Override
    public void connectionFailed() {
        // Disable message sending
        sendMessage.setEnabled(false);
        // Toast connection error message
        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void messageReceived(String message) {
        Log.d("svdsvsv", "Activity: message received");
        if(message != null && !message.isEmpty()) {
            mAdapter.addMessage(message);
            Log.d("svdsvsv", "Activity: message adapteres");
        }
    }

    @Override
    public void messageSend(String message) {
        if(message != null && !message.isEmpty()) {
            mAdapter.addMessage(message);
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_bluetooth_test;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_bluetooth_test;
    }
}
