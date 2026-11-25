package org.nmcpye.datarun.web.v2;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The v2 API's base path. The conventions themselves are described in
 * {@code docs/agent-context/api-v2.md}, and each lives in its own package below this one.
 * <p>
 * Nothing in v2 is shared with the v1 resources: v1's routes, payloads and error shapes stay as they are.
 */
public final class ApiV2 {

    /** Every v2 route starts here. */
    public static final String BASE = "/api/v2";

    private ApiV2() {
    }

    /** Whether the request addresses the v2 API: its path, without the context path, is the base or below it. */
    public static boolean isV2(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return path.equals(BASE) || path.startsWith(BASE + "/");
    }
}
