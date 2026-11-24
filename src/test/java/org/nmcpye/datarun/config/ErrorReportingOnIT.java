package org.nmcpye.datarun.config;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import io.sentry.Sentry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With a DSN the whole application starts with error reporting on, and an ERROR line logged while a request is
 * served reaches GlitchTip without the request, the user or the line's values. The request id stays, to find the
 * full line in the application's log. The context is closed afterwards, which closes the SDK.
 */
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
@Import({ErrorReportingOnIT.FailingResource.class, ErrorReportingOnIT.UsersPathFilter.class})
class ErrorReportingOnIT {

    private static final FakeGlitchTip GLITCHTIP = FakeGlitchTip.start();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void reportToTheStandIn(DynamicPropertyRegistry registry) {
        registry.add("sentry.dsn", GLITCHTIP::dsn);
        registry.add("sentry.environment", () -> "test");
    }

    @AfterAll
    static void stopReporting() {
        Sentry.close();
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).detachAppender("SENTRY_APPENDER");
        GLITCHTIP.close();
    }

    @Test
    @WithMockUser("777000111")
    void anErrorDuringARequestArrivesWithoutTheRequestTheUserOrTheValues() throws Exception {
        assertThat(Sentry.isEnabled()).isTrue();

        mockMvc
            .perform(post("/api/v1/test/error-reporting")
                .queryParam("login", "777000111")
                .header("X-Request-ID", "error-reporting-it")
                .header("X-Secret", "secret-header")
                .cookie(new Cookie("SESSION", "secret-cookie"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"household name\"}"))
            .andExpect(status().isNoContent());
        Sentry.flush(5000);

        assertThat(String.join("\n", GLITCHTIP.envelopes()))
            .doesNotContain("777000111", "sub00000001", "household name", "secret-header", "secret-cookie");
        assertThat(eventsFrom(FailingResource.class)).singleElement().satisfies(event -> {
            assertThat(event.path("message").path("formatted").asText())
                .isEqualTo("Submission {} from {} failed unexpectedly");
            assertThat(event.path("transaction").asText()).as("the request was in scope")
                .isEqualTo("POST /api/v1/test/error-reporting");
            assertThat(event.path("contexts").path("MDC").path("requestId").asText())
                .isEqualTo("error-reporting-it");
            assertThat(event.path("environment").asText()).isEqualTo("test");
            JsonNode exception = event.path("exception").path("values").get(0);
            assertThat(exception.path("type").asText()).isEqualTo("IllegalStateException");
            assertThat(exception.has("value")).isFalse();
            assertThat(event.has("request")).isFalse();
            assertThat(event.has("user")).isFalse();
            assertThat(event.has("breadcrumbs")).isFalse();
        });
    }

    /** Before a route matches, the SDK's default transaction name would be the raw path, here with a login. */
    @Test
    @WithMockUser("777000111")
    void anErrorBeforeRoutingCarriesNoRawPath() throws Exception {
        mockMvc.perform(get("/api/v1/test/users/777000111")).andExpect(status().isNoContent());
        Sentry.flush(5000);

        assertThat(String.join("\n", GLITCHTIP.envelopes())).doesNotContain("777000111");
        assertThat(eventsFrom(UsersPathFilter.class)).singleElement().satisfies(event -> {
            assertThat(event.path("message").path("formatted").asText()).isEqualTo("A filter failed before routing");
            assertThat(event.has("transaction")).isFalse();
            assertThat(event.has("request")).isFalse();
        });
    }

    private static List<JsonNode> eventsFrom(Class<?> source) {
        return GLITCHTIP.events().stream()
            .filter(event -> event.path("logger").asText().equals(source.getName()))
            .toList();
    }

    /** Logs as the upload path does when one submission fails unexpectedly. */
    @RestController
    static class FailingResource {

        private static final org.slf4j.Logger log = LoggerFactory.getLogger(FailingResource.class);

        @PostMapping("/api/v1/test/error-reporting")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void failOneSubmission(@RequestBody String body) {
            log.error("Submission {} from {} failed unexpectedly", "sub00000001", "777000111",
                new IllegalStateException("For input string: \"household name\""));
        }
    }

    /** Logs an ERROR line for a user's path before any route has matched, as the JWT decoder can. */
    static class UsersPathFilter extends OncePerRequestFilter {

        private static final org.slf4j.Logger log = LoggerFactory.getLogger(UsersPathFilter.class);

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !request.getRequestURI().startsWith("/api/v1/test/users/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) {
            log.error("A filter failed before routing");
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }
}
