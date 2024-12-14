package org.nmcpye.datarun.mongo.service.submissionmigration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonFlattener {

    // Method to flatten objects, but not flatten arrays themselves, only objects within arrays
    public static Map<String, Object> flatten(Map<String, Object> data) {
        return flattenMap(null, data, new HashMap<>());
    }

    private static Map<String, Object> flattenMap(String parentKey, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = parentKey != null ? parentKey + "/" + entry.getKey() : entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // Recursively flatten nested maps (objects)
                flattenMap(key, (Map<String, Object>) value, result);
            } else if (value instanceof Iterable) {
                // Keep arrays intact, but flatten objects within arrays
                result.put(key, flattenArray(null, (Iterable<?>) value));
            } else {
                // Add non-object value to result map
                result.put(key, value);
            }
        }
        return result;
    }

    private static Object flattenArray(String parentKey, Iterable<?> iterable) {
        List<Object> flattenedArray = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Map) {
                // Flatten objects inside the array and add them to the result array
                Map<String, Object> flattenedItem = new HashMap<>();
//                flattenedItem.putIfAbsent("formId", formId);
                flattenMap(parentKey, (Map<String, Object>) item, flattenedItem);
                flattenedArray.add(flattenedItem);
            } else if (item instanceof Iterable) {
                // Recursively flatten nested arrays (if necessary)
                flattenedArray.add(flattenArray(null, (Iterable<?>) item));
            } else {
                // Keep primitive or simple data types intact
                flattenedArray.add(item);
            }
        }
        return flattenedArray;
    }

    ///

    public static Map<String, Object> flattenIncludingArrays(Map<String, Object> data) {
        return flattenMapIncludingArrays(null, data, new HashMap<>());
    }

    private static Map<String, Object> flattenMapIncludingArrays(String parentKey, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = parentKey != null ? parentKey + "/" + entry.getKey() : entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // Recursively flatten objects
                flattenMapIncludingArrays(key, (Map<String, Object>) value, result);
            } else if (value instanceof Iterable) {
                // Keep arrays as is, but flatten objects inside arrays
                int index = 0;
                for (Object item : (Iterable<?>) value) {
                    if (item instanceof Map) {
                        // Flatten objects inside arrays
                        flattenMapIncludingArrays(key + "[" + index + "]", (Map<String, Object>) item, result);
                    } else {
                        result.put(key + "[" + index + "]", item);
                    }
                    index++;
                }
            } else {
                // Add non-object value to result map
                result.put(key, value);
            }
        }
        return result;
    }
}
