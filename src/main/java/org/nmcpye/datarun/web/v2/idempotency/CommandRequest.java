package org.nmcpye.datarun.web.v2.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.IfMatch;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A v2 command's request, as its handler takes it: the caller, the {@code Idempotency-Key} and the
 * {@code If-Match} precondition, all checked before the handler runs (401, 400, 428), and the request's fingerprint.
 * Declaring this parameter is what makes a handler a command: the OpenAPI document then lists both headers and the
 * 412 and 428 answers.
 */
public final class CommandRequest {

    /** Where {@link CommandBodyCapture} leaves the command's body as received. */
    static final String BODY_ATTRIBUTE = CommandRequest.class.getName() + ".body";

    private final ApiCaller caller;
    private final UUID idempotencyKey;
    private final IfMatch ifMatch;
    private final HttpServletRequest request;
    private byte[] fingerprint;

    CommandRequest(ApiCaller caller, UUID idempotencyKey, IfMatch ifMatch, HttpServletRequest request) {
        this.caller = caller;
        this.idempotencyKey = idempotencyKey;
        this.ifMatch = ifMatch;
        this.request = request;
    }

    public ApiCaller caller() {
        return caller;
    }

    public UUID idempotencyKey() {
        return idempotencyKey;
    }

    public IfMatch ifMatch() {
        return ifMatch;
    }

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
            fingerprint = RequestFingerprint.of(request.getMethod(), path, request.getQueryString(),
                ifMatch.canonical(), body instanceof byte[] bytes ? bytes : null);
        }
        return fingerprint.clone();
    }

    /** Whether the request has a body at all: a positive length, or a chunked one of unknown length. */
    static boolean carriesBody(HttpServletRequest request) {
        return request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null;
    }
}
