package org.nmcpye.datarun.mongo.service.formcomfiguration;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.drun.postgres.repository.DataElementRepository;
import org.nmcpye.datarun.mongo.domain.DataFieldRule;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.enumeration.RuleAction;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.nmcpye.datarun.mongo.repository.MetadataSchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.nmcpye.datarun.utils.PathUtil.replaceLastElement;

@Component
@Transactional
public class FormElementConfigurator {
    private final Logger log = LoggerFactory.getLogger(FormElementConfigurator.class);

    private final DataElementRepository dataElementRepository;
    private final MetadataSchemaRepository metadataSchemaRepository;

    public FormElementConfigurator(DataElementRepository dataElementRepository,
                                   MetadataSchemaRepository metadataSchemaRepository) {
        this.dataElementRepository = dataElementRepository;
        this.metadataSchemaRepository = metadataSchemaRepository;
    }

    public List<FormElementConf> mergeElements(DataFormTemplate source) {
        return FormElementMerger.mergeElements(source);
    }

    public void overrideDataElementConf(FormDataElementConf elementConf) {
        final DataElement source = dataElementRepository.findByUid(elementConf.getId())
            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1100, elementConf.getId()));

        elementConf.setId(source.getUid());
        elementConf.setCode(source.getCode());
        elementConf.setName(Optional.ofNullable(elementConf.getName()).orElse(source.getName()));
        elementConf.setPath(replaceLastElement(elementConf.getPath(), source.getUid()));
        elementConf.setType(source.getType());
        elementConf.setDescription(Optional.ofNullable(elementConf.getDescription()).orElse(source.getDescription()));
        elementConf.setLabel(Optional.ofNullable(elementConf.getLabel()).orElse(source.getLabel()));

        elementConf.setRules(elementConf.getRules());
        final List<DataFieldRule> errorRules = getErrorRules(elementConf);
        if (!errorRules.isEmpty()) {
            if (elementConf.getConstraint() == null) {
                elementConf.setConstraint(errorRules.stream().findFirst().orElseThrow().getExpression());
                elementConf.setConstraintMessage(errorRules.stream().findFirst().orElseThrow().getMessage());
            }
        }

        if (source.getType().isOptionsType()) {
            elementConf.setOptionSet(source.getOptionSet().getUid());
        }

        if (source.getType() == ValueType.ScannedCode) {
            if (elementConf.getType() != null && elementConf.getType() != ValueType.ScannedCode) {
                throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1105, elementConf.getId()));
            }
            elementConf
                .setValueTypeRendering(Optional.ofNullable(elementConf.getValueTypeRendering())
                    .orElse(ValueTypeRendering.BAR_CODE));

            if (elementConf.getGs1Enabled() == Boolean.TRUE) {
                elementConf.setValueTypeRendering(Optional.ofNullable(elementConf.getValueTypeRendering())
                    .orElse(ValueTypeRendering.GS1_DATAMATRIX));
            }
        }

        if (source.getType().isReference()) {

            final String elementResourceMetaDataSchema = Optional.ofNullable(elementConf.getResourceMetadataSchema())
                .orElseThrow(() ->
                    new IllegalQueryException(new ErrorMessage(ErrorCode.E1103, elementConf.getId()))
                );

            metadataSchemaRepository.findByUid(elementResourceMetaDataSchema)
                .orElseThrow(() -> {
                    log.error("Form's Reference field: {}, is referencing non existing metadata schema",
                        elementResourceMetaDataSchema);
                    return new IllegalQueryException(new ErrorMessage(ErrorCode.E1104, elementResourceMetaDataSchema));
                });
            elementConf.setResourceMetadataSchema(elementResourceMetaDataSchema);
            elementConf.setResourceType(source.getResourceType());
        }
    }

    private static List<DataFieldRule> getErrorRules(FormDataElementConf element) {
        return element.getRules().stream().filter(r -> r.getAction() == RuleAction.Error).toList();
    }
}
