package org.nmcpye.datarun.web.v2.idempotency;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The SHA-256 by which a retry is recognized as the same request: a format version, the method, the decoded path, the
 * query parameters (decoded, ordered by name, each name's values in their order), the {@code If-Match}
 * precondition in canonical form, and the body in canonical form ({@link CanonicalJson}). Each part is
 * length-prefixed, so no two different requests run together into the same bytes. A body that is not JSON is
 * hashed as sent.
 * <p>
 * The key's owner is not part of it: keys are already per caller. Nothing of the request is stored but this digest.
 */
public final class RequestFingerprint {

    private static final String FORMAT = "datarun-v2-request/1";

    private RequestFingerprint() {
    }

    public static byte[] of(String method, String rawPath, String rawQuery, String ifMatch, byte[] body) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
        part(sha256, FORMAT.getBytes(StandardCharsets.UTF_8));
        part(sha256, method.getBytes(StandardCharsets.UTF_8));
        part(sha256, rawPath.getBytes(StandardCharsets.UTF_8));
        part(sha256, canonicalQuery(rawQuery).getBytes(StandardCharsets.UTF_8));
        part(sha256, (ifMatch == null ? "" : ifMatch).getBytes(StandardCharsets.UTF_8));
        part(sha256, canonicalBody(body));
        return sha256.digest();
    }

    /** {@code b=2&a=1&a=0} as {@code a=1&a=0&b=2}, names and values decoded then encoded one way. */
    static String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<Map.Entry<String, String>> pairs = new ArrayList<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            pairs.add(new AbstractMap.SimpleEntry<>(decode(name), decode(value)));
        }
        pairs.sort(Map.Entry.comparingByKey(Comparator.naturalOrder()));
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<String, String> pair : pairs) {
            if (!canonical.isEmpty()) {
                canonical.append('&');
            }
            canonical.append(pair.getKey().length()).append(':').append(pair.getKey())
                .append('=').append(pair.getValue().length()).append(':').append(pair.getValue());
        }
        return canonical.toString();
    }

    private static byte[] canonicalBody(byte[] body) {
        if (body == null || body.length == 0) {
            return new byte[0];
        }
        try {
            return CanonicalJson.canonicalize(body);
        } catch (IOException | IllegalArgumentException e) {
            return body;
        }
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    private static void part(MessageDigest digest, byte[] bytes) {
        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
