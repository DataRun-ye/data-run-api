package org.nmcpye.datarun.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationFailureListenerTest {

    private static final Pattern TAG = Pattern.compile("login tag ([0-9a-f]{8}),");
    private static final String REQUEST_ID = "6f1c0b0e-0000-4000-8000-000000000001";

    private final ConcurrentMapCacheManager caches = new ConcurrentMapCacheManager(
        UserRepository.USERS_BY_LOGIN_CACHE, UserRepository.USERS_BY_EMAIL_CACHE);

    @AfterEach
    void clearRequestId() {
        MDC.clear();
    }

    /**
     * A failed login names its outcome, the request id, a tag that repeats with the login, and the account's id when
     * the login's lookup cached the account; never the login, a personal identifier (here a phone number).
     */
    @Test
    void aFailedLoginIsLoggedWithoutTheLogin() {
        User account = new User();
        account.setId("01JUSER0000000000000000003");
        caches.getCache(UserRepository.USERS_BY_LOGIN_CACHE).put("7770001112", account);
        caches.getCache(UserRepository.USERS_BY_LOGIN_CACHE).put("7770001113", null); // looked up, no such account
        MDC.put(RequestIdFilter.MDC_REQUEST_ID_KEY, REQUEST_ID);
        AuthenticationFailureListener listener = new AuthenticationFailureListener(caches);

        List<ILoggingEvent> lines = logged(() -> {
            listener.onApplicationEvent(failure("7770001112"));
            listener.onApplicationEvent(failure("7770001113"));
            listener.onApplicationEvent(failure("7770001112"));
        });

        assertThat(lines).hasSize(3).allSatisfy(line -> {
            assertThat(line.getLevel()).isEqualTo(Level.WARN);
            assertThat(line.getFormattedMessage())
                .startsWith("Login failed (BadCredentialsException): request " + REQUEST_ID + ", login tag ")
                .doesNotContain("7770001112", "7770001113");
        });
        assertThat(lines.get(0).getFormattedMessage()).endsWith(", account 01JUSER0000000000000000003");
        assertThat(lines.get(1).getFormattedMessage()).endsWith(", account unknown");
        assertThat(tag(lines.get(0))).as("the same login, the same tag").isEqualTo(tag(lines.get(2)))
            .isNotEqualTo(tag(lines.get(1)));
    }

    /** Each run draws its own key, so a tag is comparable within one run and means nothing outside it. */
    @Test
    void eachRunTagsALoginDifferently() {
        AuthenticationFailureListener run = new AuthenticationFailureListener(caches);
        AuthenticationFailureListener nextRun = new AuthenticationFailureListener(caches);

        assertThat(run.tag("7770001112")).isEqualTo(run.tag("7770001112")).isNotEqualTo(nextRun.tag("7770001112"));
        assertThat(run.tag("Admin")).as("logins match without regard to case").isEqualTo(run.tag("admin"));
    }

    /** A cache that cannot answer (closed, failing) leaves the account unknown; the failure is still logged. */
    @Test
    void aCacheThatCannotAnswerLeavesTheAccountUnknown() {
        CacheManager broken = mock(CacheManager.class);
        when(broken.getCache(any())).thenThrow(new IllegalStateException("Cache[usersByLogin] is closed"));

        List<ILoggingEvent> lines = logged(() ->
            new AuthenticationFailureListener(broken).onApplicationEvent(failure("7770001112")));

        assertThat(lines).singleElement().satisfies(line ->
            assertThat(line.getFormattedMessage()).endsWith(", account unknown").doesNotContain("7770001112"));
    }

    /** An email login is looked up as typed, in its own cache. */
    @Test
    void anEmailLoginFindsItsAccountInTheEmailCache() {
        User account = new User();
        account.setId("01JUSER0000000000000000004");
        caches.getCache(UserRepository.USERS_BY_EMAIL_CACHE).put("Someone@Example.org", account);

        assertThat(new AuthenticationFailureListener(caches).cachedAccountId("Someone@Example.org"))
            .contains("01JUSER0000000000000000004");
        assertThat(new AuthenticationFailureListener(caches).cachedAccountId("nobody@example.org")).isEmpty();
    }

    private static AuthenticationFailureBadCredentialsEvent failure(String login) {
        return new AuthenticationFailureBadCredentialsEvent(
            new UsernamePasswordAuthenticationToken(login, "wrong-password"), new BadCredentialsException("Bad credentials"));
    }

    private static String tag(ILoggingEvent line) {
        Matcher tag = TAG.matcher(line.getFormattedMessage());
        assertThat(tag.find()).as(line.getFormattedMessage()).isTrue();
        return tag.group(1);
    }

    private static List<ILoggingEvent> logged(Runnable work) {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthenticationFailureListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            work.run();
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list;
    }
}
