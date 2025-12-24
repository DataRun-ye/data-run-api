package org.nmcpye.datarun.jpa.datatemplategenerator;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;

public final class ColumnNameGenerator {

    private static final int PG_IDENTIFIER_MAX = 63;
    private static final String PREFIX = "_"; // safe prefix to ensure non-digit start
    private static final int SUFFIX_MAX_CHARS = 5; // length of short base36 hash suffix

    private ColumnNameGenerator(){}

    public static String toStableColumnName(String canonicalKey, String preferredName) {
        // canonicalKey must be stable (e.g. canonical_element_uid or deterministic UUID)
        if (canonicalKey == null) canonicalKey = "";
        String base = sanitize(preferredName == null ? canonicalKey : preferredName);
        // ensure base is not empty
        if (base.isEmpty()) base = "col";

        String hashSuffix = computeBase36ShortHash(canonicalKey, SUFFIX_MAX_CHARS);
        // compute allowed length for base
        int reserved = PREFIX.length() + 1 + hashSuffix.length(); // prefix + '_' + suffix
        int allowedBaseLen = PG_IDENTIFIER_MAX - reserved;
        if (allowedBaseLen <= 0) {
            // extreme case if suffix too long; fallback to prefix + suffix truncated
            String suffixTrunc = hashSuffix.substring(0, Math.max(1, PG_IDENTIFIER_MAX - PREFIX.length()));
            return (PREFIX + suffixTrunc).toLowerCase(Locale.ROOT);
        }
        String truncatedBase = base.length() <= allowedBaseLen ? base : base.substring(0, allowedBaseLen);
        String col = PREFIX + truncatedBase + "_" + hashSuffix;
        return col.toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        // normalize and remove diacritics
        String n = Normalizer.normalize(s, Normalizer.Form.NFKD);
        n = n.replaceAll("\\p{M}", "");          // remove combining marks
        n = n.toLowerCase(Locale.ROOT);
        // keep only ascii letters/digits/underscore, replace others with underscore
        n = n.replaceAll("[^a-z0-9]+", "_");
        // collapse underscores
        n = n.replaceAll("_+", "_");
        // trim leading/trailing underscores
        n = n.replaceAll("^_+|_+$", "");
        // ensure doesn't start with digit (if it does, prefix will handle it)
        if (n.matches("^[0-9].*")) {
            n = "_" + n;
        }
        return n;
    }

    private static String computeBase36ShortHash(String input, int maxChars) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            // take first 8 bytes as a long for stable-ish 64-bit fingerprint
            ByteBuffer bb = ByteBuffer.wrap(digest);
            long v = bb.getLong(); // signed but we'll treat it unsigned below
            String base36 = Long.toUnsignedString(v, 36);
            // base36 may be shorter/longer; truncate to maxChars (but keep at least 1 char)
            int len = Math.min(maxChars, base36.length());
            return base36.substring(0, len);
        } catch (Exception e) {
            // fallback deterministic fallback
            String fallback = Integer.toHexString(Math.abs((input == null ? "" : input).hashCode()));
            return fallback.substring(0, Math.min(maxChars, fallback.length()));
        }
    }
}

