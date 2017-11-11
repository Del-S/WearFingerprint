package kikm.fim.uhk.cz.wearnavigationsimple.activities.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import kikm.fim.uhk.cz.wearnavigationsimple.R;
import kikm.fim.uhk.cz.wearnavigationsimple.WearApplication;
import kikm.fim.uhk.cz.wearnavigationsimple.model.adapters.BlDevicesAdapter;
import kikm.fim.uhk.cz.wearnavigationsimple.model.configuration.Configuration;

public class BluetoothDevicesFragment extends Fragment {

    private final String TAG = "BluetoothDevicesFragment";

    private BlDevicesAdapter mBlDeviceAdapterBonded, mBlDeviceAdapter;
    private ActivityConnection mInterface;

    BluetoothAdapter mBluetoothAdapter;
    List<BluetoothDevice> bondedDevices = new ArrayList<>();

    /**
     * Create instance of BluetoothDevicesFragment and pass variable to it to change functions
     *
     * @return BluetoothDevicesFragment
     */
    public static BluetoothDevicesFragment newInstance() {
        return new BluetoothDevicesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if startup activity implements listener
        try {
            mInterface = (ActivityConnection) getActivity();
        } catch (Exception e) {
            throw new ClassCastException(TAG
                    + " must implement TradingInterface");
        }

        mBluetoothAdapter = mInterface.getBluetoothAdapter();
        bondedDevices.addAll(mBluetoothAdapter.getBondedDevices());

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices_bluetooth, container, false);

        Configuration configuration = ((WearApplication) getActivity().getApplicationContext()).getConfiguration();

        mBlDeviceAdapterBonded = new BlDevicesAdapter(getActivity(), configuration.getAppUUID(), bondedDevices);
        RecyclerView recyclerViewBonded = rootView.findViewById(R.id.fdb_bluetooth_list_bonded);
        recyclerViewBonded.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerViewBonded.setAdapter(mBlDeviceAdapterBonded);

        mBlDeviceAdapter = new BlDevicesAdapter(getActivity(), configuration.getAppUUID(), null);
        RecyclerView recyclerView = rootView.findViewById(R.id.fdb_bluetooth_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mBlDeviceAdapter);

        Button button = rootView.findViewById(R.id.fdb_start_scan);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.startDiscovery();
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Don't forget to unregister the ACTION_FOUND receiver.
        getActivity().unregisterReceiver(mReceiver);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBlDeviceAdapter.addDevice(device);
                mBlDeviceAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * Interface to handle connection between Fragment and Activity
     */
    public interface ActivityConnection {
        BluetoothAdapter getBluetoothAdapter();
    }
}
