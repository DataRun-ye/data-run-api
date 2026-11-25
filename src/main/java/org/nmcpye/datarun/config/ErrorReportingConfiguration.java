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
import jakarta.servlet.Filter;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.web.rest.errors.ExceptionTranslator;
import org.nmcpye.datarun.web.v2.problem.V2ProblemResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Error reporting to GlitchTip through the Sentry SDK (sentry-spring-boot-starter-jakarta and sentry-logback).
 * <p>
 * It is off unless {@code SENTRY_DSN} is set. Without a DSN the SDK's auto-configuration does not load; with an
 * empty or malformed one the SDK stays disabled. Either way nothing is sent. With a DSN, every ERROR log line and every
 * uncaught exception becomes an event, except Hibernate's line for the expected duplicate-uid race of submissions
 * ({@link #isSubmissionUidRace}). {@code SENTRY_ENVIRONMENT} names the deployment, and the release is the build's
 * version. {@code ExceptionTranslator} answers every request error, so none reaches Sentry's exception resolver; it
 * logs a server fault (5xx) as one ERROR line, which becomes an event like any other ERROR line (its pattern, the
 * route and the request id), and any other error (4xx) as an INFO line, which is not reported.
 * <p>
 * Only the starter's own initialisation may start the SDK, since only it carries these settings: sentry-logback's
 * appender initialises Sentry from the environment whenever it finds the SDK off, so {@link #errorReportingOptions}
 * blanks that source when it leaves the SDK off. (The third caller, {@code SentryInitBeanPostProcessor}, comes only
 * with {@code @EnableSentry}, which this application does not use.)
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

    private static final Pattern SQL_STATE = Pattern.compile("SQLState (\\w+)");

    /** Where Hibernate logs the database's error text, at ERROR, for every failed statement. */
    static final String HIBERNATE_SQL_ERRORS = "org.hibernate.engine.jdbc.spi.SqlExceptionHelper";

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
            if (!options.isEnabled() || options.getDsn() == null || options.getDsn().isEmpty()) {
                // Whenever this leaves the SDK off, the logback appender would start it on its own, from
                // SENTRY_DSN and without any of these settings or the beforeSend. It reads system properties first.
                System.setProperty("sentry.dsn", "");
            }
            options.setSendDefaultPii(false); // no user, IP address or cookies
            options.setMaxRequestBodySize(SentryOptions.RequestSize.NONE);
            options.setMaxBreadcrumbs(0); // breadcrumbs repeat earlier log lines, and a WARN line can carry a login
            options.setTracesSampleRate(null);
            options.setTracesSampler(null);
            options.setTracePropagationTargets(List.of()); // no trace headers on outgoing requests
            options.getLogs().setEnabled(false); // log lines are not shipped; ERROR lines become events
            options.setAttachServerName(false); // the container's host name says nothing GlitchTip needs
            // Sentry becomes the JVM's default uncaught exception handler and, unlike the JVM's own, prints nothing;
            // the event carries no message, so the stack trace must still reach the container log.
            options.setPrintUncaughtStackTrace(true);
            // How long a dying thread waits for its report to start going out, and shutdown for reports to leave.
            options.setFlushTimeoutMillis(2000);
            // A GlitchTip that does not answer holds each send for seconds, not the SDK's default 30 s, and
            // shutdown by a few seconds at most, within docker's 10 s stop grace.
            options.setConnectionTimeoutMillis(5000);
            options.setReadTimeoutMillis(5000);
            options.setShutdownTimeoutMillis(2000);
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
     * Takes the place of the starter's {@code SentryTracingFilter} (same bean name), unregistered. Tracing is off,
     * but that filter still continues an incoming {@code sentry-trace} and {@code baggage} header into the request's
     * scope, and from there into each report's envelope, where a client's baggage can name its user
     * ({@code sentry-user_id}). Spring Boot asserts a filter even on a disabled registration, hence the no-op one.
     */
    @Bean
    @ConditionalOnProperty(name = "sentry.dsn")
    public FilterRegistrationBean<Filter> sentryTracingFilter() {
        FilterRegistrationBean<Filter> registration =
            new FilterRegistrationBean<>((request, response, chain) -> chain.doFilter(request, response));
        registration.setEnabled(false);
        return registration;
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
        if (isSubmissionUidRace(event)) {
            return null;
        }
        event.setRequest(null);
        event.setUser(null);
        event.setBreadcrumbs(null);
        Message message = event.getMessage();
        String logger = event.getLogger();
        if (message != null && logger == null) {
            event.setMessage(null);
        } else if (message != null) {
            if (isHandledServerFault(logger, message)) {
                event.setFingerprints(handledServerFaultFingerprint(message.getParams()));
            }
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

    /**
     * A handled server fault's line (5xx), from ExceptionTranslator (v1) or V2ProblemResolver (v2), with at least
     * its first five arguments. Both lines take the same arguments in the same order.
     */
    private static boolean isHandledServerFault(String logger, Message message) {
        boolean v1 = ExceptionTranslator.class.getName().equals(logger)
            && ExceptionTranslator.SERVER_FAULT_LINE.equals(message.getMessage());
        boolean v2 = V2ProblemResolver.class.getName().equals(logger)
            && V2ProblemResolver.FAULT_LINE.equals(message.getMessage());
        return (v1 || v2) && message.getParams() != null && message.getParams().size() >= 5;
    }

    /**
     * How GlitchTip groups handled server faults. Their line carries no exception (the container log stays free of
     * exception messages), so without a fingerprint every fault would share its one pattern and one issue. The
     * fingerprint is the method and route pattern and the outermost exception type, with the SQLState when there is
     * one, read from the line's arguments (method, route, status, error code, failure shape, request id) before they
     * are dropped. The route pattern is already the event's transaction; the type and the SQLState name code, never
     * a value. The request id and the frames stay out, so one fault keeps one issue across requests and releases.
     */
    static List<String> handledServerFaultFingerprint(List<String> params) {
        String shape = params.get(4);
        List<String> fingerprint = new ArrayList<>(List.of(
            "handled-server-fault", params.get(0) + " " + params.get(1), shape.split(" < |,", 2)[0]));
        Matcher sqlState = SQL_STATE.matcher(shape);
        if (sqlState.find()) {
            fingerprint.add("SQLState " + sqlState.group(1));
        }
        return fingerprint;
    }

    /**
     * Hibernate's ERROR line for the expected duplicate-uid race of submissions: two saves of one new submission at
     * once, where the later insert hits {@code data_submission}'s unique uid constraint (a phone re-sending while
     * its first request runs, or a held release meeting a live copy). The upload logs that outcome at WARN with its
     * ids and SQLState, so this line is not reported; the container log keeps it. Hibernate logs it before the upload
     * can tell, and no check before the insert can see a save that is not committed yet. Only a unique violation of
     * that constraint is dropped, in PostgreSQL's English text. lc_messages was read on staging only (C.UTF-8);
     * production's was not read, and a server answering in another language leaves this line reported. Every other
     * database error, and this one in any other wording, is still reported.
     */
    private static boolean isSubmissionUidRace(SentryEvent event) {
        Message message = event.getMessage();
        if (!HIBERNATE_SQL_ERRORS.equals(event.getLogger()) || message == null) {
            return false;
        }
        String text = message.getFormatted() != null ? message.getFormatted() : message.getMessage();
        return text != null && text.contains("unique constraint \"" + DataSubmission.UID_CONSTRAINT + "\"");
    }
}
