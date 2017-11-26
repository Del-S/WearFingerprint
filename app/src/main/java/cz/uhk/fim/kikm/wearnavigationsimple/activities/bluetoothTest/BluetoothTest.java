package cz.uhk.fim.kikm.wearnavigationsimple.activities.bluetoothTest;

import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cz.uhk.fim.kikm.wearnavigationsimple.BaseActivity;
import cz.uhk.fim.kikm.wearnavigationsimple.R;
import cz.uhk.fim.kikm.wearnavigationsimple.WearApplication;
import cz.uhk.fim.kikm.wearnavigationsimple.model.tasks.BluetoothConnection.BluetoothConnectionHandler;
import cz.uhk.fim.kikm.wearnavigationsimple.model.tasks.BluetoothConnection.BluetoothConnectionInterface;
import cz.uhk.fim.kikm.wearnavigationsimple.model.tasks.BluetoothConnection.BluetoothConnectionService;
import cz.uhk.fim.kikm.wearnavigationsimple.utils.SimpleDividerItemDecoration;

public class BluetoothTest extends BaseActivity implements BluetoothConnectionInterface {

    // Bluetooth connection service
    private BluetoothConnectionService mConnectionService;
    // Handler for Bluetooth connection service using this as interface
    private final Handler mHandler = new BluetoothConnectionHandler(this);
    // Messages adapter
    private BltAdapter mAdapter;
    // List of messages
    private RecyclerView mList;
    // Button to send message
    private Button sendMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load connection service
        mConnectionService = WearApplication.getConnectionService(this);
        mConnectionService.setHandler(mHandler);

        // Try to connect to the device
        BluetoothDevice device = mConfiguration.getBondedDevice();
        Log.d("BTTest", "Connect device: " + device.getName());
        Log.d("BTTest", "Connect device: " + device.getAddress());
        if(device != null) {
            mConnectionService.connect(device, true);
        }

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
                mConnectionService.write(writeMessage);
            }
        });
    }

    /**
     * Handles Fragment destroy function to disable services
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel Bluetooth communication service
        if (mConnectionService != null) {
            mConnectionService.stop();
        }
    }

    /**
     * Handles Fragment resume function to bind all functions back
     */
    @Override
    public void onResume() {
        super.onResume();

        // Start Bluetooth connection service
        if (mConnectionService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mConnectionService.getState() == BluetoothConnectionService.STATE_NONE) {
                // Start the Bluetooth chat services
                mConnectionService.start();
            }
        }
    }

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
    public void messageReceived(String message) {
        if(message != null && !message.isEmpty()) {
            mAdapter.addMessage(message);
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
