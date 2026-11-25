package org.nmcpye.datarun.web.v2.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;

import java.util.UUID;

/**
 * A request to create a resource that has no version yet (a note, a grant), as its handler takes it: the caller and
 * the {@code Idempotency-Key}, checked before the handler runs. It takes no {@code If-Match}, since nothing exists to
 * name a version of; one sent is refused, 400. Run it with {@link IdempotentCommands#create}.
 */
public final class CreateRequest extends IdempotentRequest {

    CreateRequest(ApiCaller caller, UUID idempotencyKey, HttpServletRequest request) {
        super(caller, idempotencyKey, request);
    }

    @Override
    String precondition() {
        return "";
    }
}
