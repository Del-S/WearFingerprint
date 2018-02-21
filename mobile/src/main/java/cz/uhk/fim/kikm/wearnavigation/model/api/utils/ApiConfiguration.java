package cz.uhk.fim.kikm.wearnavigation.model.api.utils;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-02-11T15:15:00.746Z")
public class ApiConfiguration {
    private static ApiClient defaultApiClient = new ApiClient();

    /**
     * Get the default API client, which would be used when creating API
     * instances without providing an API client.
     *
     * @return Default API client
     */
    public static ApiClient getDefaultApiClient() {
        defaultApiClient.setConnectTimeout(60_000);
        defaultApiClient.setReadTimeout(60_000);
        defaultApiClient.setWriteTimeout(60_000);
        return defaultApiClient;
    }

    /**
     * Set the default API client, which would be used when creating API
     * instances without providing an API client.
     *
     * @param apiClient API client
     */
    public static void setDefaultApiClient(ApiClient apiClient) {
        defaultApiClient = apiClient;
    }
}
