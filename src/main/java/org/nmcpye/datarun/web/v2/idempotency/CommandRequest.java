package org.nmcpye.datarun.web.v2.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.IfMatch;

import java.util.UUID;

/**
 * A request to change a versioned resource, as its handler takes it: the caller, the {@code Idempotency-Key} and
 * the {@code If-Match} precondition, all checked before the handler runs (401, 400, 428). Declaring this parameter
 * makes a handler a command; the OpenAPI document then lists both headers and the 412 and 428 answers. Run it with
 * {@link IdempotentCommands#execute}.
 * <p>
 * A dry run of the same change takes the same parameter, so it sends the same headers and they are checked the
 * same way, but it never calls {@code execute}: it claims nothing, and the change itself may follow with the same
 * key.
 */
public final class CommandRequest extends IdempotentRequest {

    /** Where {@link CommandBodyCapture} leaves the request's body as received. */
    static final String BODY_ATTRIBUTE = IdempotentRequest.BODY_ATTRIBUTE;

    private final IfMatch ifMatch;

    CommandRequest(ApiCaller caller, UUID idempotencyKey, IfMatch ifMatch, HttpServletRequest request) {
        super(caller, idempotencyKey, request);
        this.ifMatch = ifMatch;
    }

    public IfMatch ifMatch() {
        return ifMatch;
    }

    @Override
    String precondition() {
        return ifMatch.canonical();
    }
}
