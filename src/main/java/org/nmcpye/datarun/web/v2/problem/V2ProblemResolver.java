package org.nmcpye.datarun.web.v2.problem;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nmcpye.datarun.config.activation.NotEnabledException;
import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.web.v2.ApiV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Answers every exception a v2 request raises in MVC with a problem ({@link Problem}), and nothing else.
 * <p>
 * It runs before the application's other exception resolvers and declines any request outside {@link ApiV2#BASE},
 * so v1's error contract (ExceptionTranslator) is untouched. Being a resolver rather than a controller advice, it
 * also answers what fails before a v2 handler is chosen: an unknown v2 path (404), a method the route does not
 * take (405, with {@code Allow}), a body type it does not read (415).
 * <p>
 * Each answer is one log line and nothing else: a server fault (5xx) at ERROR, which error reporting turns into an
 * event, anything else at INFO. A line names the method, the matched route pattern (never the raw path), the
 * status, the code, the exception types and the request id; never an exception message, a body or a value.
 */
@Component
public class V2ProblemResolver implements HandlerExceptionResolver, Ordered {

    /** The pattern of the INFO line for an answered client error. */
    public static final String ANSWERED_LINE = "v2 request {} {} answered {} ({}): {}, request {}";

    /**
     * The pattern of the ERROR line for a server fault. Its arguments are ExceptionTranslator's, in the same order
     * (method, route, status, code, failure shape, request id), so GlitchTip groups v2 faults by the same rule.
     */
    public static final String FAULT_LINE = "v2 request {} {} failed with {} ({}): {}, request {}";

    private static final Logger log = LoggerFactory.getLogger(V2ProblemResolver.class);

    private final ProblemWriter writer;

    public V2ProblemResolver(ProblemWriter writer) {
        this.writer = writer;
    }

    /** Before Spring's resolvers and the application's controller advice; after the error attributes are noted. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
                                         Exception ex) {
        if (!ApiV2.isV2(request)) {
            return null;
        }
        Answer answer = answer(ex);
        Problem problem = writer.problem(request, response, answer.code(), answer.status(), answer.detail(),
            answer.errors(), answer.extensions());
        log(request, problem, ex);
        if (!response.isCommitted()) {
            try {
                writer.write(response, problem, answer.headers());
            } catch (IOException e) {
                log.warn("v2 request {} {}: the problem answer could not be written ({})",
                    request.getMethod(), route(request), e.getClass().getSimpleName());
            }
        }
        return new ModelAndView();
    }

    /** What a v2 request is answered for an exception. */
    record Answer(ProblemCode code, int status, String detail, List<ProblemError> errors,
                  Map<String, Object> extensions, HttpHeaders headers) {

        static Answer of(ProblemCode code, String detail) {
            return new Answer(code, code.status(), detail, List.of(), Map.of(), new HttpHeaders());
        }

        static Answer of(ProblemCode code, String detail, ProblemError error) {
            return new Answer(code, code.status(), detail, List.of(error), Map.of(), new HttpHeaders());
        }

        Answer withHeader(String name, String value) {
            HttpHeaders copy = new HttpHeaders();
            copy.putAll(headers);
            copy.add(name, value);
            return new Answer(code, status, detail, errors, extensions, copy);
        }
    }

