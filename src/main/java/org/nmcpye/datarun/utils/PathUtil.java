package org.nmcpye.datarun.utils;

public class PathUtil {
    public static String getDirectParent(String path) {
        if (path == null || !path.contains(".")) {
            return null;
        }

        String[] parts = path.split("\\.");

        // Get the direct parent
        return parts.length > 1 ? parts[parts.length - 2] : null;
    }

    public static String replaceLastElement(String path, String newElement) {
        if (path == null || !path.contains(".")) {
            return newElement;
        }

        String[] parts = path.split("\\.");

        parts[parts.length - 1] = newElement;

        return String.join(".", parts);
    }

    public static String generatePath(String parentPath, String fieldName, Integer repeatIndex) {
        if (repeatIndex != null) {
            return parentPath + "[" + repeatIndex + "]." + fieldName;
        }
        return parentPath + "." + fieldName;
    }

    public static String[] parsePath(String path) {
        return path.split("\\.");
    }
}
