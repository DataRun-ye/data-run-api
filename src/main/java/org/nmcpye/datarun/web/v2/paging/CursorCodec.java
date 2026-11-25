package org.nmcpye.datarun.web.v2.paging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Encodes and decodes v2 cursors: an opaque token, base64url without padding, holding a format version, the query
 * binding ({@link QueryBinding}) and the keyset position ({@link Cursor}).
 * <p>
 * Clients pass a cursor back unchanged. A cursor is bound to its query: one decoded with another query's binding
 * is refused, as is anything that is not a cursor this codec wrote. Refusals are 400 {@code invalid-cursor} and
 * never repeat the token. The token is not signed: it holds only sort keys, which grant nothing.
 */
public final class CursorCodec {

    /** The longest token decoded; anything longer is refused unread. */
    public static final int MAX_LENGTH = 1024;

    private static final int FORMAT = 1;
    private static final int MAX_KEYS = 8;
    private static final ObjectMapper JSON = new ObjectMapper();

    private CursorCodec() {
    }

    /**
     * @param binding the query's binding
     * @param keys    the last item's sort key parts: {@link Long}, {@link Integer}, {@link String} or
     *                {@link Instant}
     */
    public static String encode(String binding, List<?> keys) {
        if (keys.isEmpty() || keys.size() > MAX_KEYS) {
            throw new IllegalArgumentException("A cursor holds 1 to " + MAX_KEYS + " sort key parts");
        }
        ObjectNode root = JSON.createObjectNode();
        root.put("v", FORMAT);
        root.put("b", binding);
        ArrayNode parts = root.putArray("k");
        for (Object key : keys) {
            if (key instanceof Long || key instanceof Integer) {
                parts.add(((Number) key).longValue());
            } else if (key instanceof String text) {
                parts.add(text);
            } else if (key instanceof Instant instant) {
                parts.add(instant.toString());
            } else {
                throw new IllegalArgumentException("A cursor key part is a whole number, a string or an instant");
            }
        }
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsBytes(root));
        } catch (IOException e) {
            throw new IllegalStateException("A cursor could not be written", e);
        }
    }

    /**
     * @throws ApiProblemException 400 {@code invalid-cursor} when the token is not a cursor of this query
     */
    public static Cursor decode(String token, String binding) {
        if (token == null || token.isEmpty() || token.length() > MAX_LENGTH) {
            throw invalid();
        }
        JsonNode root;
        try {
            root = JSON.readTree(new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IOException e) {
            throw invalid();
        }
        if (root == null || !root.isObject() || root.path("v").asInt(-1) != FORMAT
            || !root.path("b").isTextual() || !root.path("b").asText().equals(binding)
            || !root.path("k").isArray() || root.path("k").isEmpty() || root.path("k").size() > MAX_KEYS) {
            throw invalid();
        }
        List<Object> keys = new ArrayList<>();
        for (JsonNode part : root.get("k")) {
            if (part.isIntegralNumber() && part.canConvertToLong()) {
                keys.add(part.longValue());
            } else if (part.isTextual()) {
                keys.add(part.asText());
            } else {
                throw invalid();
            }
        }
        return new Cursor(keys);
    }

    static ApiProblemException invalid() {
        return ApiProblemException.of(ProblemCode.INVALID_CURSOR)
            .detail("The cursor is not one this list returned for this query; start again without a cursor, or "
                + "send the next value exactly as received with the same filters.")
            .error(ProblemError.atParameter(PageQuery.CURSOR, "invalid", null));
    }
}
