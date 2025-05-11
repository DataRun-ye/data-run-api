package org.nmcpye.datarun.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmissionTransformer {

    private final Map<String, Object> mappingDictionary;

    public SubmissionTransformer(Map<String, Object> mappingDictionary) {
        this.mappingDictionary = mappingDictionary;
    }

    /**
     * Transforms a v1 submission (represented as a nested Map) into a v2 submission.
     */
    public Map<String, Object> transform(Map<String, Object> v1Submission) {
        Map<String, Object> v2Submission = new HashMap<>();
        transformRecursive(v1Submission, mappingDictionary, v2Submission);
        return v2Submission;
    }

    /**
     * Recursively applies the mapping dictionary to build the target map.
     */
    @SuppressWarnings("unchecked")
    private void transformRecursive(Map<String, Object> source,
                                    Map<String, Object> mappingDict,
                                    Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : mappingDict.entrySet()) {
            String v2Key = entry.getKey();
            Object mappingValue = entry.getValue();

            if (mappingValue instanceof String) {
                // Simple field mapping
                String v1Path = (String) mappingValue;
                Object value = getValueByPath(source, v1Path);
                if (value != null) {
                    target.put(v2Key, value);
                }
            } else if (mappingValue instanceof Map) {
                Map<String, Object> nestedMapping = (Map<String, Object>) mappingValue;
                // Check if this mapping defines a repeatable section.
                if (Boolean.TRUE.equals(nestedMapping.get("repeat"))) {
                    String v1RepeatKey = (String) nestedMapping.get("v1RepeatKey");
                    Object repeatObject = getValueByPath(source, v1RepeatKey);
                    if (repeatObject instanceof List) {
                        List<?> v1List = (List<?>) repeatObject;
                        List<Object> v2List = new ArrayList<>();
                        // Get the sub-mapping for each element
                        Map<String, Object> subMapping = (Map<String, Object>) nestedMapping.get("mapping");
                        for (Object item : v1List) {
                            if (item instanceof Map) {
                                Map<String, Object> v2Item = new HashMap<>();
                                transformRecursive((Map<String, Object>) item, subMapping, v2Item);
                                v2List.add(v2Item);
                            }
                        }
                        target.put(v2Key, v2List);
                    }
                } else {
                    // Handle nested non-repeatable section
                    Map<String, Object> nestedTarget = new HashMap<>();
                    transformRecursive(source, nestedMapping, nestedTarget);
                    target.put(v2Key, nestedTarget);
                }
            }
        }
    }

    /**
     * Retrieves a value from a nested map based on a dot-separated path.
     */
    @SuppressWarnings("unchecked")
    private Object getValueByPath(Map<String, Object> source, String path) {
        String[] parts = path.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
