package org.nmcpye.datarun.mongo.migration;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.datatemplateelement.*;
import org.nmcpye.datarun.datatemplateelement.enumeration.RuleAction;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.jpa.option.repository.OptionSetRepository;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.datafield.*;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.legacydatatemplate.repository.DataFormRepository;
import org.nmcpye.datarun.mongo.legacydatatemplate.repository.DataFormTemplateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.utils.OptionSetUtil.createOptionMap;
import static org.nmcpye.datarun.utils.PathUtil.getDirectParent;
import static org.nmcpye.datarun.utils.PathUtil.replaceLastElement;

@Component
@Transactional
@SuppressWarnings("unused")
public class DataFormTemplateMigrationService /*implements CommandLineRunner*/ {

    private final DataFormRepository dataFormRepository;
    private final DataFormTemplateRepository dataFormTemplateRepository;
    private final DataElementRepository dataElementRepository;
    private final OptionSetRepository optionSetRepository;
//    private final DataFormMigrationService dataFormMigrationService;

    public DataFormTemplateMigrationService(DataFormRepository dataFormRepository,
                                            DataFormTemplateRepository dataFormTemplateRepository,
                                            DataElementRepository dataElementRepository,
                                            OptionSetRepository optionSetRepository) {
        this.dataFormRepository = dataFormRepository;
        this.dataFormTemplateRepository = dataFormTemplateRepository;
        this.dataElementRepository = dataElementRepository;
        this.optionSetRepository = optionSetRepository;
//        this.dataFormMigrationService = dataFormMigrationService;
    }

    //    @Override
    public void runForDataFormTemplates(String... args) {
        List<DataForm> dataForms = dataFormRepository.findAll();
        for (DataForm dataForm : dataForms) {
            migrateOptionSets(dataForm);
            optionSetRepository.flush();
            migrateDataElements(dataForm);
            dataElementRepository.flush();
            migrateDataForm(dataForm);
        }
    }

    private void migrateDataForm(DataForm dataForm) {
        // Extract fields and create DataElement documents
        final DataFormTemplate template = createDataFormTemplate(dataForm);
        for (AbstractField field : dataForm.flattenFields()) {
            if (field instanceof Section section) {
                // Create DataFormSectionConf for section fields
                FormSectionConf sectionConf = createSectionConf(section);
                template.getSections().add(sectionConf);
            } else if (field instanceof DefaultField formField) {
                DataElement dataTemplateElement = dataElementRepository.findByNameIgnoreCase(formField.getName()).orElseThrow();
                FormDataElementConf elementConf = createDataElementConf(formField, dataTemplateElement.getUid(), dataTemplateElement.getCode());
                template.getFields().add(elementConf);
            } else {
                throw new EntityNotFoundException("DataElement not found: " + field.getName());
            }
        }

        dataFormTemplateRepository.save(template);
    }

    private void migrateDataElements(DataForm dataForm) {
        for (AbstractField field : dataForm.flattenFields().stream().filter((f) -> f instanceof DefaultField).collect(Collectors.toSet())) {
            DataElement dataTemplateElement = createDataElement((DefaultField) field, dataForm);
            dataElementRepository.save(dataTemplateElement);
        }
    }

    private void migrateOptionSets(DataForm dataForm) {
        for (AbstractField field : dataForm.flattenFields().stream().filter((f) -> f.getType().isOptionsType()).collect(Collectors.toSet())) {
            final OptionField optionField = (OptionField) field;
            final OptionSet optionSet = optionSetRepository.findByNameIgnoreCase(optionField.getListName())
                .orElse(new OptionSet());
            final List<DataOption> optionSetOptions = createOptionMap(dataForm.getOptions())
                .get(optionField.getListName());
            optionSet.setLegacyOptions(optionSetOptions);
            optionSet.setName(optionField.getListName().toLowerCase());
            optionSetRepository.save(optionSet);
        }
    }

    private DataFormTemplate createDataFormTemplate(DataForm form) {
        DataFormTemplate template = dataFormTemplateRepository.findByUid(form.getUid()).orElse(new DataFormTemplate());
        template.setUid(form.getUid());
        template.setName(form.getName());
        template.setDescription(form.getDescription());
        template.setDeleted(form.getDeleted());
        template.setDisabled(Optional.ofNullable(form.getDisabled()).orElse(false));
        template.setVersion(form.getVersion() != null ? form.getVersion() : 1);
        template.setLabel(form.getLabel());
        template.setDefaultLocale(form.getDefaultLocal());
        template.getFields().clear();
        template.getSections().clear();
        return template;
    }

