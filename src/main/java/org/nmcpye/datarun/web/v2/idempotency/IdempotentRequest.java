package org.nmcpye.datarun.web.v2.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * What every idempotent v2 request carries, checked before its handler runs: the caller, the
 * {@code Idempotency-Key}, and the request's fingerprint. A handler takes one of its two kinds:
 * {@link CommandRequest} to change a versioned resource (with {@code If-Match}), {@link CreateRequest} to create one
 * (without).
 */
public abstract sealed class IdempotentRequest permits CommandRequest, CreateRequest {

    /** Where {@link CommandBodyCapture} leaves the request's body as received. */
    static final String BODY_ATTRIBUTE = IdempotentRequest.class.getName() + ".body";

    private final ApiCaller caller;
    private final UUID idempotencyKey;
    private final HttpServletRequest request;
    private byte[] fingerprint;

    IdempotentRequest(ApiCaller caller, UUID idempotencyKey, HttpServletRequest request) {
        this.caller = caller;
        this.idempotencyKey = idempotencyKey;
        this.request = request;
    }

    public ApiCaller caller() {
        return caller;
    }

    public UUID idempotencyKey() {
        return idempotencyKey;
    }

    /** The precondition as the fingerprint covers it: the canonical {@code If-Match}, or nothing for a create. */
    abstract String precondition();

    /**
     * The request's fingerprint ({@link RequestFingerprint}), computed when first asked: by then the handler's
     * arguments, its body included, have been read.
     *
     * @throws IllegalStateException when the request carries a body that was not captured (the handler did not
     *                               read it as its {@code @RequestBody}): hashing it as empty would let a different
     *                               body replay the first outcome under the same key
     */
    byte[] fingerprint() {
        if (fingerprint == null) {
            Object body = request.getAttribute(BODY_ATTRIBUTE);
            if (body == null && carriesBody(request)) {
                throw new IllegalStateException("A command's body was sent but not read as its @RequestBody, so it "
                    + "cannot be part of its idempotency fingerprint");
            }
            String path = request.getRequestURI();
            String context = request.getContextPath();
            if (context != null && !context.isEmpty() && path.startsWith(context)) {
                path = path.substring(context.length());
            }
            // Decoded, so the same request sent with ':' or '%3A' is one request. An encoded '/' never gets here:
            // the security firewall refuses it.
            path = UriUtils.decode(path, StandardCharsets.UTF_8);
            fingerprint = RequestFingerprint.of(request.getMethod(), path, request.getQueryString(), precondition(),
                body instanceof byte[] bytes ? bytes : null);
        }
        return fingerprint.clone();
    }

    /** Whether the request has a body at all: a positive length, or a chunked one of unknown length. */
    static boolean carriesBody(HttpServletRequest request) {
        return request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null;
    }
}
