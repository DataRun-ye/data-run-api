package org.nmcpye.datarun.config;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import io.sentry.protocol.User;
import io.sentry.spring.boot.jakarta.SentryProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.info.BuildProperties;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorReportingConfigurationTest {

    private final ErrorReportingConfiguration configuration = new ErrorReportingConfiguration();

    @Test
    void anEventKeepsWhatFailedAndWhereButNoValues() {
        SentryEvent event = new SentryEvent();
        event.setLogger("org.nmcpye.datarun.web.rest.v1.datasubmission.service.SubmissionUploadService");
        Message message = new Message();
        message.setMessage("Submission {} from {} failed unexpectedly");
        message.setParams(List.of("sub00000001", "777000111"));
        message.setFormatted("Submission sub00000001 from 777000111 failed unexpectedly");
        event.setMessage(message);
        SentryException exception = new SentryException();
        exception.setType("IllegalStateException");
        exception.setModule("java.lang");
        exception.setValue("For input string: \"household name\"");
        SentryStackFrame frame = new SentryStackFrame();
        frame.setModule("org.nmcpye.datarun.jpa.datasubmission.Example");
        frame.setFunction("save");
        frame.setLineno(42);
        exception.setStacktrace(new SentryStackTrace(List.of(frame)));
        event.setExceptions(List.of(exception));
        Request request = new Request();
        request.setUrl("https://api.example/api/v1/dataSubmission/bulk");
        request.setQueryString("login=777000111");
        request.setHeaders(Map.of("Authorization", "Bearer token", "Cookie", "SESSION=abc"));
        request.setData("{\"name\":\"household name\"}");
        event.setRequest(request);
        User user = new User();
        user.setUsername("777000111");
        user.setIpAddress("192.0.2.7");
        event.setUser(user);
        event.setBreadcrumbs(List.of(new Breadcrumb("Submission sub00000001 from 777000111 rejected")));
        event.setExtra("thread_name", "XNIO-1 task-2");
        event.getContexts().put("MDC", Map.of("requestId", "6f1c0b0e"));

        SentryEvent sent = ErrorReportingConfiguration.withoutValues(event, new Hint());

        assertThat(sent.getRequest()).isNull();
        assertThat(sent.getUser()).isNull();
        assertThat(sent.getBreadcrumbs()).isNullOrEmpty();
        assertThat(sent.getMessage().getFormatted()).isEqualTo("Submission {} from {} failed unexpectedly");
        assertThat(sent.getMessage().getMessage()).isEqualTo("Submission {} from {} failed unexpectedly");
        assertThat(sent.getMessage().getParams()).isNull();
        assertThat(sent.getExceptions()).singleElement().satisfies(kept -> {
            assertThat(kept.getValue()).isNull();
            assertThat(kept.getType()).isEqualTo("IllegalStateException");
            assertThat(kept.getModule()).isEqualTo("java.lang");
            assertThat(kept.getStacktrace().getFrames()).containsExactly(frame);
        });
        assertThat(sent.getLogger()).endsWith("SubmissionUploadService");
        assertThat(sent.getExtra("thread_name")).isEqualTo("XNIO-1 task-2");
        assertThat(sent.getContexts().get("MDC")).isEqualTo(Map.of("requestId", "6f1c0b0e"));
    }

    /** JBoss Logging formats before logback sees the line, so the "pattern" is the whole text. */
    @Test
    void anotherLibrarysLineArrivesAsItsLoggersName() {
        SentryEvent event = new SentryEvent();
        event.setLogger("org.hibernate.engine.jdbc.spi.SqlExceptionHelper");
        Message message = new Message();
        message.setMessage("ERROR: invalid input syntax for type integer: \"777000111\"");
        message.setFormatted("ERROR: invalid input syntax for type integer: \"777000111\"");
        event.setMessage(message);

        Message sent = ErrorReportingConfiguration.withoutValues(event, new Hint()).getMessage();

        assertThat(sent.getMessage()).isEqualTo("org.hibernate.engine.jdbc.spi.SqlExceptionHelper");
        assertThat(sent.getFormatted()).isEqualTo("org.hibernate.engine.jdbc.spi.SqlExceptionHelper");
        assertThat(sent.getParams()).isNull();
    }

    @Test
    void anOwnLineWithoutAPatternKeepsItsText() {
        SentryEvent event = new SentryEvent();
        event.setLogger("org.nmcpye.datarun.etl.Example");
        Message message = new Message();
        message.setFormatted("Written by the code itself");
        event.setMessage(message);

        assertThat(ErrorReportingConfiguration.withoutValues(event, new Hint()).getMessage().getFormatted())
            .isEqualTo("Written by the code itself");
    }

    @Test
    void aMessageFromNoLoggerIsDropped() {
        SentryEvent event = new SentryEvent();
        Message message = new Message();
        message.setFormatted("Captured by some library");
        event.setMessage(message);

        assertThat(ErrorReportingConfiguration.withoutValues(event, new Hint()).getMessage()).isNull();
    }

    @Test
    void theOptionsHoldWhateverThePropertiesSay() {
        SentryProperties options = new SentryProperties();
        options.setSendDefaultPii(true);
        options.setMaxRequestBodySize(SentryOptions.RequestSize.ALWAYS);
        options.setMaxBreadcrumbs(100);
        options.setTracesSampleRate(1.0);
        options.setTracesSampler(context -> 1.0);
        options.getLogs().setEnabled(true);
        options.getLogging().setMinimumEventLevel(Level.INFO);

        configuration.errorReportingOptions(build("6.5.3")).configure(options);

        assertThat(options.isSendDefaultPii()).isFalse();
        assertThat(options.getMaxRequestBodySize()).isEqualTo(SentryOptions.RequestSize.NONE);
        assertThat(options.getMaxBreadcrumbs()).isZero();
        assertThat(options.getTracesSampleRate()).isNull();
        assertThat(options.getTracesSampler()).isNull();
        assertThat(options.isTracingEnabled()).isFalse();
        assertThat(options.getTracePropagationTargets()).isEmpty();
        assertThat(options.getLogs().isEnabled()).isFalse();
        assertThat(options.getLogging().getMinimumEventLevel()).isEqualTo(Level.ERROR);
        assertThat(options.getRelease()).isEqualTo("6.5.3");
    }

    @Test
    void withoutBuildInformationTheReleaseIsLeftAlone() {
        SentryProperties options = new SentryProperties();

        configuration.errorReportingOptions(new DefaultListableBeanFactory().getBeanProvider(BuildProperties.class))
            .configure(options);

        assertThat(options.getRelease()).isNull();
    }

    private static org.springframework.beans.factory.ObjectProvider<BuildProperties> build(String version) {
        Properties entries = new Properties();
        entries.setProperty("version", version);
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("buildProperties", new BuildProperties(entries));
        return beans.getBeanProvider(BuildProperties.class);
    }
}
