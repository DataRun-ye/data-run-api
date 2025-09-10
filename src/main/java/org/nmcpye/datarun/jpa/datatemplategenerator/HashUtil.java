package org.nmcpye.datarun.jpa.datatemplategenerator;


import java.nio.ByteBuffer;
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
}
