package org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess;


import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.dataelement.DataElement;
import org.nmcpye.datarun.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.templateprocessor.FormElementProcessor;
import org.nmcpye.datarun.templateprocessor.validation.DefaultFormTemplateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada, 19/03/2025
 */
@Component
@Transactional(readOnly = true)
public class FormTemplateProcessor {
    protected final Logger log = LoggerFactory.getLogger(FormTemplateProcessor.class);

    private final DataElementRepository dataElementRepository;

    private final DefaultFormTemplateValidator validator;

    public FormTemplateProcessor(DataElementRepository dataElementRepository,
                                 DefaultFormTemplateValidator validator) {

        this.dataElementRepository = dataElementRepository;
        this.validator = validator;
    }

    public <T extends FormWithFields> T validate(T formTemplate) {
        log.debug("start validating form template {}", formTemplate.getUid());

        validator.validate(formTemplate);
        return formTemplate;
    }

    public <T extends FormWithFields> FormWithFields processMetadata(T formTemplate) {
        log.debug("start processing form template's metadata {}", formTemplate.getUid());

        final var fieldUids = formTemplate.getFields().stream()
            .map(FormDataElementConf::getId).toList();
        final var dataElements = dataElementRepository.findAllByUidIn(fieldUids);
        validateElementsDataElement(formTemplate, dataElements);

        return new FormElementProcessor(formTemplate).process(dataElements)
            .get();
    }

    private <T extends FormWithFields> void validateElementsDataElement(T formTemplate, Collection<DataElement> dataElements) {
        final var fieldUids = formTemplate.getFields().stream()
            .map(FormDataElementConf::getId).toList();
        final var dataElementUids = getUids(dataElements);
        final var notFoundElementUids = fieldUids.stream()
            .filter(f -> !dataElementUids.contains(f))
            .collect(Collectors.toSet());

        if (!notFoundElementUids.isEmpty()) {
            throw new IllegalQueryException(
                new ErrorMessage(ErrorCode.E1100, formTemplate.getUid(),
                    String.join(", ", String.join("\n", notFoundElementUids))));
        }
    }

    /**
     * Returns a list of uids for the given collection of IdentifiableObjects.
     *
     * @param objects the list of IdentifiableObjects.
     * @return a list of uids.
     */
    public static <T extends AuditableObject<?>> Collection<String> getUids(Collection<T> objects) {
        return objects != null ? objects.stream().filter(o -> o != null).map(o -> o.getUid())
            .collect(Collectors.toSet()) : null;
    }

}
