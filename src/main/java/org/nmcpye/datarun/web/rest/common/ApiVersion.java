package org.nmcpye.datarun.web.rest.common;

/**
 * @author Hamza Assada 27/04/2025 (7amza.it@gmail.com)
 */
public final class ApiVersion {
    /**
     * Deprecated compatibility alias for operational clients that have not
     * moved to the versioned API.
     */
    @Deprecated(forRemoval = true)
    public static final String API_CUSTOM = "/api" + "/custom";
    public static final String API_V1 = "/api" + "/v1";

}
