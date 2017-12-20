package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import org.altbeacon.beacon.Beacon;

import java.util.Collection;

/**
 * Interface for communication with Bluetooth LE FingerprintScanner.
 */
public interface BLEScannerInterface {
    /**
     * Service connected is called when Beacon scanner was bound.
     * At this point Beacon scanning can be triggered (not sooner).
     */
    void serviceConnected();

    /**
     * Multiple beacons found triggered when in one scan cycle multiple beacons
     * were found.
     * Caution: This function is called from service thread and not the main one.
     *
     * @param beacons that were found
     */
    void foundBeacons(Collection<Beacon> beacons);
}
