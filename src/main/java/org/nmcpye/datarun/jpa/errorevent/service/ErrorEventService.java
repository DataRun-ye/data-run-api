package org.nmcpye.datarun.jpa.errorevent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.DatabaseFailures;
import org.nmcpye.datarun.jpa.errorevent.ErrorEvent;
import org.nmcpye.datarun.jpa.errorevent.repository.ErrorEventRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Records the request errors the REST layer answers in {@code error_event}. A record says what failed and where,
 * never with which values: an exception's message can quote a submitted value, a request body or the database's row,
 * so a record keeps the error's code in {@code message}, the exception's type, a stack trace of exception types and
 * frames without their messages, and in {@code context_json} identifiers only (the request id, the user id, the
 * exception types of the causes, the SQLState and the violated constraint). Nothing reads these records in code; they
 * are read by hand.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ErrorEventService {
    /**
     * The request attribute in which a failed login leaves its account's durable id, when the account exists; never
     * the login it tried. {@code ExceptionTranslator} reads it into the record's {@code userId}.
     */
    public static final String ATTR_FAILED_LOGIN_ACCOUNT_ID = "LOGGING_FAILED_LOGIN_ACCOUNT_ID";

    private static final int MAX_STACKTRACE = 4000;

    private final ErrorEventRepository repo;
    private final ObjectMapper objectMapper;

    /**
     * Records one request error. Runs on another thread in production, so the caller passes what it reads from the
     * request's thread (the request id, the user id) in {@code context}.
     *
     * @param errorCode the error's code: the answer's message key ({@code error.validation}, {@code error.http.401})
     *                  or a refusal's code; stored in {@code message}
     * @param context   identifiers only, never a value from the request
     */
    @Async("errorLoggingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(Throwable ex, NativeWebRequest request, HttpStatus status, String errorCode,
                         Map<String, Object> context) {
        try {
            HttpServletRequest servletRequest = null;
            if (request instanceof ServletWebRequest) {
                servletRequest = ((ServletWebRequest) request).getRequest();
            }

            ErrorEvent ev = ErrorEvent.builder()
                .occurredAt(Instant.now())
                .level("ERROR")
                .status(status != null ? status.value() : 500)
                .path(extractPath(servletRequest))
                .method(servletRequest != null ? servletRequest.getMethod() : "UNKNOWN")
                .clientIp(extractClientIp(servletRequest))
                .username(resolveUsername(ex))
                .message(errorCode)
                .exception(ex.getClass().getName())
                .stacktrace(truncate(stackTraceWithoutMessages(ex), MAX_STACKTRACE))
                .contextJson(toJsonSafe(withFailureFacts(ex, context)))
                .build();
            // persistAndFlush is rarely needed in async unless you need the ID immediately
            repo.persistAndFlush(ev);
        } catch (Exception e) {
            log.error("Failed to persist error event: {} (SQLState {})",
                e.getClass().getSimpleName(), DatabaseFailures.sqlState(e).orElse("none"));
        }
    }

    /**
     * The stack trace as {@link Throwable#printStackTrace()} prints it (causes, suppressed exceptions, frames in
     * common elided), with each exception named by its type only: the line that printStackTrace starts with the
     * exception's message is where values appear.
     */
    static String stackTraceWithoutMessages(Throwable failure) {
        StringBuilder trace = new StringBuilder();
        append(trace, failure, new StackTraceElement[0], "", "", Collections.newSetFromMap(new IdentityHashMap<>()));
        return trace.toString();
    }

    private static void append(StringBuilder trace, Throwable failure, StackTraceElement[] enclosing, String caption,
                               String prefix, Set<Throwable> seen) {
        if (!seen.add(failure)) {
            trace.append(prefix).append(caption).append("[CIRCULAR REFERENCE: ")
                .append(failure.getClass().getName()).append("]\n");
            return;
        }
        StackTraceElement[] frames = failure.getStackTrace();
        int own = frames.length - 1;
        int outer = enclosing.length - 1;
        while (own >= 0 && outer >= 0 && frames[own].equals(enclosing[outer])) {
            own--;
            outer--;
        }
        trace.append(prefix).append(caption).append(failure.getClass().getName()).append('\n');
        for (int i = 0; i <= own; i++) {
            trace.append(prefix).append("\tat ").append(frames[i]).append('\n');
        }
        int inCommon = frames.length - 1 - own;
        if (inCommon > 0) {
            trace.append(prefix).append("\t... ").append(inCommon).append(" more\n");
        }
        for (Throwable suppressed : failure.getSuppressed()) {
            append(trace, suppressed, frames, "Suppressed: ", prefix + "\t", seen);
        }
        if (failure.getCause() != null) {
            append(trace, failure.getCause(), frames, "Caused by: ", prefix, seen);
        }
    }

    /**
     * The caller's identifiers; the exception types from the outermost to the root cause, which a stack trace cut at
     * {@value #MAX_STACKTRACE} characters usually loses; and for a database failure its SQLState and violated
     * constraint (schema names).
     */
    private static Map<String, Object> withFailureFacts(Throwable ex, Map<String, Object> context) {
        Map<String, Object> facts = new LinkedHashMap<>();
        if (context != null) {
            facts.putAll(context);
        }
        if (ex.getCause() != null) {
            facts.put("causes", causes(ex));
        }
        DatabaseFailures.sqlState(ex).ifPresent(state -> facts.put("sqlState", state));
        DatabaseFailures.constraint(ex).ifPresent(constraint -> facts.put("constraint", constraint));
        return facts;
    }

    /** For example {@code DataIntegrityViolationException < ConstraintViolationException < PSQLException}. */
    private static String causes(Throwable ex) {
        StringBuilder types = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = ex; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (!types.isEmpty()) {
                types.append(" < ");
            }
            types.append(cause.getClass().getSimpleName());
        }
        return types.toString();
    }

    private String extractUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? String.valueOf(auth.getName()) : "anonymous";
    }

    /**
     * Never the login a failed login tried, a personal identifier (until 6.5.5 it was stored): such a record stores
     * null, and the account's id, when the account exists, goes to {@code context_json}. Otherwise the request's
     * user, as before; production writes on another thread, where no user is known, so those rows read
     * {@code anonymous}.
     */
    private String resolveUsername(Throwable ex) {
        if (ex instanceof BadCredentialsException) {
            return null;
        }
        return SecurityUtils.getCurrentUserDetails()
            .map(CurrentUserDetails::getUsername)
            .orElse(extractUsername());
    }

    private String extractClientIp(HttpServletRequest req) {
        if (req == null) return null;
        String xf = req.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(xf)) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String extractPath(HttpServletRequest req) {
        return req != null ? req.getRequestURI() : "unknown";
    }

    private static String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }

    private String toJsonSafe(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\": \"json_serialization_failed\"}";
        }
    }
}
