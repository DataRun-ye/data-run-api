package org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess;


import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.dataelement.DataTemplateElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateprocessor.TemplateElementProcessor;
import org.nmcpye.datarun.datatemplateprocessor.validation.DefaultTemplateValidator;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersionInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada 19/03/2025 <7amza.it@gmail.com>
 */
@Component
@Transactional(readOnly = true)
public class FormTemplateProcessor {
    protected final Logger log = LoggerFactory.getLogger(FormTemplateProcessor.class);

    private final DataElementRepository dataElementRepository;

    private final DefaultTemplateValidator validator;

    public FormTemplateProcessor(DataElementRepository dataElementRepository,
                                 DefaultTemplateValidator validator) {

        this.dataElementRepository = dataElementRepository;
        this.validator = validator;
    }

    public <T extends DataTemplateVersionInterface> T validate(T formTemplate) {
        log.debug("start validating form template {}", formTemplate.getUid());

        validator.validate(formTemplate);
        return formTemplate;
    }

    public <T extends DataTemplateVersionInterface> DataTemplateVersionInterface processMetadata(T formTemplate) {
        log.debug("start processing form template's metadata {}", formTemplate.getUid());

        final var fieldUids = formTemplate.getFields().stream()
            .map(FormDataElementConf::getId).toList();
        final var dataElements = dataElementRepository.findAllByUidIn(fieldUids);
        validateElementsDataElement(formTemplate, dataElements);

        return new TemplateElementProcessor(formTemplate).process(dataElements)
            .get();
    }

    private <T extends DataTemplateVersionInterface> void validateElementsDataElement(T formTemplate, Collection<DataTemplateElement> dataTemplateElements) {
        final var fieldUids = formTemplate.getFields().stream()
            .map(FormDataElementConf::getId).toList();
        final var dataElementUids = getUids(dataTemplateElements);
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
