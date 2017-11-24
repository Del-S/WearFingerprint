package kikm.fim.uhk.cz.wearnavigationsimple.model.adapters;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.view.LayoutInflater;
import android.view.View;

import org.altbeacon.beacon.Beacon;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import kikm.fim.uhk.cz.wearnavigationsimple.R;

import static org.junit.Assert.*;

public class BleBeaconsAdapterTest {
    private BleBeaconsAdapter adapter;
    private View mockView;
    private static Context instrumentationCtx;

    @Before
    public void setUp() throws Exception {
        instrumentationCtx = InstrumentationRegistry.getContext();

        adapter = new BleBeaconsAdapter(instrumentationCtx);

        mockView = mock(View.class);
    }

    @Test
    public void itemCount() {
        Beacon beacon1 = new Beacon.Builder()
                .setId1("b9407f30-f5f8-466e-aff9-25556b57fe6d")
                .build();

        Beacon beacon2 = new Beacon.Builder()
                .setId1("25556b57fe6d")
                .build();

        Beacon beacon3 = new Beacon.Builder()
                .setId1("b9407f30")
                .build();

        List<Beacon> beaconList = Arrays.asList(beacon1, beacon2);

        adapter.addBeacon(beacon3);
        adapter.addAllBeacons(beaconList);

        assertEquals("Adapter item count is not equal: ", adapter.getItemCount(), 3);
    }

    @Test
    public void addSameBeacons() {
        Beacon beacon = new Beacon.Builder().build();
        adapter.addBeacon(beacon);
        adapter.addBeacon(beacon);

        assertEquals("Added same beacon (single) count is not equal: ", adapter.getItemCount(), 1);

        List<Beacon> beaconList = Arrays.asList(beacon, beacon);
        adapter.addAllBeacons(beaconList);

        assertEquals("Added same beacon (multiple) count is not equal: ", adapter.getItemCount(), 1);
    }

}