package cz.uhk.fim.kikm.wearnavigation.model.api.auth;

import java.util.Map;
import java.util.List;

import cz.uhk.fim.kikm.wearnavigation.model.api.utils.Pair;

public interface Authentication {
    /**
     * Apply authentication settings to header and query params.
     *
     * @param queryParams List of query parameters
     * @param headerParams Map of header parameters
     */
    void applyToParams(List<Pair> queryParams, Map<String, String> headerParams);
}
