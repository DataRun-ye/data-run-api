package org.nmcpye.datarun.formtemplate;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.formtemplate.postprocessors.AbstractFormElementHandler;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;

import java.util.*;
import java.util.stream.Collectors;

public class FormElementProcessor {
    private final FormWithFields formTemplate;
    final private Map<String, FormSectionConf> sectionMap;

    public FormElementProcessor(FormWithFields formTemplate) {
        this.formTemplate = formTemplate;
        this.sectionMap = formTemplate.getSections()
            .stream().collect(Collectors.toMap(FormElementConf::getId, s -> s));
    }

    /**
     * @return FormElementConfigService to run the next step of get the formTemplate
     */
    private FormElementProcessor configureAndValidateSections() {
        final var formSections = new HashSet<>(formTemplate.getSections());
        final var sections = formSections.stream()
            .map(AbstractFormElementHandler::processSection)
            .peek(s -> s.path(buildPath(s, sectionMap))).toList();

        formTemplate.setSections(sections);
        return this;
    }

    /**
     * @param dataElements data elements corresponding to the fields, should be fetched from repository and passed
     * @return FormElementConfigService to run the next step of get the formTemplate
     */
    private FormElementProcessor configureAndValidateFields(Collection<DataElement> dataElements) {
        final var dataElementMap = dataElements.stream().collect(Collectors.toMap(DataElement::getUid, s -> s));
        final var sectionMap = getSectionMap();
        final var fields = formTemplate.getFields().stream().distinct()
            .map((f) ->
                AbstractFormElementHandler
                    .processElement(f, formTemplate,
                        Optional.ofNullable(dataElementMap.get(f.getId()))
                            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1100, formTemplate.getUid(), f.getId()))))
            .peek(f -> f.path(buildPath(f, sectionMap)))
            .toList();

        formTemplate.setFields(fields);
        return this;
    }

    public FormElementProcessor process(Collection<DataElement> dataElements) {
        return configureAndValidateSections().configureAndValidateFields(dataElements);
    }

    public FormWithFields get() {
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
    private String buildPath(FormElementConf element, Map<String, FormSectionConf> sectionMap) {
        Deque<String> pathParts = new ArrayDeque<>();
        pathParts.push(element.getId());

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
                    formTemplate.getUid(), element.getId(), parentId));
            }
            pathParts.push(parentSection.getId());
            parentId = parentSection.getParent();
        }

        // Join the parts with '.' to form the path.
        return String.join(".", pathParts);
    }

    private Map<String, FormSectionConf> getSectionMap() {
        return formTemplate.getSections()
            .stream().collect(Collectors.toMap(FormElementConf::getId, s -> s));
    }
}
