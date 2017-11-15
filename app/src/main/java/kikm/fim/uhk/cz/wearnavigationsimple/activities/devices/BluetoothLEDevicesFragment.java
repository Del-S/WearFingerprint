package kikm.fim.uhk.cz.wearnavigationsimple.activities.devices;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;

import java.util.Collection;

import kikm.fim.uhk.cz.wearnavigationsimple.R;
import kikm.fim.uhk.cz.wearnavigationsimple.WearApplication;
import kikm.fim.uhk.cz.wearnavigationsimple.model.adapters.BleBeaconsAdapter;
import kikm.fim.uhk.cz.wearnavigationsimple.model.tasks.BluetoothLEScanner;
import kikm.fim.uhk.cz.wearnavigationsimple.model.tasks.BluetoothLEScannerInterface;
import kikm.fim.uhk.cz.wearnavigationsimple.utils.SimpleDividerItemDecoration;

public class BluetoothLEDevicesFragment extends Fragment implements BluetoothLEScannerInterface {

    // Error and debug tag
    private final String TAG = "BleDevicesFragment";

    // Bluetooth scanner class that handles connection to AltBeacon scanning
    private BluetoothLEScanner mBluetoothLEScanner;

    // TextView informing about discovering devices
    private TextView mScanForDevices;

    // Bluetooth LE devices adapter for RecyclerView
    private BleBeaconsAdapter mBleBeaconsAdapter;

    /**
     * Create instance of BluetoothLEDevicesFragment and pass variable to it to change functions
     *
     * @return BluetoothLEDevicesFragment instance
     */
    public static BluetoothLEDevicesFragment newInstance() {
        return new BluetoothLEDevicesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initiate scanner
        mBluetoothLEScanner = new BluetoothLEScanner((WearApplication) getActivity().getApplicationContext(), this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices_bluetooth_low_energy, container, false);

        // Find view for scanning devices
        mScanForDevices = rootView.findViewById(R.id.fdble_title_search);

        // Divider for row items
        Drawable divider = ContextCompat.getDrawable(getActivity(), R.drawable.row_with_divider);

        // Load adapter for the Bluetooth LE devices
        mBleBeaconsAdapter = new BleBeaconsAdapter(getActivity());

        // Recycler view for bonded devices
        RecyclerView devicesView = rootView.findViewById(R.id.fdble_list);
        devicesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        devicesView.setAdapter(mBleBeaconsAdapter);
        devicesView.addItemDecoration(new SimpleDividerItemDecoration(divider));

        return rootView;
    }

    @Override
    public void serviceConnected() {
        // Start beacon scanning
        startScanning();
    }

    @Override
    public void foundBeacon(final Beacon beacon) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBleBeaconsAdapter.addBeacon(beacon);
            }
        });
    }

    @Override
    public void foundMultipleBeacons(final Collection<Beacon> beacons) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBleBeaconsAdapter.addAllBeacons(beacons);
            }
        });
    }

    /**
     * Starts classic bluetooth discovery for limited amount of time
     */
    private void startScanning() {
        if( !mBluetoothLEScanner.isScanning() ) {
            // Starting bluetooth discovery
            mBluetoothLEScanner.startScan(60000);

            // Change discovering view
            mScanForDevices.setText(R.string.fdb_title_bl_searching);
            mScanForDevices.setTextColor(getResources().getColor( android.R.color.tab_indicator_text ));
            mScanForDevices.setOnClickListener(null);

            // After 15 seconds disable discovery
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cancelScanning();
                }
            }, 15000);
        }
    }

    /**
     * Cancel bluetooth discovery and change button to enable next search
     */
    private void cancelScanning() {
        if( mBluetoothLEScanner.isScanning() ) {
            mBluetoothLEScanner.cancelScan();
        }

        mScanForDevices.setText(R.string.fdb_title_bl_search);
        mScanForDevices.setTextColor(getResources().getColor( R.color.colorTextPrimaryDark ));
        mScanForDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
            }
        });
    }
}
