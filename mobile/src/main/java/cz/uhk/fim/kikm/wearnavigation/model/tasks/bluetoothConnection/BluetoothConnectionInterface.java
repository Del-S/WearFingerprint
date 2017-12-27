package cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection;

import android.bluetooth.BluetoothDevice;

/**
 * Interface enables communication between BluetoothConnectionHandler and
 * context that started the BluetoothCommunication
 */
public interface BluetoothConnectionInterface {

    /**
     * Runs when specific device was connected.
     * Only single device can be connected to.
     *
     * @param device that was connected
     */
    void deviceConnected(BluetoothDevice device);

    /**
     * Failed connection to a specific device.
     * Inform context that connection failed.
     *
     * @param device that was not connected
     */
    void connectionFailed(BluetoothDevice device);

    /**
     * Connection failed but cannot identify device.
     * Inform context that connection failed.
     */
    void connectionFailed();

    /**
     * Pass received message to handle or display.
     *
     * @param message that was received
     */
    void messageReceived(String message);

    /**
     * Informs that message was send. Just to confirm.
     *
     * @param message that was send
     */
    void messageSend(String message);
}
