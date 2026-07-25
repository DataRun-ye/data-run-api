package org.nmcpye.datarun.jpa.reference;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ReferenceDisplayNamePolicy {

    private static final Pattern VALID_CHARACTERS =
        Pattern.compile("^[\\p{L}\\s'’\\-\\u200C]+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> CONNECTORS = Set.of(
        "عبد", "بن", "ابن", "آل", "أبو", "ابو", "أم",
        "ibn", "bin", "binte", "binna", "de", "del", "da", "van", "von", "al");

    public String normalizeNewName(String rawName) {
        if (rawName == null || !VALID_CHARACTERS.matcher(rawName).matches()) {
            return null;
        }
        String normalized = WHITESPACE.matcher(rawName).replaceAll(" ").trim();
        if (normalized.isEmpty() || normalized.length() > 255) {
            return null;
        }

        String[] tokens = normalized.split(" ");
        List<String> parts = new ArrayList<>();
        for (int index = 0; index < tokens.length; index++) {
            String token = tokens[index];
            if (CONNECTORS.contains(token)
                || CONNECTORS.contains(token.toLowerCase(Locale.ROOT))) {
                if (index + 1 < tokens.length) {
                    parts.add(token + " " + tokens[++index]);
                } else {
                    parts.add(token);
                }
            } else {
                parts.add(token);
            }
        }

        long validParts = parts.stream()
            .filter(part -> WHITESPACE.matcher(part).replaceAll("").length() >= 2)
            .count();
        return validParts >= 4 ? normalized : null;
    }
}
