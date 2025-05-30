package org.nmcpye.datarun.templateprocessor;

import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;

import java.util.*;
import java.util.stream.Collectors;

public class FormElementMerger {
    /**
     * Merges fields and sections into a single, flat list ordered according to
     * the parent-child relationships and the 'order' attribute.
     *
     * @param fields   The collection of fields (FormDataElementConf)
     * @param sections The collection of sections (FormSectionConf)
     * @return A flat list of FormElementConf with parents preceding their children.
     */
    public static List<FormElementConf> mergeElements(
        Collection<? extends FormElementConf> fields,
        Collection<FormSectionConf> sections) {

        // Combine both fields and sections into a single list.
        List<FormElementConf> allElements = new ArrayList<>();
        if (fields != null) {
            allElements.addAll(fields);
        }
        if (sections != null) {
            allElements.addAll(sections);
        }

        // Build a set of all element IDs.
        Set<String> allIds = allElements.stream()
            .map(FormElementConf::getId)
            .collect(Collectors.toSet());

        // Build a map from parent ID to children.
        Map<String, List<FormElementConf>> childrenMap = new HashMap<>();
        List<FormElementConf> roots = new ArrayList<>();

        for (FormElementConf element : allElements) {
            String parentId = element.getParent();
            // If no parent or parent is not found in our overall set, treat as root.
            if (parentId == null || parentId.trim().isEmpty() || !allIds.contains(parentId)) {
                roots.add(element);
            } else {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(element);
            }
        }

        // Define a comparator to sort by the 'order' property.
        Comparator<FormElementConf> orderComparator = Comparator.comparingInt(FormElementConf::getOrder);

        // Sort roots and all children lists.
        roots.sort(orderComparator);
        for (List<FormElementConf> list : childrenMap.values()) {
            list.sort(orderComparator);
        }

        // Now, traverse the hierarchy and build the merged list.
        List<FormElementConf> merged = new ArrayList<>();
        for (FormElementConf root : roots) {
            traverse(root, childrenMap, merged);
        }
        return merged;
    }

    /**
     * Recursive traversal (pre-order) to add an element and then its children.
     */
    private static void traverse(FormElementConf element,
                                 Map<String, List<FormElementConf>> childrenMap,
                                 List<FormElementConf> merged) {
        merged.add(element);
        List<FormElementConf> children = childrenMap.get(element.getId());
        if (children != null) {
            for (FormElementConf child : children) {
                traverse(child, childrenMap, merged);
            }
        }
    }
}
