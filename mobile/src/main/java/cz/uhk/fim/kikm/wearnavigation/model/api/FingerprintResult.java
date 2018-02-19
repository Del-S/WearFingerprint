package cz.uhk.fim.kikm.wearnavigation.model.api;

import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiException;

/**
 * Interface that enables to communicate with FingerprintAPI.
 * Has all necessary functions to inform jobs and activities.
 */
public interface FingerprintResult {

    /**
     * Called after an amount of fingerprints is loaded and
     * parsed by the Database. Count number returned is how
     * many of fingerprints were added successfully.
     *
     * @param count fingerprints added
     */
    void loadedFingerprints(int count);

    /**
     * Informs that fingerprints were added successfully.
     */
    void postedFingerprints();

    /**
     * Called when loading of fingerprint meta data is complete.
     * This data is used to determine how many fingerprints to
     * download and upload.
     *
     * @param fingerprintMeta class that was loaded
     */
    void loadedFingerprintMeta(FingerprintMeta fingerprintMeta);

    /**
     * Called when exception was thrown.
     *
     * @param ex exception to return.
     */
    void apiException(ApiException ex);
}