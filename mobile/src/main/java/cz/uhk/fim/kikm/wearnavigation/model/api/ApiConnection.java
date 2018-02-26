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

public interface ApiConnection {

    @GET("fingerprints")
    Call<List<Fingerprint>> getFingerprints(@Header("deviceId") String deviceId,
                                            @Query("timestamp") long timestamp,
                                            @Query("limit") int limit,
                                            @Query("offset") long offset);

    @GET("fingerprints")
    Call<List<Fingerprint>> getFingerprintsByLocation(@Header("deviceId") String deviceId,
                                            @Query("timestamp") long timestamp,
                                            @Query("limit") int limit,
                                            @Query("offset") long offset,
                                            @Body LocationEntry locationEntry);

    @GET("fingerprints-meta")
    Call<FingerprintMeta> getFingerprintsMeta(@Header("deviceId") String deviceId,
                                              @Query("timestamp") long timestamp);

    @POST("fingerprints")
    Call<Void> postFingerprints(@Header("deviceId") String deviceId,
                                @Body List<Fingerprint> fingerprints);
}
