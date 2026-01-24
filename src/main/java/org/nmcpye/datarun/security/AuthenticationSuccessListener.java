package org.nmcpye.datarun.security;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.user.service.LastSeenService;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
    private final LastSeenService lastSeenService;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String userId = extractUserId(principal); // adapt to your principal
        lastSeenService.touch(userId, Instant.now());
    }

    private String extractUserId(Object principal) {
        // adapt: if principal is a UserDetails with id, etc.
        // example:
        if (principal instanceof CurrentUserDetails p) return p.getId();
        return null;
    }
}
