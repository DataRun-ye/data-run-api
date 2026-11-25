package org.nmcpye.datarun.web.v2.problem;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The one registry of v2 error codes. Every v2 error response is an RFC 9457 problem whose {@code code} is one of
 * these and whose {@code type} is {@link #type()}, derived from the code; its {@code title} is the code's title and
 * its status the code's status.
 * <p>
 * Codes are kebab-case and name the problem, not the remedy; the generic ones keep their HTTP status's name. A 409
 * never carries a bare {@code conflict} code. The list is open: a later module adds codes here, with the words of
 * its glossary, and clients ignore codes they do not know (falling back on the status). A code is never renamed,
 * repurposed or given another status once published; retiring one means no longer raising it.
 */
public enum ProblemCode {

    // 400: the request is malformed; resending it unchanged fails again.
    BAD_REQUEST("bad-request", 400, "The request is malformed"),
    INVALID_CURSOR("invalid-cursor", 400, "The cursor does not belong to this query"),
    IDEMPOTENCY_KEY_MISSING("idempotency-key-missing", 400, "The Idempotency-Key header is missing"),
    INVALID_IDEMPOTENCY_KEY("invalid-idempotency-key", 400, "The Idempotency-Key header is not a UUID"),
    INVALID_IF_MATCH("invalid-if-match", 400, "The If-Match header is not a list of entity tags"),

    UNAUTHENTICATED("unauthenticated", 401, "Authentication is required"),
    FORBIDDEN("forbidden", 403, "The request is not allowed for this account"),
    NOT_FOUND("not-found", 404, "The resource does not exist"),
    METHOD_NOT_ALLOWED("method-not-allowed", 405, "The method is not allowed on this resource"),
    NOT_ACCEPTABLE("not-acceptable", 406, "No acceptable representation is available"),

    // 409: the request may succeed when sent again later.
    REQUEST_IN_PROGRESS("request-in-progress", 409,
        "An earlier request with the same Idempotency-Key is still being processed"),
    RESOURCE_BUSY("resource-busy", 409, "The resource is being changed by another request"),

    PRECONDITION_FAILED("precondition-failed", 412, "The resource has changed since the version in If-Match"),
    PAYLOAD_TOO_LARGE("payload-too-large", 413, "The request body is too large"),
    UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", 415, "The request body's media type is not supported"),

    // 422: the request is well formed but cannot be applied.
    IDEMPOTENCY_KEY_REUSED("idempotency-key-reused", 422,
        "The Idempotency-Key was already used for a different request"),
    VALIDATION_FAILED("validation-failed", 422, "The request is not valid"),
    METHOD_NOT_ENABLED("method-not-enabled", 422, "The method is not enabled on this server"),
    OPERATION_NOT_ENABLED("operation-not-enabled", 422, "The operation is not enabled on this server"),

    PRECONDITION_REQUIRED("precondition-required", 428,
        "An If-Match header with the resource's entity tag is required"),

    INTERNAL_ERROR("internal-error", 500, "The server failed to process the request"),
    SERVICE_UNAVAILABLE("service-unavailable", 503, "The service is temporarily unavailable");

    /**
     * The prefix of every problem type URI. A tag URI (RFC 4151): a permanent identifier that names no host and
     * resolves nowhere, which RFC 9457 allows; the codes are documented in the OpenAPI document instead.
     */
    public static final String TYPE_PREFIX = "tag:nmcpye.org,2026:datarun/problem/";

    private static final Map<String, ProblemCode> BY_CODE =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(ProblemCode::code, Function.identity()));

    /** The code a status falls back on when no more specific code applies. */
    private static final Map<Integer, ProblemCode> GENERIC_BY_STATUS = Map.ofEntries(
        Map.entry(400, BAD_REQUEST), Map.entry(401, UNAUTHENTICATED), Map.entry(403, FORBIDDEN),
        Map.entry(404, NOT_FOUND), Map.entry(405, METHOD_NOT_ALLOWED), Map.entry(406, NOT_ACCEPTABLE),
        Map.entry(409, RESOURCE_BUSY), Map.entry(412, PRECONDITION_FAILED), Map.entry(413, PAYLOAD_TOO_LARGE),
        Map.entry(415, UNSUPPORTED_MEDIA_TYPE), Map.entry(422, VALIDATION_FAILED),
        Map.entry(428, PRECONDITION_REQUIRED), Map.entry(500, INTERNAL_ERROR), Map.entry(503, SERVICE_UNAVAILABLE));

    private final String code;
    private final int status;
    private final String title;

    ProblemCode(String code, int status, String title) {
        this.code = code;
        this.status = status;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public String title() {
        return title;
    }

    public URI type() {
        return URI.create(TYPE_PREFIX + code);
    }

    /** Every code, in declaration order: what the OpenAPI document lists. */
    public static List<String> codes() {
        return Arrays.stream(values()).map(ProblemCode::code).toList();
    }

    public static ProblemCode byCode(String code) {
        ProblemCode found = BY_CODE.get(code);
        if (found == null) {
            throw new IllegalArgumentException("No problem code " + code);
        }
        return found;
    }

    /**
     * The generic code for a status that no more specific code describes: the status's own code when the registry
     * has one, otherwise {@link #BAD_REQUEST} for any other client error and {@link #INTERNAL_ERROR} for any other
     * server error. The answer keeps the status it was given.
     */
    public static ProblemCode forStatus(int status) {
        ProblemCode generic = GENERIC_BY_STATUS.get(status);
        if (generic != null) {
            return generic;
        }
        return status >= 500 ? INTERNAL_ERROR : BAD_REQUEST;
    }
}
