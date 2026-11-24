package org.nmcpye.datarun.web.rest.errors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An unreadable request body never reaches the logs: the exception's message quotes the body (a malformed token, a
 * value of the wrong type), so the one line logged, ExceptionTranslator's INFO line for a 4xx, names the route and
 * the exception types only, and the answer stays Spring's 400. Every logger's lines are read, at the levels
 * production logs.
 */
@WithMockUser
@AutoConfigureMockMvc
@IntegrationTest
class UnreadableBodyLoggingIT {

    private static final String REQUEST_ID = ", request [0-9a-f-]{36}";

    @Autowired
    private MockMvc mockMvc;

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
        assertThat(lines()).anySatisfy(line -> assertThat(line).matches("INFO Request POST "
            + "/api/exception-translator-test/method-argument answered 400 \\(error\\.http\\.400\\): "
            + "HttpMessageNotReadableException < JsonParseException" + REQUEST_ID));
    }

    @Test
    void aValueOfTheWrongTypeIsLoggedByRouteAndTypeOnly() throws Exception {
        send("/api/exception-translator-test/typed-body", "{\"count\": \"secretvalue8\"}");

        assertNothingLogged("secretvalue8");
        assertThat(lines()).anySatisfy(line -> assertThat(line).matches("INFO Request POST "
            + "/api/exception-translator-test/typed-body answered 400 \\(error\\.http\\.400\\): "
            + "HttpMessageNotReadableException < InvalidFormatException" + REQUEST_ID));
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
