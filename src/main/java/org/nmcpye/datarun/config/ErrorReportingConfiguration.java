package org.nmcpye.datarun.config;

import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import io.sentry.spring.boot.jakarta.SentryProperties;
import io.sentry.spring.jakarta.tracing.SpringMvcTransactionNameProvider;
import io.sentry.spring.jakarta.tracing.TransactionNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Error reporting to GlitchTip through the Sentry SDK (sentry-spring-boot-starter-jakarta and sentry-logback).
 * <p>
 * It is off unless {@code SENTRY_DSN} is set. Without a DSN the SDK's auto-configuration does not load; with an
 * empty or malformed one the SDK stays disabled. Either way nothing is sent. With a DSN, every ERROR log line and every
 * uncaught exception becomes an event. {@code SENTRY_ENVIRONMENT} names the deployment, and the release is the
 * build's version. Request errors are not reported: {@code ExceptionTranslator} answers all of them and records
 * them in {@code error_event}, so none reaches Sentry's exception resolver.
 * <p>
 * The settings in {@link #errorReportingOptions} override any {@code sentry.*} property or environment variable.
 * An event tells what failed and where, never with which values: log arguments and exception messages can carry a
 * login or submitted values, so {@link #withoutValues} sends the exception types and the stack frames, and the log
 * line's pattern only for the application's own loggers. The application's ERROR lines must therefore pass every
 * value as a {@code {}} argument, never inside the pattern.
 */
@Configuration
public class ErrorReportingConfiguration {

    /** Loggers whose ERROR patterns this code base writes, with values only in {@code {}} arguments. */
    static final String OWN_LOGGERS = "org.nmcpye.datarun.";

    private static final Logger log = LoggerFactory.getLogger(ErrorReportingConfiguration.class);

    /**
     * Errors only, and nothing collected about the user or the request. No traces sample rate is set: any rate, 0
     * included, turns tracing on, and the SDK then follows an incoming {@code sentry-trace} header that says
     * "sampled" and sends that request's transaction.
     */
    @Bean
    public Sentry.OptionsConfiguration<SentryOptions> errorReportingOptions(ObjectProvider<BuildProperties> build) {
        return options -> {
            if (options.getDsn() != null && !options.getDsn().isEmpty() && !isDsn(options.getDsn())) {
                log.warn("SENTRY_DSN is not a DSN of the form https://<public key>@<host>/<project id>; "
                    + "error reporting stays off");
                options.setDsn("");
            }
            options.setSendDefaultPii(false); // no user, IP address or cookies
            options.setMaxRequestBodySize(SentryOptions.RequestSize.NONE);
            options.setMaxBreadcrumbs(0); // breadcrumbs repeat earlier log lines, and a WARN line can carry a login
            options.setTracesSampleRate(null);
            options.setTracesSampler(null);
            options.setTracePropagationTargets(List.of()); // no trace headers on outgoing requests
            options.getLogs().setEnabled(false); // log lines are not shipped; ERROR lines become events
            if (options instanceof SentryProperties properties) {
                properties.getLogging().setMinimumEventLevel(Level.ERROR);
            }
            build.ifAvailable(info -> options.setRelease(info.getVersion()));
        };
    }

    @Bean
    public SentryOptions.BeforeSendCallback errorReportingWithoutValues() {
        return ErrorReportingConfiguration::withoutValues;
    }

    /**
     * Whether a value is a DSN the SDK accepts: http(s), a public key, a host, and a numeric project id as the last
     * path segment. The SDK throws on a malformed DSN while the context starts, and compose restarts the application
     * after every failed start, so anything this accepts must also pass the SDK's own parser; it is the stricter.
     */
    static boolean isDsn(String dsn) {
        try {
            URI uri = new URI(dsn.trim());
            String scheme = uri.getScheme();
            String userInfo = uri.getRawUserInfo();
            String path = uri.getRawPath();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || userInfo == null || userInfo.isEmpty() || userInfo.startsWith(":")
                || uri.getHost() == null || path == null) {
                return false;
            }
            String withoutSlash = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            String projectId = withoutSlash.substring(withoutSlash.lastIndexOf('/') + 1);
            return !projectId.isEmpty() && projectId.chars().allMatch(c -> c >= '0' && c <= '9');
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Names an event's transaction by the matched route only ({@code GET /api/v1/users/{login}}), or not at all.
     * The SDK's default falls back to the raw request path while no route has matched, as for a line a filter logs,
     * and a path can hold a login.
     */
    @Bean
    public TransactionNameProvider errorReportingRouteNames() {
        return new SpringMvcTransactionNameProvider();
    }

    /**
     * Keeps what failed and where: the logger, the level, the exception types, the stack frames, the thread, the
     * request id and the release, and for the application's own loggers the log line's pattern. Drops the request,
     * the user, breadcrumbs, the log line's arguments and every exception message. Another library's line is sent as
     * its logger's name: libraries that log through JBoss Logging (Hibernate, Undertow) format values into the text
     * before logback sees it, so its pattern is the whole line. Undertow's UT005023 names the request path, and
     * Hibernate's SqlExceptionHelper logs the database's message, which can quote an input value even with
     * {@code logServerErrorDetail=false}.
     */
    static SentryEvent withoutValues(SentryEvent event, Hint hint) {
        event.setRequest(null);
        event.setUser(null);
        event.setBreadcrumbs(null);
        Message message = event.getMessage();
        String logger = event.getLogger();
        if (message != null && logger == null) {
            event.setMessage(null);
        } else if (message != null) {
            if (!logger.startsWith(OWN_LOGGERS)) {
                message.setMessage(logger);
                message.setFormatted(logger);
            } else if (message.getMessage() != null) {
                message.setFormatted(message.getMessage());
            }
            message.setParams(null);
        }
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null) {
            exceptions.forEach(exception -> exception.setValue(null));
        }
        return event;
    }
}
