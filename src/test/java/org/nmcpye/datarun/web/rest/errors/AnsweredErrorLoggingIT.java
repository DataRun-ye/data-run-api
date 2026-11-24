package org.nmcpye.datarun.web.rest.errors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * What a request error that the REST layer answers leaves behind since 6.5.5: one log line, and no error_event row.
 * A server fault (5xx) is an ERROR line that reaches GlitchTip as its pattern; a client error (4xx) an INFO line.
 * Both name the method, route pattern, status, error code, exception types and request id, never an exception's
 * message, which quotes database rows and submitted values.
 */
@WithMockUser
@AutoConfigureMockMvc
@IntegrationTest
class AnsweredErrorLoggingIT {

    private static final String REQUEST_ID = ", request [0-9a-f-]{36}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SentryOptions.BeforeSendCallback errorReportingWithoutValues;

    private final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureEveryLogger() {
        logs.start();
        root.addAppender(logs);
    }

    @AfterEach
    void stopCapturing() {
        root.detachAppender(logs);
        logs.stop();
    }

    @Test
    void aServerFaultIsOneErrorLineThatReachesGlitchTipAsItsPattern() throws Exception {
        List<ILoggingEvent> lines = send(get("/api/exception-translator-test/database-failure"), 500);

        ILoggingEvent fault = answered(lines);
        assertThat(fault.getLevel()).isEqualTo(Level.ERROR);
        assertThat(fault.getFormattedMessage()).matches("Request GET /api/exception-translator-test/database-failure "
            + "failed with 500 \\(error\\.http\\.500\\): DataIntegrityViolationException < PSQLException, "
            + "SQLState 23505, at ExceptionTranslatorTestController\\.databaseFailure:\\d+( < \\w+\\.\\w+:\\d+)*"
            + REQUEST_ID);
        assertThat(fault.getThrowableProxy()).as("no exception, whose message quotes the row").isNull();
        assertThat(errorReportingWithoutValues.execute(asSentryEvent(fault), new Hint()).getMessage().getFormatted())
            .as("what GlitchTip gets").isEqualTo("Request {} {} failed with {} ({}): {}, request {}");
        assertNothingLogged(lines, "secret-sql-7", "secret-detail-7");
    }

    @Test
    void aClientErrorIsOneInfoLineWithoutTheRejectedValue() throws Exception {
        List<ILoggingEvent> lines = send(post("/api/exception-translator-test/method-argument-size")
            .contentType(MediaType.APPLICATION_JSON).content("{\"code\": \"secret-rejected-7\"}"), 400);

        ILoggingEvent rejected = answered(lines);
        assertThat(rejected.getLevel()).isEqualTo(Level.INFO);
        assertThat(rejected.getFormattedMessage()).matches("Request POST "
            + "/api/exception-translator-test/method-argument-size answered 400 \\(error\\.validation\\): "
            + "MethodArgumentNotValidException" + REQUEST_ID);
        assertNothingLogged(lines, "secret-rejected-7");
    }

    /**
     * A request without a valid token is answered 401 by Spring Security's entry point before any controller, so
     * ExceptionTranslator logs nothing for it: phones' connection checks (HEAD /api/authenticate, about 1,170 a
     * day on production, and only GET is open there) stay out of the logs.
     */
    @Test
    @WithAnonymousUser
    void aRequestWithoutAValidTokenNeverReachesTheTranslator() throws Exception {
        List<ILoggingEvent> lines = send(head("/api/authenticate"), 401);
        lines.addAll(send(head("/api/authenticate").header("Authorization", "Bearer not-a-token"), 401));

        assertThat(lines).noneMatch(line -> line.getLoggerName().equals(ExceptionTranslator.class.getName()));
    }

    /** The table stays for 6.5.4, the rollback target, which still writes it; nothing here does. */
    @Test
    void noRequestErrorWritesErrorEvent() throws Exception {
        Integer before = jdbc.queryForObject("SELECT count(*) FROM error_event", Integer.class);

        send(get("/api/exception-translator-test/internal-server-error"), 500);
        send(get("/api/exception-translator-test/access-denied"), 403);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM error_event", Integer.class)).isEqualTo(before);
    }

    private List<ILoggingEvent> send(MockHttpServletRequestBuilder request, int status) throws Exception {
        int from = logs.list.size();
        assertThat(mockMvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(status);
        return new ArrayList<>(logs.list.subList(from, logs.list.size()));
    }

    /** ExceptionTranslator's one line for the request. */
    private static ILoggingEvent answered(List<ILoggingEvent> lines) {
        List<ILoggingEvent> own = lines.stream()
            .filter(line -> line.getLoggerName().equals(ExceptionTranslator.class.getName())).toList();
        assertThat(own).as("ExceptionTranslator's lines").hasSize(1);
        return own.get(0);
    }

    /** The event the logback appender of the Sentry SDK makes of a line. */
    private static SentryEvent asSentryEvent(ILoggingEvent line) {
        SentryEvent event = new SentryEvent();
        event.setLogger(line.getLoggerName());
        Message message = new Message();
        message.setMessage(line.getMessage());
        message.setFormatted(line.getFormattedMessage());
        event.setMessage(message);
        return event;
    }

    private static void assertNothingLogged(List<ILoggingEvent> lines, String... values) {
        for (ILoggingEvent event : lines) {
            List<String> texts = new ArrayList<>(List.of(event.getFormattedMessage()));
            for (IThrowableProxy part = event.getThrowableProxy(); part != null; part = part.getCause()) {
                texts.add(String.valueOf(part.getMessage()));
            }
            for (String text : texts) {
                assertThat(text).as("a line of %s", event.getLoggerName()).doesNotContain(values);
            }
        }
    }
}
