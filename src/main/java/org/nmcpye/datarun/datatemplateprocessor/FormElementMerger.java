//package org.nmcpye.datarun.templateprocessor;
//
//import org.nmcpye.datarun.templateelementconf.AbstractElement;
//import org.nmcpye.datarun.templateelementconf.FormSectionConf;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class FormElementMerger {
//    /**
//     * Merges fields and sections into a single, flat list ordered according to
//     * the parent-child relationships and the 'order' attribute.
//     *
//     * @param fields   The collection of fields (FormDataElementConf)
//     * @param sections The collection of sections (FormSectionConf)
//     * @return A flat list of FormElementConf with parents preceding their children.
//     */
//    public static List<AbstractElement> mergeElements(
//        Collection<? extends AbstractElement> fields,
//        Collection<FormSectionConf> sections) {
//
//        // Combine both fields and sections into a single list.
//        List<AbstractElement> allElements = new ArrayList<>();
//        if (fields != null) {
//            allElements.addAll(fields);
//        }
//        if (sections != null) {
//            allElements.addAll(sections);
//        }
//
//        // Build a set of all element IDs.
//        Set<String> allIds = allElements.stream()
//            .map(AbstractElement::getId)
//            .collect(Collectors.toSet());
//
//        // Build a map from parent ID to children.
//        Map<String, List<AbstractElement>> childrenMap = new HashMap<>();
//        List<AbstractElement> roots = new ArrayList<>();
//
//        for (AbstractElement element : allElements) {
//            String parentId = element.getParent();
//            // If no parent or parent is not found in our overall set, treat as root.
//            if (parentId == null || parentId.trim().isEmpty() || !allIds.contains(parentId)) {
//                roots.add(element);
//            } else {
//                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(element);
//            }
//        }
//
//        // Define a comparator to sort by the 'order' property.
//        Comparator<AbstractElement> orderComparator = Comparator.comparingInt(AbstractElement::getOrder);
//
//        // Sort roots and all children lists.
//        roots.sort(orderComparator);
//        for (List<AbstractElement> list : childrenMap.values()) {
//            list.sort(orderComparator);
//        }
//
//        // Now, traverse the hierarchy and build the merged list.
//        List<AbstractElement> merged = new ArrayList<>();
//        for (AbstractElement root : roots) {
//            traverse(root, childrenMap, merged);
//        }
//        return merged;
//    }
//
//    /**
//     * Recursive traversal (pre-order) to add an element and then its children.
//     */
//    private static void traverse(AbstractElement element,
//                                 Map<String, List<AbstractElement>> childrenMap,
//                                 List<AbstractElement> merged) {
//        merged.add(element);
//        List<AbstractElement> children = childrenMap.get(element.getId());
//        if (children != null) {
//            for (AbstractElement child : children) {
//                traverse(child, childrenMap, merged);
//            }
//        }
//    }
//}
