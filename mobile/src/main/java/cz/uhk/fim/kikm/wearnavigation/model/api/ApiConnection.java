package cz.uhk.fim.kikm.wearnavigation.model.api;

import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.LocationEntry;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Class that connects to the beacon server and downloads/uploads fingerprint.
 * Using retrofit instead of Swagger. Server swagger documentation is here:
 * https://app.swaggerhub.com/apis/Del-S/FingerprintAPI/
 */
public interface ApiConnection {

    /**
     * Get all fingerprints from the api. Can use parameters to filter out
     * data from the server. Downloading all fingerprint may take a lot
     * of data and time so it's better to split the data up by limit and offset.
     * Ideal number for limit is around 50 - 100.
     *
     * @param deviceId id of this devices used for authorization
     * @param timestamp to filter out old fingerprints
     * @param limit of fingerprints downloaded
     * @param offset to move the limited data
     * @return List of fingerprints
     */
    @GET("fingerprints")
    Call<List<Fingerprint>> getFingerprints(@Header("deviceId") String deviceId,
                                            @Query("timestamp") long timestamp,
                                            @Query("limit") int limit,
                                            @Query("offset") long offset);

    /**
     * Get all fingerprints from the api. Can use parameters to filter out
     * data from the server. Downloading all fingerprint may take a lot
     * of data and time so it's better to split the data up by limit and offset.
     * Ideal number for limit is around 50 - 100.
     *
     * Note: this function can also filter based on specific location.
     *
     * @param deviceId id of this devices used for authorization
     * @param timestamp to filter out old fingerprints
     * @param limit of fingerprints downloaded
     * @param offset to move the limited data
     * @param locationEntry to load fingerprints for.
     * @return List of fingerprints
     */
    @GET("fingerprints")
    Call<List<Fingerprint>> getFingerprintsByLocation(@Header("deviceId") String deviceId,
                                            @Query("timestamp") long timestamp,
                                            @Query("limit") int limit,
                                            @Query("offset") long offset,
                                            @Body LocationEntry locationEntry);

    /**
     * Get fingerprint meta data from the api. This will calculate
     * how many fingerprints should be downloaded and also load
     * timestamp of last added fingerprint by this device.
     *
     * @param deviceId id of this devices used for authorization
     *                 and to get lastInsert timestamp
     * @param timestamp to filter out old fingerprints
     * @return calculated meta data
     */
    @GET("fingerprints-meta")
    Call<FingerprintMeta> getFingerprintsMeta(@Header("deviceId") String deviceId,
                                              @Query("timestamp") long timestamp);

    /**
     * Send fingerprints into the api. This feature uses Couchbase Synch
     * Gateway so fingerprint can be also loaded via that.
     * Gateway link: http://beacon.uhk.cz/fingerprintgw/
     *
     * @param deviceId id of this devices used for authorization
     * @param fingerprints to upload into the api
     * @return nothing is returned
     */
    @POST("fingerprints")
    Call<Void> postFingerprints(@Header("deviceId") String deviceId,
                                @Body List<Fingerprint> fingerprints);
}
