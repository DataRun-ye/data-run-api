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
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
            assertThat(glitchTip.events())
                .extracting(event -> event.path("logger").asText() + " | " + event.path("message").path("formatted").asText())
                .containsExactlyInAnyOrder(
                    "org.hibernate.engine.jdbc.spi.SqlExceptionHelper | org.hibernate.engine.jdbc.spi.SqlExceptionHelper",
                    "io.undertow.request | io.undertow.request");
            assertThat(glitchTip.events()).filteredOn(event -> event.has("exception")).singleElement()
                .satisfies(event -> assertThat(event.path("exception").path("values").get(0).path("type").asText())
                    .isEqualTo("IllegalStateException"));
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
