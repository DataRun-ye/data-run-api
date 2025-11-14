package org.nmcpye.datarun.jpa.datatemplategenerator;

import lombok.NoArgsConstructor;

/**
 * @author Hamza Assada
 * @since 09/09/2025
 */
@NoArgsConstructor
public final class PathUtils {
    /**
     * Replace last segment of idPath with fieldName (structural name, not display label).
     * If fieldName is null/blank, returns idPath (caller may fallback later).
     */
    public static String computeDataPath(String idPath, String fieldName) {
        if (idPath == null || idPath.isBlank()) return idPath;
        String[] parts = idPath.split("\\.", -1);
        if (parts.length == 0) return idPath;

        int last = parts.length - 1;
        String safeName = sanitizeSegment(fieldName);
        if (safeName == null || safeName.isEmpty()) {
            return idPath;
        }
        parts[last] = safeName;
        return String.join(".", parts);
    }

    private static String sanitizeSegment(String seg) {
        if (seg == null) return null;
        String s = seg.trim();
        if (s.isEmpty()) return s;
        // Simple sanitization: replace dot to avoid breaking path semantics.
        return s.replace(".", "·");
    }
}