    static Answer answer(Exception ex) {
        if (ex instanceof ApiProblemException problem) {
            return new Answer(problem.code(), problem.code().status(), problem.detail(), problem.errors(),
                problem.extensions(), problem.headers());
        }
        if (ex instanceof NotEnabledException off) {
            boolean method = off.scope() == NotEnabledException.Scope.METHOD;
            return Answer.of(method ? ProblemCode.METHOD_NOT_ENABLED : ProblemCode.OPERATION_NOT_ENABLED,
                "This " + (method ? "method" : "operation") + " is switched off on this server (activation switch "
                    + off.switchName() + ").");
        }
        if (ex instanceof HttpMessageNotReadableException unreadable) {
            return unreadable(unreadable);
        }
        if (ex instanceof MethodArgumentNotValidException invalid) {
            return invalidBody(invalid);
        }
        if (ex instanceof HandlerMethodValidationException invalid) {
            return invalidParameters(invalid);
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return Answer.of(ProblemCode.BAD_REQUEST, "A parameter does not have the expected format.",
                ProblemError.atParameter(mismatch.getName(), "invalid-format", "Expected " + jsonType(mismatch.getRequiredType()) + "."));
        }
        if (ex instanceof MissingServletRequestParameterException missing) {
            return Answer.of(ProblemCode.BAD_REQUEST, "A required parameter is missing.",
                ProblemError.atParameter(missing.getParameterName(), "missing", "This parameter is required."));
        }
        if (ex instanceof MissingRequestHeaderException missing) {
            return Answer.of(ProblemCode.BAD_REQUEST, "A required header is missing.",
                ProblemError.atHeader(missing.getHeaderName(), "missing", "This header is required."));
        }
        if (ex instanceof TypeMismatchException && !(ex instanceof ConversionNotSupportedException)) {
            return Answer.of(ProblemCode.BAD_REQUEST, "A parameter does not have the expected format.");
        }
        if (ex instanceof AccessDeniedException) {
            return Answer.of(ProblemCode.FORBIDDEN, "This account may not make this request.");
        }
        if (ex instanceof AuthenticationException) {
            return Answer.of(ProblemCode.UNAUTHENTICATED, UNAUTHENTICATED_DETAIL)
                .withHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        if (ex instanceof PessimisticLockingFailureException || ex instanceof OptimisticLockingFailureException
            || SqlStates.has(ex, SqlStates.LOCK_NOT_AVAILABLE)) {
            return Answer.of(ProblemCode.RESOURCE_BUSY,
                    "Another request is changing this resource; send the request again shortly.")
                .withHeader(HttpHeaders.RETRY_AFTER, "1");
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return Answer.of(ProblemCode.PAYLOAD_TOO_LARGE, "The request body exceeds the size this endpoint accepts.");
        }
        if (ex instanceof ErrorResponse response) {
            int status = response.getStatusCode().value();
            ProblemCode code = ProblemCode.forStatus(status);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(response.getHeaders());
            return new Answer(code, status, genericDetail(status), List.of(), Map.of(), headers);
        }
        ResponseStatus annotated = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        if (annotated != null) {
            int status = annotated.code().value();
            return new Answer(ProblemCode.forStatus(status), status, genericDetail(status), List.of(), Map.of(),
                new HttpHeaders());
        }
        return Answer.of(ProblemCode.INTERNAL_ERROR, genericDetail(500));
    }

    static final String UNAUTHENTICATED_DETAIL =
        "Send a bearer token from POST /api/v1/authenticate in the Authorization header.";

    private static String genericDetail(int status) {
        return switch (status) {
            case 401 -> UNAUTHENTICATED_DETAIL;
            case 403 -> "This account may not make this request.";
            case 404 -> "There is no resource at this path.";
            case 405 -> "This method is not allowed here; the Allow header lists the methods that are.";
            case 406 -> "None of the media types in the Accept header can be produced here.";
            case 413 -> "The request body exceeds the size this endpoint accepts.";
            case 415 -> "The body's media type is not read here; the Accept header lists those that are.";
            case 503 -> "The service is temporarily unavailable; try again shortly.";
            default -> status >= 500
                ? "The server failed to process the request; quote the instance when reporting it."
                : "The request was not processed.";
        };
    }

    /** A body that is not JSON, or JSON whose value at some place has the wrong type, with that place as a pointer. */
    private static Answer unreadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            List<String> segments = new ArrayList<>();
            for (JsonMappingException.Reference reference : mapping.getPath()) {
                segments.add(reference.getFieldName() != null
                    ? reference.getFieldName() : String.valueOf(reference.getIndex()));
            }
            String pointer = ProblemError.pointer(segments);
            if (mapping instanceof UnrecognizedPropertyException) {
                return Answer.of(ProblemCode.BAD_REQUEST, "The request body has a member this endpoint does not take.",
                    ProblemError.atPointer(pointer, "unknown-member", "This member is not part of the request."));
            }
            Class<?> target = mapping instanceof InvalidFormatException format ? format.getTargetType()
                : mapping instanceof MismatchedInputException mismatched ? mismatched.getTargetType() : null;
            return Answer.of(ProblemCode.BAD_REQUEST, "A value in the request body does not have the expected type.",
                ProblemError.atPointer(pointer, "invalid-type", "Expected " + jsonType(target) + "."));
        }
        if (cause instanceof JsonParseException) {
            return Answer.of(ProblemCode.BAD_REQUEST, "The request body is not valid JSON.");
        }
        return Answer.of(ProblemCode.BAD_REQUEST, "The request body is missing or cannot be read.");
    }

    /**
     * Bean validation of a body (a pointer per finding, 422) or of parameters bound from the query string (a
     * parameter locator per finding, 400). Each finding's text is fixed by its constraint: a constraint's own
     * message, or a binding failure's, can quote the value and Java type names.
     */
    private static Answer invalidBody(MethodArgumentNotValidException ex) {
        boolean body = ex.getParameter().hasParameterAnnotation(RequestBody.class);
        List<ProblemError> errors = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String code = kebab(error.getCode());
            String field = error instanceof FieldError fieldError ? fieldError.getField() : null;
            errors.add(body
                ? ProblemError.atPointer(field == null ? "#" : fieldPointer(field), code, fixedText(code))
                : ProblemError.atParameter(field, code, fixedText(code)));
        }
        return body
            ? new Answer(ProblemCode.VALIDATION_FAILED, ProblemCode.VALIDATION_FAILED.status(),
                "The request body has values that are not valid; see errors.", errors, Map.of(), new HttpHeaders())
            : new Answer(ProblemCode.BAD_REQUEST, ProblemCode.BAD_REQUEST.status(),
                "Parameters have values that are not valid; see errors.", errors, Map.of(), new HttpHeaders());
    }

    private static Answer invalidParameters(HandlerMethodValidationException ex) {
        List<ProblemError> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                String code = kebab(error.getCodes() != null && error.getCodes().length > 0
                    ? lastSegment(error.getCodes()[error.getCodes().length - 1]) : null);
                errors.add(ProblemError.atParameter(name, code, fixedText(code)));
            });
        });
        return new Answer(ProblemCode.BAD_REQUEST, ProblemCode.BAD_REQUEST.status(),
            "Parameters have values that are not valid; see errors.", errors, Map.of(), new HttpHeaders());
    }

    /** What each constraint's finding says, in fixed words: never the value, never a type name. */
    private static final Map<String, String> FIXED_TEXT = Map.ofEntries(
        Map.entry("not-null", "A value is required."),
        Map.entry("not-blank", "A value with text is required."),
        Map.entry("not-empty", "A non-empty value is required."),
        Map.entry("size", "The value's size is outside the allowed range."),
        Map.entry("min", "The value is below the minimum."),
        Map.entry("max", "The value is above the maximum."),
        Map.entry("decimal-min", "The value is below the minimum."),
        Map.entry("decimal-max", "The value is above the maximum."),
        Map.entry("positive", "The value must be above zero."),
        Map.entry("positive-or-zero", "The value must be zero or above."),
        Map.entry("negative", "The value must be below zero."),
        Map.entry("negative-or-zero", "The value must be zero or below."),
        Map.entry("digits", "The value has too many digits."),
        Map.entry("pattern", "The value does not have the required form."),
        Map.entry("email", "The value is not an email address."),
        Map.entry("past", "The time must be in the past."),
        Map.entry("future", "The time must be in the future."),
        Map.entry("type-mismatch", "The value does not have the expected type."),
        Map.entry("missing", "This value is required."));

    static String fixedText(String code) {
        return FIXED_TEXT.getOrDefault(code, "The value is not valid.");
    }

    /** {@code operations[1].value} as {@code #/operations/1/value}. */
    static String fieldPointer(String field) {
        List<String> segments = new ArrayList<>();
        for (String part : field.split("\\.")) {
            int bracket = part.indexOf('[');
            if (bracket < 0) {
                segments.add(part);
                continue;
            }
            if (bracket > 0) {
                segments.add(part.substring(0, bracket));
            }
            for (String index : part.substring(bracket).split("[\\[\\]]")) {
                if (!index.isEmpty()) {
                    segments.add(index);
                }
            }
        }
        return ProblemError.pointer(segments);
    }

    /** {@code NotBlank} as {@code not-blank}; a missing code as {@code invalid}. */
    static String kebab(String code) {
        if (code == null || code.isBlank()) {
            return "invalid";
        }
        return code.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

    private static String lastSegment(String code) {
        int dot = code.lastIndexOf('.');
        return dot < 0 ? code : code.substring(dot + 1);
    }

    /** A Java type described in JSON's terms, so no class name reaches the client. */
    static String jsonType(Class<?> type) {
        if (type == null) {
            return "another type";
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class && type != char.class)) {
            return "a number";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "true or false";
        }
        if (CharSequence.class.isAssignableFrom(type) || type == Character.class || type == char.class
            || type.isEnum() || java.util.UUID.class == type || java.time.temporal.Temporal.class.isAssignableFrom(type)) {
            return "a string";
        }
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return "an array";
        }
        return "an object";
    }

    private static void log(HttpServletRequest request, Problem problem, Exception ex) {
        String requestId = problem.getInstance() == null ? null
            : problem.getInstance().toString().substring("urn:uuid:".length());
        if (problem.getStatus() >= 500) {
            log.error(FAULT_LINE, request.getMethod(), route(request), problem.getStatus(), problem.getCode(),
                FailureShape.of(ex), requestId);
        } else {
            log.info(ANSWERED_LINE, request.getMethod(), route(request), problem.getStatus(), problem.getCode(),
                types(ex), requestId);
        }
    }

    /** The matched route pattern ({@code /api/v2/things/{id}}), never the raw path, which can hold a value. */
    static String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern.toString() : "(no route matched)";
    }

    /** For example {@code HttpMessageNotReadableException < JsonParseException}. */
    static String types(Throwable ex) {
        StringJoiner types = new StringJoiner(" < ");
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = ex; cause != null && seen.add(cause); cause = cause.getCause()) {
            types.add(cause.getClass().getSimpleName());
        }
        return types.toString();
    }

}
