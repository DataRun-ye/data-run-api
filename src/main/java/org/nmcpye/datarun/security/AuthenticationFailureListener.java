package org.nmcpye.datarun.security;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Logs a failed username-and-password login without the login it tried: a login is a personal identifier, often a
 * phone number. The line
 * names the outcome, the request id, a short tag of the attempted login, and the account's durable id when the
 * account exists and the user cache already holds it. The login's own lookup has just filled that cache, so reading
 * it makes no query.
 * <p>
 * The tag is an HMAC of the login under a key this process draws when it starts and never stores. The same login
 * gets the same tag until the next restart, so repeated failures stand out, and a tag cannot be turned back into the
 * login, even for logins as guessable as phone numbers.
 */
@Component
@Slf4j
public class AuthenticationFailureListener
    implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private static final String TAG_ALGORITHM = "HmacSHA256";
    private static final int TAG_BYTES = 4;

    private final CacheManager cacheManager;
    private final SecretKeySpec tagKey;

    public AuthenticationFailureListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        this.tagKey = new SecretKeySpec(key, TAG_ALGORITHM);
    }

    /**
     * Only a username-and-password login is logged. Spring Security also reports a rejected bearer token as bad
     * credentials (an InvalidBearerTokenException, with the raw token as the principal): such a request logs nothing,
     * and the invalid-token meter ({@code security.authentication.invalid-tokens}) counts it by cause.
     */
    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        if (!(event.getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
            return;
        }
        Object principal = event.getAuthentication().getPrincipal();
        String attemptedUsername = principal instanceof String ? (String) principal : String.valueOf(principal);
        String accountId = cachedAccountId(attemptedUsername).orElse(null);

        // The failed login's only record: ExceptionTranslator logs nothing else for it.
        log.warn("Login failed ({}): request {}, login tag {}, account {}",
            event.getException().getClass().getSimpleName(), MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY),
            tag(attemptedUsername), accountId != null ? accountId : "unknown");
    }

    /** Eight hex digits of the login's HMAC; logins are matched without regard to case, so the tag is too. */
    String tag(String login) {
        try {
            Mac mac = Mac.getInstance(TAG_ALGORITHM);
            mac.init(tagKey);
            byte[] digest = mac.doFinal(login.toLowerCase(Locale.ENGLISH).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, TAG_BYTES);
        } catch (GeneralSecurityException e) {
            return "none";
        }
    }

    /**
     * The account's id if the user cache holds it: {@code DomainUserDetailsService} looks a login up by its lower case,
     * or an email address as typed, and both lookups are cached. Never a query.
     */
    Optional<String> cachedAccountId(String login) {
        return cachedUser(UserRepository.USERS_BY_LOGIN_CACHE, login.toLowerCase(Locale.ENGLISH))
            .or(() -> cachedUser(UserRepository.USERS_BY_EMAIL_CACHE, login))
            .map(User::getId);
    }

    /** Never throws: a cache that cannot answer leaves the account unknown, and the login's 401 as it is. */
    private Optional<User> cachedUser(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            Cache.ValueWrapper hit = cache == null ? null : cache.get(key);
            return hit != null && hit.get() instanceof User user ? Optional.of(user) : Optional.empty();
        } catch (RuntimeException unavailable) {
            return Optional.empty();
        }
    }
}
