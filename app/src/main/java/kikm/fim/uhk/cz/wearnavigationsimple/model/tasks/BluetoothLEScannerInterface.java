package kikm.fim.uhk.cz.wearnavigationsimple.model.tasks;

import org.altbeacon.beacon.Beacon;

import java.util.Collection;

public interface BluetoothLEScannerInterface {
    void serviceConnected();
    void foundBeacon(Beacon beacon);
    void foundMultipleBeacons(Collection<Beacon> beacons);
}
