package cz.uhk.fim.kikm.wearnavigation.model.database.helpers;

public interface DatabaseDataInterface<T> {

    /**
     * Returning all the fingerprints from the Task to the Context.
     *
     * @param result List<Fingerprint> as T (cast it needed)
     */
    void allFingerprintsLoaded(T result);

    /**
     * There was an error loading data.
     */
    void loadError();
}
