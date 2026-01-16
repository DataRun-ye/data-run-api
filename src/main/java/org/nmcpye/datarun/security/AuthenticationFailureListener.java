package org.nmcpye.datarun.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {
    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String username = principal instanceof String ? (String) principal : String.valueOf(principal);


        Object details = event.getAuthentication().getDetails();
        String ip = details instanceof WebAuthenticationDetails
            ? ((WebAuthenticationDetails) details).getRemoteAddress()
            : "unknown";


        // log the failure; do NOT log credentials
        log.warn("Authentication failure for user='{}' from ip='{}' - reason={}",
            username, ip, event.getException().getMessage());


        // Optionally: increment counters, emit metrics, or record to audit DB here
    }
}
