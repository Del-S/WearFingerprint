package cz.uhk.fim.kikm.wearnavigationsimple.model.tasks.BluetoothConnection;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

public class BluetoothConnectionHandler extends Handler {
    private BluetoothConnectionInterface mInterface;

    private final String TAG = "BCH";

    public BluetoothConnectionHandler(BluetoothConnectionInterface pInterface) {
        mInterface = pInterface;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BluetoothConnectionService.MESSAGE_STATE_CHANGE:
                break;
            case BluetoothConnectionService.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mInterface.messageSend(writeMessage);
                break;
            case BluetoothConnectionService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mInterface.messageReceived(readMessage);
                break;
            case BluetoothConnectionService.MESSAGE_DEVICE_CONNECTED:
                // Call device connected via Interface
                BluetoothDevice mConnectedDevice = msg.getData().getParcelable(BluetoothConnectionService.DEVICE);
                if(mConnectedDevice != null) {
                    mInterface.deviceConnected(mConnectedDevice);
                }
                break;
            case BluetoothConnectionService.MESSAGE_DEVICE_CONNECTION_FAILED:
                BluetoothDevice mFailedDevice = msg.getData().getParcelable(BluetoothConnectionService.DEVICE);
                if(mFailedDevice != null) {
                    mInterface.connectionFailed(mFailedDevice);
                }
                break;
            case BluetoothConnectionService.MESSAGE_CONNECTION_FAILED:
                mInterface.connectionFailed();
                break;
        }
    }
}