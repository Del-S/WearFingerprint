package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import org.altbeacon.beacon.Beacon;

import java.util.Collection;

/**
 * Interface for communication with Bluetooth LE FingerprintScanner.
 */
public interface BluetoothLEScannerInterface {
    /**
     * Service connected is called when Beacon scanner was bound.
     * At this point Beacon scanning can be triggered (not sooner).
     */
    void serviceConnected();

    /**
     * If the scanner found a single beacon this function will be triggered.
     * The function is just to make it easy or find only once specific beacon.
     * Caution: This function is called from service thread and not the main one.
     *
     * @param beacon that was found via scanner
     */
    void foundBeacon(Beacon beacon);

    /**
     * Multiple beacons found triggered when in one scan cycle multiple beacons
     * were found.
     * Caution: This function is called from service thread and not the main one.
     *
     * @param beacons that were found
     */
    void foundMultipleBeacons(Collection<Beacon> beacons);
}