    private DataElement createDataElement(DefaultField element, DataForm dataForm) {
        try {
            DataElement dataTemplateElement = dataElementRepository.findByName(element.getName()).orElse(new DataElement());
            dataTemplateElement.setName(element.getName().toLowerCase());
            dataTemplateElement.setCode(element.getName().toLowerCase());
            dataTemplateElement.setType(element.getType());
            dataTemplateElement.setDescription(element.getDescription());
//            dataElement.setLabel(element.getLabel());
            if (element.getType().isOptionsType()) {
                final OptionSet optionSet = optionSetRepository.findByNameIgnoreCase(((OptionField) element)
                    .getListName()).orElseThrow();
                dataTemplateElement.setOptionSet(optionSet);
            }

            if (element instanceof ReferenceField field) {
                dataTemplateElement.setResourceType(field.getResourceType());
//            dataElement.setResourceMetadataSchema(field.getResourceMetadataSchema());
            }

            return dataTemplateElement;
        } catch (Exception e) {
            throw new EntityNotFoundException("DataElement not found: " + element.getName());
        }

    }

    private FormDataElementConf createDataElementConf(DefaultField element, String dataElementUid, String dataElementCode) {
        FormDataElementConf elementConf = new FormDataElementConf();
        elementConf.setId(dataElementUid);
        elementConf.setName(element.getName());
        elementConf.setCode(dataElementCode);
        elementConf.setParent(getDirectParent(element.getPath()));
        elementConf.setPath(replaceLastElement(element.getPath(), dataElementUid));
        elementConf.setType(element.getType());
        elementConf.setDescription(element.getDescription());
        elementConf.setLabel(element.getLabel());
        elementConf.setMandatory(element.isMandatory());
        elementConf.setDefaultValue(element.getDefaultValue());
        elementConf.setCalculation(element.getCalculation());

        if (element.getConstraint() != null) {
            elementConf.setValidationRule(new ElementValidationRule()
                .setExpression(element.getConstraint())
                .setValidationMessage(element.getConstraintMessage()));
        }

        elementConf.setRules(element.getRules());
        final var errorRules = getErrorRules(element);

        if (!errorRules.isEmpty() && element.getConstraint() == null) {
            final var elementValidationRule = new ElementValidationRule()
                .setExpression(errorRules.stream().findFirst().orElseThrow().getExpression())
                .setValidationMessage(errorRules.stream().findFirst().orElseThrow().getMessage());
            elementConf.setValidationRule(elementValidationRule);
        }
        elementConf.setShowInSummary(element.isMainField());
        elementConf.setOrder(element.getOrder());
//        elementConf.setReadOnly(element.isReadOnly());

        if (element instanceof OptionField field) {
            final var de = dataElementRepository.findByUid(dataElementUid).orElseThrow();
            elementConf.setOptionSet(de.getOptionSet().getUid());
            elementConf.setChoiceFilter(field.getChoiceFilter());
        }

        if (element instanceof ScannedCodeField field) {
            elementConf.setGs1Enabled(field.getGs1Enabled());
            elementConf.setScannedCodeProperties(field.getProperties());
        }

        if (element instanceof ReferenceField field) {
            elementConf.setResourceType(field.getResourceType());
            elementConf.setResourceMetadataSchema(field.getResourceMetadataSchema());
        }

        return elementConf;
    }

    private List<DataFieldRule> getErrorRules(DefaultField element) {
        return element.getRules().stream().filter(r -> r.getAction() == RuleAction.Error).toList();
    }

    private FormSectionConf createSectionConf(Section section) {
        final FormSectionConf sectionConf = new FormSectionConf();
        sectionConf.setPath(section.getPath());
        sectionConf.setName(section.getName());
        sectionConf.setDescription(section.getDescription());
        sectionConf.setLabel(section.getLabel());
        sectionConf.setOrder(section.getOrder());
        sectionConf.setRepeatable(section.getType().isRepeat());
        sectionConf.setParent(getDirectParent(section.getPath()));
        sectionConf.setRules(section.getRules());
        return sectionConf;
    }
}
