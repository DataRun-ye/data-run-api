package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The {@code Idempotency-Key} request header of v2 commands: a UUID chosen by the caller, one per intended change,
 * reused unchanged on every retry of that change (a tool gives each line of a sheet its own fixed key).
 * <p>
 * The IETF draft (draft-ietf-httpapi-idempotency-key-header-09) makes the field a Structured Field string, so the
 * canonical form is quoted, {@code Idempotency-Key: "8e03978e-40d5-43e8-bc93-6894a57f9324"}; the bare UUID most
 * tools send is accepted too. Upper and lower case are the same key. Anything else, including the nil and max
 * UUIDs, is refused.
 */
public final class IdempotencyKey {

    public static final String HEADER = "Idempotency-Key";

    private static final Pattern UUID_TEXT =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final UUID NIL = new UUID(0L, 0L);
    private static final UUID MAX = new UUID(-1L, -1L);

    private IdempotencyKey() {
    }

    /**
     * The key from the header's field lines.
     *
     * @throws ApiProblemException 400 {@code idempotency-key-missing} when absent, 400
     *                             {@code invalid-idempotency-key} when not exactly one UUID
     */
    public static UUID parse(List<String> fieldLines) {
        if (fieldLines == null || fieldLines.isEmpty()
            || (fieldLines.size() == 1 && fieldLines.get(0).isBlank())) {
            throw ApiProblemException.of(ProblemCode.IDEMPOTENCY_KEY_MISSING)
                .detail("Send Idempotency-Key with a UUID chosen for this change, and the same UUID on every retry "
                    + "of it.")
                .error(ProblemError.atHeader(HEADER, "missing", null));
        }
        if (fieldLines.size() > 1) {
            throw invalid();
        }
        String value = fieldLines.get(0).trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        if (!UUID_TEXT.matcher(value).matches()) {
            throw invalid();
        }
        UUID key = UUID.fromString(value);
        if (key.equals(NIL) || key.equals(MAX)) {
            throw invalid();
        }
        return key;
    }

    private static ApiProblemException invalid() {
        return ApiProblemException.of(ProblemCode.INVALID_IDEMPOTENCY_KEY)
            .detail("Idempotency-Key must be one UUID, such as \"8e03978e-40d5-43e8-bc93-6894a57f9324\".")
            .error(ProblemError.atHeader(HEADER, "invalid", null));
    }
}
