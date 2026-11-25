package org.nmcpye.datarun.web.v2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.RequestPath;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * The v2 API's base path. The conventions themselves are described in
 * {@code docs/agent-context/api-v2.md}, and each lives in its own package below this one.
 * <p>
 * Nothing in v2 is shared with the v1 resources: v1's routes, payloads and error shapes stay as they are.
 */
public final class ApiV2 {

    /** Every v2 route starts here. */
    public static final String BASE = "/api/v2";

    private static final PathPattern V2 = PathPatternParser.defaultInstance.parse(BASE + "/**");

    private ApiV2() {
    }

    /**
     * Whether the request addresses the v2 API, decided as Spring MVC routes it: on the path within the
     * application, its segments percent-decoded and stripped of path parameters, matched against
     * {@code /api/v2/**}. So {@code /api/%762/x} is a v2 request for the security chain, the problem resolver and
     * the handlers alike; deciding on the raw path would let it reach v2 handlers while v1's error handling
     * answered it.
     */
    public static boolean isV2(HttpServletRequest request) {
        RequestPath path = RequestPath.parse(request.getRequestURI(), request.getContextPath());
        return V2.matches(path.pathWithinApplication());
    }
}
