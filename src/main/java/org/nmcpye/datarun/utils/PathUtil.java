package org.nmcpye.datarun.utils;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;

import java.util.ArrayList;
import java.util.List;

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

    public static String buildPath(FormElementConf element, List<FormSectionConf> sections) {
        if (element.getParent() != null && sections.stream().noneMatch((s) -> s.getId().equals(element.getParent()))) {
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1101, element.getParent(), element.getId()));
        }
        final List<String> pathParts = new ArrayList<>();
        pathParts.add(element.getId());
        FormSectionConf current = getSection(element.getParent(), sections);

        while (current != null) {
            pathParts.add(0, current.getId());
            current = current.getParent() != null ? getSection(element.getParent(), sections) : null;
        }

        return String.join(".", pathParts);
    }

    private static FormSectionConf getSection(String sectionId, List<FormSectionConf> sections) {
        return sections.stream()
            .filter((s) -> s.getId().equals(sectionId))
            .findFirst().orElse(null);
    }

//    public static FormSectionConf getSection(String sectionId, List<FormSectionConf> sections) {
//        final FormSectionConf section = sections.stream().filter((s) -> s.getId().equals(sectionId))
//            .findFirst().orElseThrow(() -> new NotFoundElementParentSection("Element parent section: " + sectionId + "Not in the list"));
//        return section;
//    }
}
