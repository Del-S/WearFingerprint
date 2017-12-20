package cz.uhk.fim.kikm.wearnavigation.model.tasks.bluetoothConnection;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothConnectionService extends Service {

    // Debugging
    private static final String TAG = "BluetoothConnection";

    // Message types sent from the BluetoothConnectionService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;             // When state of this service changed
    public static final int MESSAGE_READ = 2;                     // When service has read a message from other device
    public static final int MESSAGE_WRITE = 3;                    // When message was send to other device
    public static final int MESSAGE_DEVICE_CONNECTED = 4;         // When device was connected
    public static final int MESSAGE_DEVICE_CONNECTION_FAILED = 5; // When device was disconnected
    public static final int MESSAGE_CONNECTION_FAILED = 6;        // When connection failed
    public static final String DEVICE = "device";                 // Key name used to send device from BluetoothConnectionService to Handler

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothConnectionSecure";
    // TODO: change UUID
    // Unique UUID for this application used for BluetoothSocket identification
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    private BluetoothAdapter mAdapter;          // Bluetooth adapter instance
    private BluetoothDevice mDeviceConnected;   // Currently connected device (in this service only)
    private Handler mHandler;                   // Handler to send messages to
    private AcceptThread mAcceptThread;         // Server thread accepting connections
    private ConnectThread mConnectThread;       // Thread used to connect server with client
    private ConnectedThread mConnectedThread;   // Connected thread to send/receive messages to/from other device
    private int mState;                         // Current state of this service
    private int mOldState;                      // Old state used in messages to handle different state changes
    private boolean mStopping = false;          // Stopping identification if this service is stopping then no new Threads cannot be created

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // Service is doing nothing
    public static final int STATE_LISTEN = 1;     // Service is listening for incoming connections
    public static final int STATE_CONNECTING = 2; // Service is initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // Service is connected to a remote device

    // Instance of service binder
    private final IBinder mIBinder = new LocalBinder();
    // Service binder to enable bond with this service
    public class LocalBinder extends Binder {
        public BluetoothConnectionService getInstance() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothConnectionService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mOldState = mState;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mStopping = true;
        stop();
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        mStopping = false;
        return mIBinder;
    }

    /**
     * Sets handler to communicate with.
     *
     * @param handler to communicate
     */
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Update Activity according to the current state of the Bluetooth connection
     */
    private synchronized void notifyStatusChanged() {
        // Get state using synchronized method
        mState = getState();
        // Give the new state to the Handler to notify context
        if(mHandler != null) {
            mHandler.obtainMessage(MESSAGE_STATE_CHANGE, mState, mOldState).sendToTarget();
        }
        // Rewrite old state
        mOldState = mState;
    }


    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the Bluetooth connection service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        // Don't start any new threads if the service is being stopped
        if(!mStopping) {
            Log.i(TAG, "Starting BluetoothConnectionService");

            // Cancel any thread attempting to make a connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Start the thread to listen on a BluetoothServerSocket
            if (mAcceptThread == null) {
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            }

            // Reset connected device
            mDeviceConnected = null;

            // Send status changed info
            notifyStatusChanged();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if(getState() != STATE_CONNECTED || !device.equals(mDeviceConnected) ) {
            Log.i(TAG, "Connecting to device: " + device);

            // Cancel any thread attempting to make a connection
            if (mState == STATE_CONNECTING) {
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(device);
            mConnectThread.start();

            // Send status changed info
            notifyStatusChanged();
        } else {
            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_CONNECTED);
            Bundle bundle = new Bundle();
            bundle.putParcelable(DEVICE, device);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.i(TAG, "Connected to device: " + device);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Save connected device for checks
        mDeviceConnected = device;

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putParcelable(DEVICE, device);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Send status changed info
        notifyStatusChanged();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.i(TAG, "Stopping BluetoothConnectionService");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Reset connected device
        mDeviceConnected = null;

        mState = STATE_NONE;
        // Send status changed info
        notifyStatusChanged();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed(BluetoothDevice device) {
        // Reset connected device
        mDeviceConnected = null;

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_CONNECTION_FAILED);
        Bundle bundle = new Bundle();
        bundle.putParcelable(DEVICE, device);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Send status changed info
        notifyStatusChanged();

        // Start the service over to restart listening mode
        start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Reset connected device
        mDeviceConnected = null;

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_FAILED);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Send status changed info
        notifyStatusChanged();

        // Start the service over to restart listening mode
        start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                        MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            Log.i(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothConnectionService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");

        }

        void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e3) {
                    Log.e(TAG, "ConnectThread: unable to close() socket during connection failure", e3);
                }

                Log.e(TAG, "ConnectThread: unable to connect() socket during connection failure", e);

                connectionFailed(mmDevice);
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnectionService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            setName("ConnectedThread");

            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        /**
         * Cancel socket to disable connection and enable Thread to end
         */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}