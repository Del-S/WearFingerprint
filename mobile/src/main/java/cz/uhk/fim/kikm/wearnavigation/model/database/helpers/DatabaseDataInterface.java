package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;

/**
 * Interface between AsyncTask that is loading fingerprints and
 * class that called the load.
 */
public interface DatabaseDataInterface {

    /**
     * Fingerprint positions were loaded.
     *
     * @param result List of Fingerprints
     */
    void loadedFingerprintPositions(List<Fingerprint> result);

    /**
     * There was an error loading data.
     */
    void loadError();
}
