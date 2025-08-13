package org.nmcpye.datarun.jpa.etl.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada 10/08/2025 (7amza.it@gmail.com)
 */
public final class FormDataUtils {
    // extract list at materialized repeatPath (dot delim), like "adult.adultClassification"
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getRepeatList(Map<String, Object> formData, String repeatPath) {
        Object val = getRawByPath(formData, repeatPath);
        if (val == null) return Collections.emptyList();
        if (!(val instanceof List)) throw new IllegalArgumentException("Expected list at path: " + repeatPath);
        return ((List<?>) val).stream()
            .map(item -> {
                if (item instanceof Map) return (Map<String, Object>) item;
                else throw new IllegalArgumentException("Repeat list item is not an object at " + repeatPath);
            })
            .collect(Collectors.toList());
    }

    // existing getRawByPath adapted to Map<String,Object>
    public static Object getRawByPath(Map<String, Object> data, String path) {
        String[] segments = path.split("\\.");
        Object current = data;
        for (String seg : segments) {
            if (current == null) return null;
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(seg);
            } else {
                return null;
            }
        }
        return current;
    }

    // get child value inside repeat item by childRelativePath (e.g., "UxA1u3cSLFQ")
    public static Object getValueFromRepeatItem(Map<String, Object> item, String childRelativePath) {
        return getRawByPath(item, childRelativePath);
    }
}
