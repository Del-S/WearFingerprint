package cz.uhk.fim.kikm.wearnavigation.model.api;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiCallback;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiClient;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiException;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiResponse;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ApiConfiguration;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.Pair;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ProgressRequestBody;
import cz.uhk.fim.kikm.wearnavigation.model.api.utils.ProgressResponseBody;
import cz.uhk.fim.kikm.wearnavigation.model.database.Fingerprint;
import cz.uhk.fim.kikm.wearnavigation.model.database.LocationEntry;

class MobileUsersApi {
    private ApiClient apiClient;

    public MobileUsersApi() {
        this(ApiConfiguration.getDefaultApiClient());
    }

    public MobileUsersApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for addFingerprint
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param fingerprints Fingerprints to add into the database. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call addFingerprintsCall(String deviceId, List<Fingerprint> fingerprints, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = fingerprints;

        // create path and map variables
        String localVarPath = "/fingerprints";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        if (deviceId != null)
        localVarHeaderParams.put("deviceId", apiClient.parameterToString(deviceId));

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call addFingerprintsValidateBeforeCall(String deviceId, List<Fingerprint> fingerprints, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'deviceId' is set
        if (deviceId == null) {
            throw new ApiException("Missing the required parameter 'deviceId' when calling addFingerprint(Async)");
        }
        

        com.squareup.okhttp.Call call = addFingerprintsCall(deviceId, fingerprints, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Adds fingerprint entry.
     * Adds fingerprint into the database from mobile device. Can handle multiple fingerprints send as JSONArray.
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param fingerprints Fingerprints to add into the database. (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public void addFingerprints(String deviceId, List<Fingerprint> fingerprints) throws ApiException {
        addFingerprintsWithHttpInfo(deviceId, fingerprints);
    }

    /**
     * Adds fingerprint entry.
     * Adds fingerprint into the database from mobile device. Can handle multiple fingerprints send as JSONArray.
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param fingerprints Fingerprints to add into the database. (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Void> addFingerprintsWithHttpInfo(String deviceId, List<Fingerprint> fingerprints) throws ApiException {
        com.squareup.okhttp.Call call = addFingerprintsValidateBeforeCall(deviceId, fingerprints, null, null);
        return apiClient.execute(call);
    }

    /**
     * Adds fingerprint entry. (asynchronously)
     * Adds fingerprint into the database from mobile device. Can handle multiple fingerprints send as JSONArray.
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param fingerprints Fingerprints to add into the database. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call addFingerprintsAsync(String deviceId, List<Fingerprint> fingerprints, final ApiCallback<Void> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = addFingerprintsValidateBeforeCall(deviceId, fingerprints, progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }
    /**
     * Build call for getFingerprintMeta
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. It is Time of last update. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getFingerprintMetaCall(String deviceId, Long timestamp, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/fingerprints-meta";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (timestamp != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("timestamp", timestamp));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        if (deviceId != null)
        localVarHeaderParams.put("deviceId", apiClient.parameterToString(deviceId));

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getFingerprintMetaValidateBeforeCall(String deviceId, Long timestamp, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'deviceId' is set
        if (deviceId == null) {
            throw new ApiException("Missing the required parameter 'deviceId' when calling getFingerprintMeta(Async)");
        }
        
        // verify the required parameter 'timestamp' is set
        if (timestamp == null) {
            throw new ApiException("Missing the required parameter 'timestamp' when calling getFingerprintMeta(Async)");
        }
        

        com.squareup.okhttp.Call call = getFingerprintMetaCall(deviceId, timestamp, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Fingerprint meta information.
     * Gets meta information about fingerprints. Two parts of data are count of new fingerprints and last update time based on device id. 
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. It is Time of last update. (required)
     * @return FingerprintMeta
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public FingerprintMeta getFingerprintMeta(String deviceId, Long timestamp) throws ApiException {
        ApiResponse<FingerprintMeta> resp = getFingerprintMetaWithHttpInfo(deviceId, timestamp);
        return resp.getData();
    }

    /**
     * Fingerprint meta information.
     * Gets meta information about fingerprints. Two parts of data are count of new fingerprints and last update time based on device id. 
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. It is Time of last update. (required)
     * @return ApiResponse&lt;FingerprintMeta&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<FingerprintMeta> getFingerprintMetaWithHttpInfo(String deviceId, Long timestamp) throws ApiException {
        com.squareup.okhttp.Call call = getFingerprintMetaValidateBeforeCall(deviceId, timestamp, null, null);
        Type localVarReturnType = new TypeToken<FingerprintMeta>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Fingerprint meta information. (asynchronously)
     * Gets meta information about fingerprints. Two parts of data are count of new fingerprints and last update time based on device id. 
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. It is Time of last update. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getFingerprintMetaAsync(String deviceId, Long timestamp, final ApiCallback<FingerprintMeta> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getFingerprintMetaValidateBeforeCall(deviceId, timestamp, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<FingerprintMeta>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getFingerprints
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. (optional)
     * @param limit Limit for database query used to limit amout of data loaded and enable support for multiple calls to this API. (optional)
     * @param offset Offset used with the limit to load different data within the specify limit. (optional)
     * @param location Filter fingerprints by location. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getFingerprintsCall(String deviceId, Long timestamp, Integer limit, Integer offset, LocationEntry location, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = location;

        // create path and map variables
        String localVarPath = "/fingerprints";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (timestamp != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("timestamp", timestamp));
        if (limit != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("limit", limit));
        if (offset != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("offset", offset));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        if (deviceId != null)
        localVarHeaderParams.put("deviceId", apiClient.parameterToString(deviceId));

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getFingerprintsValidateBeforeCall(String deviceId, Long timestamp, Integer limit, Integer offset, LocationEntry location, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'deviceId' is set
        if (deviceId == null) {
            throw new ApiException("Missing the required parameter 'deviceId' when calling getFingerprints(Async)");
        }
        

        com.squareup.okhttp.Call call = getFingerprintsCall(deviceId, timestamp, limit, offset, location, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get fingerprints
     * Loads fingerprints from the beacon server. Can filter fingerprints by specific timestamp (ms) and location. Everything with higher timestamp will be displayed. 
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. (optional)
     * @param limit Limit for database query used to limit amout of data loaded and enable support for multiple calls to this API. (optional)
     * @param offset Offset used with the limit to load different data within the specify limit. (optional)
     * @param location Filter fingerprints by location. (optional)
     * @return Fingerprint
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public List<Fingerprint> getFingerprints(String deviceId, Long timestamp, Integer limit, Integer offset, LocationEntry location) throws ApiException {
        ApiResponse<List<Fingerprint>> resp = getFingerprintsWithHttpInfo(deviceId, timestamp, limit, offset, location);
        return resp.getData();
    }

    /**
     * Get fingerprints
     * Loads fingerprints from the beacon server. Can filter fingerprints by specific timestamp (ms) and location. Everything with higher timestamp will be displayed. 
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. (optional)
     * @param limit Limit for database query used to limit amout of data loaded and enable support for multiple calls to this API. (optional)
     * @param offset Offset used with the limit to load different data within the specify limit. (optional)
     * @param location Filter fingerprints by location. (optional)
     * @return ApiResponse&lt;Fingerprint&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<List<Fingerprint>> getFingerprintsWithHttpInfo(String deviceId, Long timestamp, Integer limit, Integer offset, LocationEntry location) throws ApiException {
        com.squareup.okhttp.Call call = getFingerprintsValidateBeforeCall(deviceId, timestamp, limit, offset, location, null, null);
        Type localVarReturnType = new TypeToken<List<Fingerprint>>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get fingerprints (asynchronously)
     * Loads fingerprints from the beacon server. Can filter fingerprints by specific timestamp (ms) and location. Everything with higher timestamp will be displayed. 
     * @param deviceId Id of specific device that run this query. Can be used to block the device. (required)
     * @param timestamp Timestamp to filter out fingerprints. (optional)
     * @param limit Limit for database query used to limit amout of data loaded and enable support for multiple calls to this API. (optional)
     * @param offset Offset used with the limit to load different data within the specify limit. (optional)
     * @param location Filter fingerprints by location. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getFingerprintsAsync(String deviceId, Long timestamp, Integer limit, Integer offset, LocationEntry location, final ApiCallback<Fingerprint> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getFingerprintsValidateBeforeCall(deviceId, timestamp, limit, offset, location, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<List<Fingerprint>>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
