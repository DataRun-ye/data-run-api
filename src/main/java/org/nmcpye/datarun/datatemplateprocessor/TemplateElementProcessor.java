package org.nmcpye.datarun.datatemplateprocessor;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateprocessor.postprocessors.AbstractFormElementHandler;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersionInterface;

import java.util.*;
import java.util.stream.Collectors;

public class TemplateElementProcessor {
    private final DataTemplateVersionInterface formTemplate;
    final private Map<String, FormSectionConf> sectionMap;

    public TemplateElementProcessor(DataTemplateVersionInterface formTemplate) {
        this.formTemplate = formTemplate;
        this.sectionMap = formTemplate.getSections()
            .stream().collect(Collectors.toMap(AbstractElement::getName, s -> s));
    }

    /**
     * @return FormElementConfigService to run the next step of get the formTemplate
     */
    private TemplateElementProcessor configureAndValidateSections() {
        final var formSections = new HashSet<>(formTemplate.getSections());
        final var sections = formSections.stream()
            .map(AbstractFormElementHandler::processSection)
            .peek(s -> s.path(buildPath(s, sectionMap))).toList();

        formTemplate.sections(sections);
        return this;
    }

    /**
     * @param dataTemplateElements data elements corresponding to the fields, should be fetched from repository and passed
     * @return FormElementConfigService to run the next step of get the formTemplate
     */
    private TemplateElementProcessor configureAndValidateFields(Collection<DataElement> dataTemplateElements) {
        final var dataElementMap = dataTemplateElements.stream().collect(Collectors.toMap(DataElement::getUid, s -> s));
        final var sectionMap = getSectionMap();
        final var fields = formTemplate.getFields().stream().distinct()
            .map((f) ->
                AbstractFormElementHandler
                    .processElement(f,
                        Optional.ofNullable(dataElementMap.get(f.getId()))
                            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1100, formTemplate.getUid(),
                                f.getId()))))
            .peek(f -> f.path(buildPath(f, sectionMap)))
            .toList();

        formTemplate.fields(fields);
        return this;
    }

    public TemplateElementProcessor process(Collection<DataElement> dataTemplateElements) {
        return configureAndValidateSections().configureAndValidateFields(dataTemplateElements);
    }

    public DataTemplateVersionInterface get() {
        return formTemplate;
    }

    /**
     * Builds the full path for a form element based on its parent's chain.
     * If an element has no parent, its path is simply its ID.
     *
     * @param element    The form element (field or section) for which to build the path.
     * @param sectionMap The Map of all sections.
     * @return The computed path (e.g., "mainSection.householdnames.P1D5FiaiHFP").
     */
    private <T extends AbstractElement> String buildPath(T element, Map<String, FormSectionConf> sectionMap) {
        Deque<String> pathParts = new ArrayDeque<>();
        pathParts.push(getKey(element));

        Set<String> visited = new HashSet<>();

        String parentId = element.getParent();
        while (parentId != null) {
            if (!visited.add(parentId)) {
                // a cycle exist.
                throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1107, formTemplate.getUid(), parentId));

            }

            FormSectionConf parentSection = sectionMap.get(parentId);
            if (parentSection == null) {
                throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1101,
                    formTemplate.getUid(), getKey(element), parentId));
            }
            pathParts.push(getKey(parentSection));
            parentId = parentSection.getParent();
        }

        // Join the parts with '.' to form the path.
        return String.join(".", pathParts);
    }

    private String getKey(AbstractElement element) {
        if (element instanceof FormDataElementConf elementConf) {
            return elementConf.getId();
        } else {
            // is section
            return element.getName();
        }
    }

    private Map<String, FormSectionConf> getSectionMap() {
        return formTemplate.getSections()
            .stream().collect(Collectors.toMap(AbstractElement::getName, s -> s));
    }
}
