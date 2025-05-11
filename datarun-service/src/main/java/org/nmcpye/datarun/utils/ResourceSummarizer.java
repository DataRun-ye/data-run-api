package org.nmcpye.datarun.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceSummarizer {
    public static Map<String, Double> sumNumericResources(Map<String, Object> formData) {
        Map<String, Double> subTotals = new HashMap<>();

        class ResourceSummarizerHelper {
            void sumResources(Map<String, Object> data) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        subTotals.put(entry.getKey(), subTotals.getOrDefault(entry.getKey(), 0.0) + ((Number) entry.getValue()).doubleValue());
                    } else if (entry.getValue() instanceof Map) {
                        sumResources((Map<String, Object>) entry.getValue());
                    } else if (entry.getValue() instanceof List) {
                        for (Object element : (List<?>) entry.getValue()) {
                            if (element instanceof Map) {
                                sumResources((Map<String, Object>) element);
                            }
                        }
                    }
                }
            }
        }

        new ResourceSummarizerHelper().sumResources(formData);
        return subTotals;
    }

    public static Map<String, Double> sumNumericResources2(Map<String, Object> formData) {
        Map<String, Double> subTotals = new HashMap<>();

        sumResources(formData, subTotals);

        return subTotals;
    }

    private static void sumResources(Map<String, Object> data, Map<String, Double> subTotals) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Number) {
                subTotals.put(entry.getKey(), subTotals.getOrDefault(entry.getKey(), 0.0) + ((Number) entry.getValue()).doubleValue());
            } else if (entry.getValue() instanceof Map) {
                sumResources((Map<String, Object>) entry.getValue(), subTotals);
            } else if (entry.getValue() instanceof List) {
                for (Object element : (List<?>) entry.getValue()) {
                    if (element instanceof Map) {
                        sumResources((Map<String, Object>) element, subTotals);
                    }
                }
            }
        }
    }
}
