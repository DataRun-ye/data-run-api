package org.nmcpye.datarun.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.JsonNode;
import io.sentry.IScopes;
import io.sentry.Sentry;
import io.sentry.logback.SentryAppender;
import io.sentry.spring.boot.jakarta.SentryAutoConfiguration;
import io.sentry.spring.boot.jakarta.SentryLogbackAppenderAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.rest.errors.ExceptionTranslator;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Error reporting through the SDK's own Spring Boot and Logback integration, against a stand-in for GlitchTip: what
 * leaves the server, and that nothing does without a DSN.
 */
class ErrorReportingTest {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger("org.nmcpye.datarun.example.UploadService");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SentryAutoConfiguration.class,
            SentryLogbackAppenderAutoConfiguration.class))
        .withUserConfiguration(ErrorReportingConfiguration.class);

    @AfterEach
    void closeTheSdk() {
        Sentry.close();
        root().detachAppender("SENTRY_APPENDER");
        System.clearProperty("sentry.dsn");
    }

    /**
     * Whenever the configuration leaves the SDK off, nothing may start it. Sentry-logback's appender would initialise
     * Sentry itself from the environment, with no beforeSend; the system property stands in for SENTRY_DSN, which the
     * SDK reads the same way, and Spring binds it as sentry.dsn as it does the environment variable.
     */
    @Test
    void whenTheConfigurationLeavesTheSdkOffTheAppenderCannotStartIt() {
        try (FakeGlitchTip glitchTip = FakeGlitchTip.start()) {
            String server = glitchTip.dsn().substring(0, glitchTip.dsn().lastIndexOf('/') + 1);
            Map<String, String[]> cases = Map.of(
                server + "project", new String[0], // the SDK takes a project id that is not a number; the check does not
                server + "sub path/1", new String[0], // nor a space in the path
                glitchTip.dsn(), new String[]{"sentry.enabled=false"}); // a good DSN with reporting switched off
            cases.forEach((dsn, properties) -> {
                System.setProperty("sentry.dsn", dsn);
                try {
                    runner.withPropertyValues(properties).run(context -> {
                        assertThat(context).hasNotFailed();
                        log.error("Submission {} from {} failed unexpectedly", "sub00000001", "777000111");
                        assertThat(Sentry.isEnabled()).as(dsn).isFalse();
                    });
                } finally {
                    closeTheSdk();
                }
            });

            assertThat(glitchTip.envelopes()).isEmpty();
        }
    }

    @Test
    void withoutADsnTheSdkIsNotSetUp() {
        assumeNoDsnInTheEnvironment();

        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(IScopes.class);
            assertThat(Sentry.isEnabled()).isFalse();
            assertThat(rootAppenders()).noneMatch(SentryAppender.class::isInstance);
        });
    }

    /** Compose passes SENTRY_DSN empty when the host's .env leaves it out. */
    @Test
    void anEmptyDsnLeavesTheSdkOff() {
        assumeNoDsnInTheEnvironment();

        runner.withPropertyValues("sentry.dsn=").run(context -> {
            assertThat(context).hasNotFailed();
            log.error("An error with nowhere to go");
            assertThat(Sentry.isEnabled()).isFalse();
        });
    }

    /** The SDK would throw on it while the context starts, and compose would restart the application forever. */
    @Test
    void aMalformedDsnLeavesTheSdkOffAndTheApplicationUp() {
        assumeNoDsnInTheEnvironment();

        runner.withPropertyValues("sentry.dsn=not-a-dsn").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(Sentry.isEnabled()).isFalse();
        });
    }

    @Test
    void anErrorLineArrivesWithoutItsValuesAndNothingElseIsSent() {
        try (FakeGlitchTip glitchTip = FakeGlitchTip.start()) {
            runner
                .withPropertyValues("sentry.dsn=" + glitchTip.dsn(), "sentry.environment=test",
                    // Loosened on purpose: the configuration holds its settings whatever these say.
                    "sentry.send-default-pii=true", "sentry.max-breadcrumbs=100", "sentry.traces-sample-rate=1.0",
                    "sentry.logs.enabled=true")
                .withBean(BuildProperties.class, () -> build("6.5.3"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(Sentry.isEnabled()).isTrue();

                    log.info("Login 777000111 opened an upload");
                    log.warn("Submission {} from {} rejected: {}", "sub00000001", "777000111", "no access");
                    log.error("Submission {} from {} failed unexpectedly", "sub00000001", "777000111",
                        new IllegalStateException("For input string: \"household name\""));
                    Sentry.flush(5000);
                });

            assertThat(String.join("\n", glitchTip.envelopes()))
                .doesNotContain("777000111", "sub00000001", "household name", "no access");
            assertThat(glitchTip.events()).singleElement().satisfies(event -> {
                assertThat(event.path("level").asText()).isEqualTo("error");
                assertThat(event.path("logger").asText()).isEqualTo("org.nmcpye.datarun.example.UploadService");
                assertThat(event.path("environment").asText()).isEqualTo("test");
                assertThat(event.path("release").asText()).isEqualTo("6.5.3");
                assertThat(event.path("message").path("formatted").asText())
                    .isEqualTo("Submission {} from {} failed unexpectedly");
                assertThat(event.path("message").has("params")).isFalse();
                JsonNode exception = event.path("exception").path("values").get(0);
                assertThat(exception.path("type").asText()).isEqualTo("IllegalStateException");
                assertThat(exception.has("value")).isFalse();
                assertThat(exception.path("stacktrace").path("frames").size()).isPositive();
                assertThat(event.has("request")).isFalse();
                assertThat(event.has("user")).isFalse();
                assertThat(event.has("breadcrumbs")).isFalse();
                assertThat(event.has("server_name")).isFalse();
            });
            assertThat(glitchTip.envelopes()).as("one envelope: no transaction, log or other item")
                .singleElement().satisfies(envelope -> assertThat(envelope.split("\n")).hasSize(3));
        }
    }

    /**
     * Hibernate and Undertow log through JBoss Logging, which formats values into the text before logback sees it,
     * as these lines do.
     */
    @Test
    void anotherLibrarysErrorLineArrivesWithoutItsText() {
        try (FakeGlitchTip glitchTip = FakeGlitchTip.start()) {
            runner.withPropertyValues("sentry.dsn=" + glitchTip.dsn()).run(context -> {
                assertThat(Sentry.isEnabled()).isTrue();
                LoggerFactory.getLogger("org.hibernate.engine.jdbc.spi.SqlExceptionHelper")
                    .error("ERROR: invalid input syntax for type integer: \"777000111\"");
                LoggerFactory.getLogger("io.undertow.request").error(
                    "UT005023: Exception handling request to /api/v1/admin/users/777000111",
                    new IllegalStateException("household name"));
                Sentry.flush(5000);
            });

            assertThat(String.join("\n", glitchTip.envelopes())).doesNotContain("777000111", "household name");
            assertThat(glitchTip.events()).as("each line's text is its logger's name")
                .allSatisfy(event -> assertThat(event.path("message").path("formatted").asText())
                    .isEqualTo(event.path("logger").asText()))
                .extracting(event -> event.path("logger").asText())
                .containsExactlyInAnyOrder("org.hibernate.engine.jdbc.spi.SqlExceptionHelper", "io.undertow.request");
            assertThat(glitchTip.events()).filteredOn(event -> event.has("exception")).singleElement()
                .satisfies(event -> assertThat(event.path("exception").path("values").get(0).path("type").asText())
                    .isEqualTo("IllegalStateException"));
        }
    }

    /**
     * The expected duplicate-uid race of submissions: the upload logs it at WARN, and Hibernate's ERROR line for the
     * same insert does not reach GlitchTip. Another constraint's line still does.
     */
    @Test
    void theSubmissionUidRaceSendsNoEvent() {
        try (FakeGlitchTip glitchTip = FakeGlitchTip.start()) {
            runner.withPropertyValues("sentry.dsn=" + glitchTip.dsn()).run(context -> {
                org.slf4j.Logger hibernate =
                    LoggerFactory.getLogger("org.hibernate.engine.jdbc.spi.SqlExceptionHelper");
                hibernate.warn("SQL Error: 0, SQLState: 23505");
                hibernate.error("ERROR: duplicate key value violates unique constraint \"uk_data_submission_uid\"");
                LoggerFactory.getLogger("org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads").warn(
                    "Submission {} (form {}) from user {} not saved: another save of the same uid came first: {}",
                    "sub00000001", "form0000001", "01JUSER0000000000000000007", "SQLState 23505");
                hibernate.error("ERROR: duplicate key value violates unique constraint \"uc_assignment_activity_id\"");
                Sentry.flush(5000);
            });

            assertThat(glitchTip.events()).as("only the other constraint's line")
                .singleElement().satisfies(event -> assertThat(event.path("logger").asText())
                    .isEqualTo("org.hibernate.engine.jdbc.spi.SqlExceptionHelper"));
        }
    }

    /** Handled server faults reach GlitchTip grouped by route, exception type and SQLState, with no argument. */
    @Test
    void handledServerFaultsArriveFingerprintedWithoutTheirArguments() {
        try (FakeGlitchTip glitchTip = FakeGlitchTip.start()) {
            runner.withPropertyValues("sentry.dsn=" + glitchTip.dsn()).run(context -> {
                org.slf4j.Logger translator = LoggerFactory.getLogger(ExceptionTranslator.class);
                String duplicate = "DataIntegrityViolationException < PSQLException, SQLState 23505, at A.b:1";
                translator.error(ExceptionTranslator.SERVER_FAULT_LINE, "POST", "/api/v1/assignments", 500,
                    "error.http.500", duplicate, "6f1c0b0e-0000-4000-8000-000000000001");
                translator.error(ExceptionTranslator.SERVER_FAULT_LINE, "POST", "/api/v1/assignments", 500,
                    "error.http.500", duplicate, "6f1c0b0e-0000-4000-8000-000000000002");
                translator.error(ExceptionTranslator.SERVER_FAULT_LINE, "GET", "/api/v1/teams/{id}", 500,
                    "error.http.500", "NullPointerException, at T.c:9", "6f1c0b0e-0000-4000-8000-000000000003");
                Sentry.flush(5000);
            });

            assertThat(String.join("\n", glitchTip.envelopes())).doesNotContain("6f1c0b0e-0000-4000-8000");
            assertThat(glitchTip.events()).hasSize(3)
                .extracting(event -> event.path("fingerprint").toString())
                .containsExactlyInAnyOrder(
                    "[\"handled-server-fault\",\"POST /api/v1/assignments\",\"DataIntegrityViolationException\",\"SQLState 23505\"]",
                    "[\"handled-server-fault\",\"POST /api/v1/assignments\",\"DataIntegrityViolationException\",\"SQLState 23505\"]",
                    "[\"handled-server-fault\",\"GET /api/v1/teams/{id}\",\"NullPointerException\"]");
        }
    }

    /**
     * With a DSN, Sentry is the JVM's default uncaught exception handler. The container log must still show the
     * stack trace, since the event carries none of the exception's message.
     */
    @Test
    void anUncaughtExceptionReachesTheLogInFullAndGlitchTipWithoutItsMessage() {
        Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(null);
        PrintStream stderr = System.err;
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        try (FakeGlitchTip glitchTip = FakeGlitchTip.start()) {
            runner.withPropertyValues("sentry.dsn=" + glitchTip.dsn()).run(context -> {
                System.setErr(new PrintStream(printed, true, StandardCharsets.UTF_8));
                try {
                    Thread dying = new Thread(() -> {
                        throw new IllegalStateException("household name");
                    }, "dying-thread");
                    dying.start();
                    dying.join(5000);
                } finally {
                    System.setErr(stderr);
                }
                Sentry.flush(5000);
            });

            assertThat(printed.toString(StandardCharsets.UTF_8))
                .contains("java.lang.IllegalStateException: household name");
            assertThat(String.join("\n", glitchTip.envelopes())).doesNotContain("household name");
            assertThat(glitchTip.events()).singleElement().satisfies(event -> {
                assertThat(event.path("level").asText()).isEqualTo("fatal");
                JsonNode exception = event.path("exception").path("values").get(0);
                assertThat(exception.path("type").asText()).isEqualTo("IllegalStateException");
                assertThat(exception.has("value")).isFalse();
            });
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(before);
        }
    }

    /**
     * A GlitchTip that takes the connection but never answers, as one behind a stuck proxy. With the SDK's defaults,
     * closing waited for the stuck send for up to 15 s more, past docker's 10 s stop grace.
     */
    @Test
    void aGlitchTipThatNeverAnswersDelaysShutdownBySecondsOnly() throws IOException {
        try (ServerSocket silent = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            long[] closing = new long[1];
            runner.withPropertyValues("sentry.dsn=http://publickey@127.0.0.1:" + silent.getLocalPort() + "/1")
                .run(context -> {
                    assertThat(Sentry.isEnabled()).isTrue();
                    log.error("A report that never arrives");
                    closing[0] = System.nanoTime();
                });

            assertThat(Duration.ofNanos(System.nanoTime() - closing[0])).as("within docker's 10 s stop grace")
                .isLessThan(Duration.ofSeconds(9));
        }
    }

    private static void assumeNoDsnInTheEnvironment() {
        String dsn = System.getenv("SENTRY_DSN");
        assumeTrue(dsn == null || dsn.isBlank(), "SENTRY_DSN is set in this environment");
    }

    private static BuildProperties build(String version) {
        Properties entries = new Properties();
        entries.setProperty("version", version);
        return new BuildProperties(entries);
    }

    private static Logger root() {
        return (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    }

    private static List<Appender<?>> rootAppenders() {
        List<Appender<?>> appenders = new ArrayList<>();
        for (Iterator<? extends Appender<?>> it = root().iteratorForAppenders(); it.hasNext(); ) {
            appenders.add(it.next());
        }
        return appenders;
    }
}
