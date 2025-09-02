package org.nmcpye.datarun.analytics.pivot.util;

import org.nmcpye.datarun.analytics.pivot.model.ValidatedMeasure;

import java.util.*;
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

    /**
     * handles alias duplicates
     * when autoRenameAliases=true, rename,
     * when false throw {@link IllegalArgumentException} if alias duplicate exist.
     *
     * @param measures   measures to check
     * @param autoRename true to auto-rename, or throw
     * @return validated measures with duplicates auto-renamed
     */
    public static List<ValidatedMeasure> ensureUniqueAliasesWithRename(List<ValidatedMeasure> measures,
                                                                       boolean autoRename) throws IllegalArgumentException {
        if (measures == null || measures.isEmpty()) return measures;
        Map<String, Integer> counts = new HashMap<>();
        List<ValidatedMeasure> out = new ArrayList<>(measures.size());

        for (ValidatedMeasure vm : measures) {
            String alias = Objects.requireNonNull(vm.alias()).trim();
            String key = alias.toLowerCase(Locale.ROOT);
            int seen = counts.getOrDefault(key, 0);
            if (seen == 0) {
                counts.put(key, 1);
                out.add(vm);
            } else {
                if (!autoRename) {
                    throw new IllegalArgumentException("Duplicate measure alias: '" + alias + "'");
                }
                String newAlias = alias + "_" + seen;
                counts.put(key, seen + 1);
                // create a new ValidatedMeasure with new alias
                ValidatedMeasure renamed = new ValidatedMeasure(
                    vm.deUid(),
                    vm.etcUid(),
                    vm.aggregation(),
                    vm.targetField(),
                    vm.elementPredicate(),
                    newAlias,
                    vm.distinct(),
                    vm.optionUid(),
                    vm.effectiveMode()
                );
                out.add(renamed);
            }
        }
        return out;
    }
}
