package org.nmcpye.datarun.analytics.pivot.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Simple, safe alias sanitizer for user-supplied SQL aliases.
 * - replaces disallowed chars with '_'
 * - lower-cases
 * - ensures alias does not start with a digit (prefixes 'a_' if so)
 * - truncates to max length
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
public final class AliasSanitizer {

    private static final Pattern INVALID = Pattern.compile("[^A-Za-z0-9_]");
    private static final int MAX_LEN = 64;

    public static String sanitize(String raw) {
        if (raw == null) return "m_measure";

        String s = raw.trim();
        if (s.isEmpty()) return "m_measure";

        // Replace disallowed characters with underscore
        s = INVALID.matcher(s).replaceAll("_");

        // Normalize: lowercase, collapse repeated underscores
        s = s.toLowerCase(Locale.ROOT).replaceAll("_+", "_");

        // Trim underscores from ends
        s = s.replaceAll("^_+", "").replaceAll("_+$", "");
        if (s.isEmpty()) s = "m";

        // Ensure not start with digit -> prefix letter
        if (Character.isDigit(s.charAt(0))) {
            s = "m_" + s;
        }

        // Truncate to MAX_LEN
        if (s.length() > MAX_LEN) {
            s = s.substring(0, MAX_LEN);
        }

        return s;
    }
}
