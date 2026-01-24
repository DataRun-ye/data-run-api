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

@Component
@RequiredArgsConstructor
public class LastSeenFilter extends OncePerRequestFilter {
    private final LastSeenService lastSeenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String userId = getUserIdFromPrincipal(auth.getPrincipal());
                if (userId != null) lastSeenService.touch(userId, Instant.now());
            }
        } catch (Exception e) {
            // must not break request
        }
        filterChain.doFilter(request, response);
    }

    private String getUserIdFromPrincipal(Object principal) {
        if (principal instanceof CurrentUserDetails p) return p.getId();
        return null;
    }
}
