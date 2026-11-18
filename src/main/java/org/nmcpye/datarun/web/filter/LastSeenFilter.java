package org.nmcpye.datarun.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.user.service.LastSeenService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class LastSeenFilter extends OncePerRequestFilter {

    static final String APP_VERSION_HEADER = "X-App-Version";
    private static final String ASSIGNMENTS_SYNC_PATH = "/assignments/forms";
    private static final Pattern APP_VERSION = Pattern.compile("[0-9A-Za-z.+_-]{1,32}");

    private final LastSeenService lastSeenService;

    // Skip static resources to save processing
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
        ".css", ".js", ".png", ".jpg", ".gif", ".ico", ".svg", ".woff", ".woff2", ".ttf"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().toLowerCase();
        // Ignore static files
        if (path.contains(".")) {
            String ext = path.substring(path.lastIndexOf('.'));
            if (IGNORED_EXTENSIONS.contains(ext)) return true;
        }
        // Ignore actuator endpoints or heartbeats
        if (path.startsWith("/management") || path.startsWith("/actuator")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // Check if authenticated (and not anonymous)
            if (auth != null && auth.isAuthenticated() &&
                auth.getPrincipal() instanceof CurrentUserDetails userDetails) {

                // No DB call here, just a fast map put
                lastSeenService.touch(
                    userDetails.getId(),
                    Instant.now(),
                    appVersion(request),
                    referenceVersion(request));
            }
        } catch (Exception e) {
            // Fail silently so user experience isn't affected by logging logic
        }

        chain.doFilter(request, response);
    }

    /** The app version a phone reports (app 6.1.0+), if well formed. */
    static String appVersion(HttpServletRequest request) {
        String value = request.getHeader(APP_VERSION_HEADER);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return APP_VERSION.matcher(trimmed).matches() ? trimmed : null;
    }

    /**
     * On the app's assignments sync, the {@code referenceVersion} it asked for
     * (0 when absent: apps before 6.0.2); null for every other request.
     */
    static Integer referenceVersion(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.toLowerCase(Locale.ROOT).endsWith(ASSIGNMENTS_SYNC_PATH)) {
            return null;
        }
        String value = request.getParameter("referenceVersion");
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
