package cz.uhk.fim.kikm.wearnavigationsimple.model.tasks.BluetoothConnection;

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
     *
     * @param device that was not connected
     */
    void connectionFailed(BluetoothDevice device);
}
