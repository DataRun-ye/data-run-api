package org.nmcpye.datarun.utils;

import java.util.*;

public class FormSubmissionDataUtil {
    //    @SuppressWarnings("unchecked")
//    public static Map<String, Object> flattenSubmission(Map<String, Object> submission, String parentPath) {
//        Map<String, Object> flatMap = new HashMap<>();
//        for (Map.Entry<String, Object> entry : submission.entrySet()) {
//            String key = entry.getKey();
//            Object value = entry.getValue();
//            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
//
//            if (value instanceof Map) {
//                flatMap.putAll(flattenSubmission((Map<String, Object>) value, currentPath));
//            } else if (value instanceof List<?> list) {
//                for (int i = 0; i < list.size(); i++) {
//                    flatMap.putAll(flattenSubmission(Map.of(String.valueOf(i), list.get(i)), currentPath + "[" + i + "]"));
//                }
//            } else {
//                flatMap.put(currentPath, value);
//            }
//        }
//        return flatMap;
//    }
//    @SuppressWarnings("unchecked")
//    public static Map<String, Object> flattenSubmission(Map<String, Object> submission, String parentPath) {
//        Map<String, Object> flatMap = new HashMap<>();
//        for (Map.Entry<String, Object> entry : submission.entrySet()) {
//            String key = entry.getKey();
//            Object value = entry.getValue();
//            String currentPath = parentPath != null ? parentPath + "." + key : key;
//
//            if (value instanceof Map) {
//                flatMap.putAll(flattenSubmission((Map<String, Object>) value, currentPath));
//            } else if (value instanceof Iterable) {
//                List<?> list = (List<?>) value;
////                currentPath = parentPath != null ? parentPath : key;
//
////                int index = 0;
//                for (int i = 0; i < list.size(); i++) {
//                    if (list.get(i) instanceof Map) {
//                        flatMap.putAll(flattenSubmission((Map<String, Object>) list.get(i), currentPath + "[" + i + "]"));
//                    } else {
//                        flatMap.put(key + "[" + i + "]", list.get(i));
//                    }
////                    flatMap.putAll(flattenSubmission(Map.of(String.valueOf(i), list.get(i)), currentPath + "[" + i + "]"));
//                }
//            } else {
//                flatMap.put(currentPath, value);
//            }
//        }
//        return flatMap;
//    }

    ///

    public static Map<String, Object> flatten(Map<String, ?> map, boolean all, boolean sortedKeys) {
        Map<String, Object> flatMap = all ? flattenAll(map, null) : flattenSections(map, null);
        return sortedKeys ? new TreeMap<>(flatMap) : flatMap;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flattenAll(Map<String, ?> map, String parentKey) {
        Map<String, Object> flatMap = new HashMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = parentKey != null ? parentKey + "/" + entry.getKey() : entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatMap.putAll(flattenAll((Map<String, ?>) value, key));
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object listItem = list.get(i);
                    String listKey = key + "[" + i + "]";
                    if (listItem instanceof Map) {
                        flatMap.putAll(flattenAll((Map<String, Object>) listItem, listKey));
                    } else {
                        flatMap.put(listKey, listItem);
                    }
                }
            } else {
                flatMap.put(key, value);
            }
        }
        return flatMap;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flattenSections(Map<String, ?> map, String parentKey) {
        Map<String, Object> flatMap = new HashMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = parentKey != null ? parentKey + "/" + entry.getKey() : entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatMap.putAll(flattenSections((Map<String, ?>) value, key));
            } else if (entry.getValue() instanceof List) {
                List<?> list = (List<?>) value;
                List<Object> newList = new ArrayList<>();
                for (final var listItem : list) {
                    if (listItem instanceof Map) {
//                        flatMap.putAll(flattenSections((Map<String, Object>) listItem, listKey));
                        Map<String, Object> flattenedListItem = flattenSections((Map<String, ?>) listItem, key);
                        newList.add(flattenedListItem);
                    } else {
//                        flatMap.put(listKey, listItem);
                        newList.add(listItem);
                    }
                }
                flatMap.put(key, newList);
            } else {
                flatMap.put(key, value);
            }
        }
        return flatMap;
    }
}
