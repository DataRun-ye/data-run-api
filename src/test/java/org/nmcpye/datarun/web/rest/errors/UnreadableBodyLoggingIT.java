package org.nmcpye.datarun.web.rest.errors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.jpa.errorevent.service.ErrorEventService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An unreadable request body never reaches the logs: the exception's message quotes the body (a malformed token, a
 * value of the wrong type), so the one line logged names the route and the exception types only, and the answer
 * stays Spring's 400. Every logger's lines are read, at the levels production logs.
 */
@WithMockUser
@AutoConfigureMockMvc
@IntegrationTest
class UnreadableBodyLoggingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

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
    void aMalformedTokenIsLoggedByRouteAndTypeOnly() throws Exception {
        send("/api/exception-translator-test/method-argument", "{\"test\": secrettoken8}");

        assertNothingLogged("secrettoken8");
        assertThat(lines()).contains("WARN Unreadable request body on POST /api/exception-translator-test/method-argument: "
            + "HttpMessageNotReadableException < JsonParseException");
    }

    @Test
    void aValueOfTheWrongTypeIsLoggedByRouteAndTypeOnly() throws Exception {
        send("/api/exception-translator-test/typed-body", "{\"count\": \"secretvalue8\"}");

        assertNothingLogged("secretvalue8");
        assertThat(lines()).contains("WARN Unreadable request body on POST /api/exception-translator-test/typed-body: "
            + "HttpMessageNotReadableException < InvalidFormatException");
    }

    /**
     * When the error record cannot be queued, the handler still answers. Before 6.5.5 it failed, and Spring's
     * fallback resolver answered instead and logged {@code Resolved [... JSON parse error: Unrecognized token
     * 'secret...']}.
     */
    @Test
    void anErrorRecordThatCannotBeQueuedChangesNeitherTheAnswerNorTheLogs() throws Exception {
        ErrorEventService full = mock(ErrorEventService.class);
        doThrow(new TaskRejectedException("queue full")).when(full).logError(any(), any(), any(), any(), any());
        Object recorder = ReflectionTestUtils.getField(exceptionTranslator, "errorEventService");
        ReflectionTestUtils.setField(exceptionTranslator, "errorEventService", full);
        try {
            send("/api/exception-translator-test/method-argument", "{\"test\": secrettoken9}");
        } finally {
            ReflectionTestUtils.setField(exceptionTranslator, "errorEventService", recorder);
        }

        assertNothingLogged("secrettoken9");
        assertThat(lines()).contains("WARN Request error (400, HttpMessageNotReadableException) not recorded in "
            + "error_event: TaskRejectedException");
    }

    /** The answer is Spring's ProblemDetail for an unreadable body, as phones get it (A2 W1-23). */
    private void send(String path, String body) throws Exception {
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Failed to read request"))
            .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    private List<String> lines() {
        return logs.list.stream().map(event -> event.getLevel() + " " + event.getFormattedMessage()).toList();
    }

    private void assertNothingLogged(String bodyFragment) {
        assertThat(logs.list).as("log events").isNotEmpty();
        for (ILoggingEvent event : logs.list) {
            assertThat(event.getFormattedMessage()).as("a line of %s", event.getLoggerName())
                .doesNotContain(bodyFragment);
            for (String message : throwableMessages(event.getThrowableProxy())) {
                assertThat(message).as("an exception logged by %s", event.getLoggerName()).doesNotContain(bodyFragment);
            }
        }
    }

    private static List<String> throwableMessages(IThrowableProxy proxy) {
        List<String> messages = new ArrayList<>();
        for (IThrowableProxy part = proxy; part != null && messages.size() < 20; part = part.getCause()) {
            messages.add(String.valueOf(part.getMessage()));
            for (IThrowableProxy suppressed : part.getSuppressed()) {
                messages.addAll(throwableMessages(suppressed));
            }
        }
        return messages;
    }
}
