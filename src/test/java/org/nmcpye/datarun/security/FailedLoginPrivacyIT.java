package org.nmcpye.datarun.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A failed login, through the login route: no log line names the login it tried, a personal identifier (here phone
 * numbers), for an account that exists and for one that does not.
 * <p>
 * A context of its own (the property only makes its key differ): the login reads the user caches, and when an
 * earlier test class with {@code @DirtiesContext} (ErrorReportingOnIT) closes its context, it closes the JCache
 * manager that the shared context's caches belong to as well.
 */
@AutoConfigureMockMvc
@IntegrationTest
@TestPropertySource(properties = "datarun.test.context=failed-login")
class FailedLoginPrivacyIT {

    private static final String ACCOUNT_LOGIN = "7770001112";
    private static final String UNKNOWN_LOGIN = "7770001113";
    private static final String ACCOUNT_ID = "it-failed-login-user";
    /** A bcrypt hash of another password than the one sent. */
    private static final String PASSWORD_HASH = "$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    /** The pool does not auto-commit, so the account's rows are written in transactions. */
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CacheManager cacheManager;

    private final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void anAccount() {
        removeTheAccount();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("""
                INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
                VALUES (?, 'itFailLogn1', ?, ?, true, 'en', 'it-test')""", ACCOUNT_ID, ACCOUNT_LOGIN, PASSWORD_HASH);
            jdbc.update("INSERT INTO app_user_authority (user_id, authority_name) VALUES (?, 'ROLE_USER')", ACCOUNT_ID);
        });
        logs.start();
        root.addAppender(logs);
    }

    @AfterEach
    void removeTheAccount() {
        root.detachAppender(logs);
        logs.stop();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM app_user_authority WHERE user_id = ?", ACCOUNT_ID);
            jdbc.update("DELETE FROM app_user WHERE id = ?", ACCOUNT_ID);
        });
        for (String cache : List.of(UserRepository.USERS_BY_LOGIN_CACHE, UserRepository.USERS_BY_EMAIL_CACHE)) {
            Cache users = cacheManager.getCache(cache);
            if (users != null) {
                users.evict(ACCOUNT_LOGIN);
                users.evict(UNKNOWN_LOGIN);
            }
        }
    }

    @Test
    void aFailedLoginLeavesNoLoginInTheLogs() throws Exception {
        String accountRequest = failLogin(ACCOUNT_LOGIN);
        String unknownRequest = failLogin(UNKNOWN_LOGIN);

        assertNothingLogged(ACCOUNT_LOGIN, UNKNOWN_LOGIN);
        assertThat(lines()).anySatisfy(line -> assertThat(line)
            .matches("Login failed \\(BadCredentialsException\\): request " + accountRequest
                + ", login tag [0-9a-f]{8}, account " + ACCOUNT_ID));
        assertThat(lines()).anySatisfy(line -> assertThat(line)
            .matches("Login failed \\(BadCredentialsException\\): request " + unknownRequest
                + ", login tag [0-9a-f]{8}, account unknown"));
    }

    /** Sends a login with a wrong password and returns the request id the answer carries. */
    private String failLogin(String login) throws Exception {
        return mvc.perform(post("/api/v1/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + login + "\", \"password\": \"wrong-password-9\"}"))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    }

    private List<String> lines() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private void assertNothingLogged(String... logins) {
        assertThat(logs.list).as("log events").isNotEmpty();
        for (ILoggingEvent event : logs.list) {
            List<String> texts = new ArrayList<>(List.of(event.getFormattedMessage()));
            for (IThrowableProxy part = event.getThrowableProxy(); part != null; part = part.getCause()) {
                texts.add(String.valueOf(part.getMessage()));
            }
            for (String text : texts) {
                assertThat(text).as("a line of %s", event.getLoggerName()).doesNotContain(logins);
            }
        }
    }
}
