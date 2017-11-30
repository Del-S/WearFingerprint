package cz.uhk.fim.kikm.wearnavigation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.widget.Toast;

import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionHandler;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionInterface;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionService;

public class MainActivity extends WearableActivity implements BluetoothConnectionInterface {

    private TextView mTextView;
    // Bluetooth check request code
    private final int REQUEST_ENABLE_BT = 1000;
    // Handler for Bluetooth connection service using this as interface
    private final Handler mHandler = new BluetoothConnectionHandler(this);

    private BluetoothConnectionService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        //setAmbientEnabled();

        checkBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Bind to BluetoothConnectionService
        Intent intent = new Intent(this, BluetoothConnectionService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((BluetoothConnectionService.LocalBinder)iBinder).getInstance();
            mService.setHandler(mHandler);

            // Connect to the device
            BluetoothDevice device = null;
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
    };

    /**
     * Checking if the device has bluetooth and if it is enabled.
     */
    protected void checkBluetooth() {
        // Loading bluetooth adapter to figure out if the device has bluetooth
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.am_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Check if the Bluetooth is enabled
        if(bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                checkBle();
            }
        }
    }

    /**
     * Checking to determine whether BLE is supported on the device.
     * - If it is not then display message and quit
     */
    protected void checkBle() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.am_bluetooth_le_support, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode != RESULT_OK) {
                    Toast.makeText(this, R.string.am_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    checkBle();
                }
                break;
        }
    }

    @Override
    public void deviceConnected(BluetoothDevice device) {
        Toast.makeText(this, "Connected device: " + device.getAddress(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void connectionFailed(BluetoothDevice device) {
        Toast.makeText(this, "Connection failed device: " + device.getAddress(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void connectionFailed() {
        Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void messageReceived(String message) {
        Toast.makeText(this, "You got a message: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void messageSend(String message) {
        Toast.makeText(this, "You send a message: " + message, Toast.LENGTH_SHORT).show();
    }
}
