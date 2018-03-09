package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;

public interface DatabaseDataInterface {

    /**
     * Returning all the fingerprints from the Task to the Context.
     *
     * @param result List<Fingerprint> as T (cast it needed)
     */
    void allFingerprintsLoaded(List<Fingerprint> result);

    /**
     * There was an error loading data.
     */
    void loadError();
}
