package org.nmcpye.datarun.jpa.datatemplategenerator;


import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author Hamza Assada
 * @since 09/09/2025
 */
public final class HashUtil {
    private HashUtil() {
    }

    /**
     * Deterministic long hash derived from SHA-256 first 8 bytes
     */
    public static long hashToLong(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.wrap(digest, 0, Long.BYTES);
            return buf.getLong();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute hash", e);
        }
    }


    /**
     * Deterministic hash derived from SHA-256 first 8 bytes
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute hash", ex);
        }
    }


    public static String canonicalKeys(CanonicalKeys canonicalKeys) {
        String raw = String.join("|",/* templateElement.getTemplateUid(),*/
            canonicalKeys.canonicalPath(),
            canonicalKeys.dataType().name(),
            canonicalKeys.semanticType() == null ? "" : canonicalKeys.semanticType().name(),
            canonicalKeys.optionSetUid() == null ? "" : canonicalKeys.optionSetUid(),
            canonicalKeys.cardinality());
        return sha256Hex(raw);
    }

//    /**
//     * Structural edv key example: submissionUid|repeatInstanceId|semanticPath|elementUid
//     */
//    public static String schemaFingerPrint(CanonicalKey canonicalKey) {
//        String raw = String.join("|", canonicalKey.templateUid(),
//            canonicalKey.canonicalPath(),
//            canonicalKey.dataType().name(),
//            canonicalKey.semanticType() == null ? "" : canonicalKey.semanticType().name(),
//            canonicalKey.optionSetUid() == null ? "" : canonicalKey.optionSetUid(),
//            canonicalKey.cardinality());
//        return sha256Hex(raw);
//    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
