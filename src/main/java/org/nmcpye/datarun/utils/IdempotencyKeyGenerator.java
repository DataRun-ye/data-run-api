package org.nmcpye.datarun.utils;


import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
public final class IdempotencyKeyGenerator {
    private IdempotencyKeyGenerator() {}

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(digest);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Domain-level key default: mappingUid + '|' + edvIdempotencyKey (+ optional parts)
     */
    public static String domainKey(String mappingUid, String edvIdempotencyKey, String optional) {
        String raw = String.join("|", mappingUid == null ? "" : mappingUid, edvIdempotencyKey == null ? "" : edvIdempotencyKey,
            optional == null ? "" : optional);
        return sha256Hex(raw);
    }

    /**
     * Structural edv key example: submissionUid|repeatInstanceId|semanticPath|elementUid
     */
    public static String edvStructuralKey(String submissionUid, String repeatInstanceId, String semanticPath, String elementUid) {
        String raw = String.join("|", submissionUid == null ? "" : submissionUid,
            repeatInstanceId == null ? "" : repeatInstanceId,
            semanticPath == null ? "" : semanticPath,
            elementUid == null ? "" : elementUid);
        return sha256Hex(raw);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
