package org.nmcpye.datarun.mongo.service.formcomfiguration;

import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.utils.PathUtil.buildPath;

public class FormElementMerger {


    /**
     * Merges two lists (fields and sections) into a single ordered list.
     * <p>
     * The ordering is such that each section is immediately followed by its child elements.
     * If a section has subsections, those subsections are processed recursively.
     * Fields and sections without a parent (i.e. root elements) are handled accordingly.
     *
     * @param dataFormTemplate source FormTemplate
     * @return Ordered List of FormElementConf
     */
    static public List<FormElementConf> mergeElements(DataFormTemplate dataFormTemplate) {
        // Combine both types into a single list.
        final List<FormElementConf> allElements = new LinkedList<>();
        if (dataFormTemplate.getFields() != null) {
            allElements.addAll(dataFormTemplate.getFields());
        }
        if (dataFormTemplate.getSections() != null) {
            allElements.addAll(dataFormTemplate.getSections());
        }

        // Build a lookup map for all elements by id.
        final Map<String, FormElementConf> elementById = allElements.stream()
            .collect(Collectors.toMap(FormElementConf::getId, e -> e));

        // Build a parent -> children mapping.
        final Map<String, List<FormElementConf>> childrenMap = new HashMap<>();
        final List<FormElementConf> roots = new LinkedList<>();

        for (FormElementConf element : allElements) {
            final String parentId = element.getParent();
            // If no parent is specified or parent isn't part of the unified list, treat as root.
            if (parentId == null || parentId.isEmpty() || !elementById.containsKey(parentId)) {
                roots.add(element);
            } else {
                childrenMap.computeIfAbsent(parentId, k -> new LinkedList<>()).add(element);
            }
        }

        // Prepare the result list.
        final List<FormElementConf> result = new LinkedList<>();
        // Use a set to avoid duplicate processing.
        final Set<String> visited = new HashSet<>();

        // Process root sections first.
        final List<FormElementConf> rootSections = roots.stream()
            .filter(e -> e instanceof FormSectionConf)
            .toList();
        for (FormElementConf rootSection : rootSections) {
            addElementWithChildren(rootSection, childrenMap, result, visited, dataFormTemplate);
        }
        // Then add any remaining root fields.
        final List<FormElementConf> rootFields = roots.stream()
            .filter(e -> !(e instanceof FormSectionConf))
            .toList();
        for (FormElementConf rootField : rootFields) {
            if (visited.add(rootField.getId())) { // add only if not visited
                result.add(rootField);
            }
        }

        return result;
    }

    /**
     * Recursively adds an element and its children to the result list.
     * For sections, it processes child sections first then fields.
     *
     * @param element     The current form element (section or field)
     * @param childrenMap Map of parent id to list of children
     * @param result      The ordered result list being built
     * @param visited     A set of already processed element IDs to avoid duplicates
     */
    static public void addElementWithChildren(FormElementConf element,
                                              Map<String, List<FormElementConf>> childrenMap,
                                              List<FormElementConf> result,
                                              Set<String> visited, DataFormTemplate dataFormTemplate) {
        element.setPath(buildPath(element, dataFormTemplate.getSections()));

        if (!visited.add(element.getId())) {
            // Already processed this element; skip to avoid duplicates.
            return;
        }

        result.add(element);

        // Retrieve children for the current element.
        final List<FormElementConf> children = childrenMap.get(element.getId());
        if (children == null || children.isEmpty()) {
            return;
        }

        // Process child sections first.
        final List<FormElementConf> childSections = children.stream()
            .filter(e -> e instanceof FormSectionConf)
            .toList();
        for (FormElementConf childSection : childSections) {
            addElementWithChildren(childSection, childrenMap, result, visited, dataFormTemplate);
        }

        // Then process child fields.
        final List<FormElementConf> childFields = children.stream()
            .filter(e -> !(e instanceof FormSectionConf))
            .toList();
        for (FormElementConf childField : childFields) {
            if (visited.add(childField.getId())) {
                result.add(childField);
            }
        }
    }
}
