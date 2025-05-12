package org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess;


import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.repository.DataElementRepository;
import org.nmcpye.datarun.formtemplate.FormElementProcessor;
import org.nmcpye.datarun.formtemplate.validation.DefaultFormTemplateValidator;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormRepository;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada, 19/03/2025
 */
@Component
@Transactional(readOnly = true)
public class FormTemplateProcessor {
    protected final Logger log = LoggerFactory.getLogger(FormTemplateProcessor.class);

    private final DataElementRepository dataElementRepository;

    private final DataFormTemplateRepository dataFormTemplateRepository;

    private final DataFormRepository legacyFormRepository;

    private final DefaultFormTemplateValidator validator;

    public FormTemplateProcessor(DataElementRepository dataElementRepository,
                                 DataFormTemplateRepository dataFormTemplateRepository, DataFormRepository legacyFormRepository, DefaultFormTemplateValidator validator) {

        this.dataElementRepository = dataElementRepository;
        this.dataFormTemplateRepository = dataFormTemplateRepository;
        this.legacyFormRepository = legacyFormRepository;
        this.validator = validator;
    }

    public <T extends FormWithFields> T validate(T formTemplate) {
        log.debug("start validating form template {}", formTemplate.getUid());

        validator.validate(formTemplate);
        return formTemplate;
    }

    public <T extends FormWithFields> FormWithFields processMetadata(T formTemplate) {
        log.debug("start processing form template's metadata {}", formTemplate.getUid());

        final var fieldUids = formTemplate.getFieldsConf().stream()
            .map(FormDataElementConf::getId).toList();
        final var dataElements = dataElementRepository.findAllByUidIn(fieldUids);
        validateElementsDataElement(formTemplate, dataElements);

        return new FormElementProcessor(formTemplate).process(dataElements)
            .get()
            .version(createOrUpdateVersion(formTemplate) + 1);
    }

    private <T extends FormWithFields> Integer createOrUpdateVersion(T source) {
        Optional<Integer> version = Optional.empty();
        if (source instanceof DataFormTemplate) {
            version = dataFormTemplateRepository.findByUid(source.getUid())
                .map(DataFormTemplate::getVersion);
        }

        if (source instanceof DataForm) {
            version = legacyFormRepository.findByUid(source.getUid())
                .map(DataForm::getVersion);
        }

        return version.orElse(0);
    }

    private <T extends FormWithFields> void validateElementsDataElement(T formTemplate, Collection<DataElement> dataElements) {
        final var fieldUids = formTemplate.getFieldsConf().stream()
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
