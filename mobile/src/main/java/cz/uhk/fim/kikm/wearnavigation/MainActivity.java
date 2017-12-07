package cz.uhk.fim.kikm.wearnavigation;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;

import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionHandler;
import cz.uhk.fim.kikm.wearnavigation.model.tasks.BluetoothConnection.BluetoothConnectionInterface;

public class MainActivity extends BaseActivity implements BluetoothConnectionInterface {

    // Handler for Bluetooth connection service using this as interface
    private final Handler mHandler = new BluetoothConnectionHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ApiConnection apiConnection = new ApiConnection(this);
        //apiConnection.execute();
    }

    @Override
    protected void updateUI() {
        // Not used
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
    public void deviceConnected(BluetoothDevice device) {
    }

    @Override
    public void connectionFailed(BluetoothDevice device) {
    }

    @Override
    public void connectionFailed() {
    }

    @Override
    public void messageReceived(String message) {
    }

    @Override
    public void messageSend(String message) {
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.action_show_main;
    }
}
