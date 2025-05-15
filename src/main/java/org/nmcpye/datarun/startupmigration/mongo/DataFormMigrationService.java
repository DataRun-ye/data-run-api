//package org.nmcpye.datarun.startupmigration.mongo;
//
//import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
//import org.nmcpye.datarun.common.feedback.ErrorCode;
//import org.nmcpye.datarun.drun.postgres.domain.DataElement;
//import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
//import org.nmcpye.datarun.drun.postgres.repository.DataElementRepository;
//import org.nmcpye.datarun.drun.postgres.repository.OptionSetRepository;
//import org.nmcpye.datarun.mongo.domain.DataFieldRule;
//import org.nmcpye.datarun.mongo.domain.DataForm;
//import org.nmcpye.datarun.mongo.domain.DataOption;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
//import org.nmcpye.datarun.mongo.domain.datafield.*;
//import org.nmcpye.datarun.mongo.domain.enumeration.RuleAction;
//import org.nmcpye.datarun.mongo.repository.DataFormRepository;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//import static org.nmcpye.datarun.utils.OptionSetUtil.createOptionMap;
//import static org.nmcpye.datarun.utils.PathUtil.getDirectParent;
//import static org.nmcpye.datarun.utils.PathUtil.replaceLastElement;
//
//@Component
//@Transactional
//public class DataFormMigrationService {
//
//    private final DataFormRepository formRepository;
//    private final DataElementRepository dataElementRepository;
//    private final OptionSetRepository optionSetRepository;
//
//    public DataFormMigrationService(DataFormRepository formRepository,
//                                    DataElementRepository dataElementRepository,
//                                    OptionSetRepository optionSetRepository) {
//        this.formRepository = formRepository;
//        this.dataElementRepository = dataElementRepository;
//        this.optionSetRepository = optionSetRepository;
//    }
//
//    public void run(String... args) throws Exception {
//        List<DataForm> dataForms = formRepository.findAll();
//        for (DataForm dataForm : dataForms) {
//            migrateDataForm(dataForm);
//        }
//    }
//
//    private void migrateDataForm(DataForm dataForm) {
//        // Extract fields and create DataElement documents
//        final DataForm template = createDataFormTemplate(dataForm);
//        for (AbstractField field : dataForm.flattenFields()) {
//            if (field instanceof Section section) {
//                // Create DataFormSectionConf for section fields
//                FormSectionConf sectionConf = createSectionConf(section);
//                template.getSections().add(sectionConf);
//            } else if (field instanceof DefaultField formField) {
//                DataElement dataElement = dataElementRepository.findByNameIgnoreCase(formField.getName()).orElseThrow();
//                FormDataElementConf elementConf = createDataElementConf(formField, dataElement.getUid(), dataElement.getCode());
//                template.getFields().add(elementConf);
//            } else {
//                throw new IllegalQueryException(ErrorCode.E1199, "DataElement Type: " + "not found ");
//            }
//        }
//        formRepository.save(template);
//    }
//
//    private DataForm createDataFormTemplate(DataForm form) {
//        DataForm template = formRepository.findByUid(form.getUid()).orElseThrow();
//        template.setUid(form.getUid());
//        template.setName(form.getName());
//        template.setDescription(form.getDescription());
//        template.setDeleted(form.getDeleted());
//        template.setDisabled(form.getDisabled());
//        template.setVersion(form.getVersion());
//        template.setLabel(form.getLabel());
//        template.setDefaultLocal(form.getDefaultLocal());
//        template.getFields().clear();
//        template.getSections().clear();
//        return template;
//    }
//
//    private DataElement createDataElement(DefaultField element, DataForm dataForm) {
//        DataElement dataElement = dataElementRepository.findByNameIgnoreCase(element.getName()).orElse(new DataElement());
//        dataElement.setName(element.getName().toLowerCase());
//        dataElement.setType(element.getType());
//        dataElement.setDescription(element.getDescription());
////        dataElement.setMandatory(element.getMandatory());
//        dataElement.setLabel(element.getLabel());
//        if (element instanceof OptionField field) {
//            final OptionSet optionSet = createOptionSet(field, dataForm.getOptions());
//            dataElement.setOptionSet(optionSet);
//        }
//
//        if (element instanceof ReferenceField field) {
//            dataElement.setResourceType(field.getResourceType());
//        }
//
//        return dataElementRepository.save(dataElement);
//    }
//
//    private OptionSet createOptionSet(OptionField field, List<DataOption> options) {
//        final OptionSet optionSet = optionSetRepository.findByNameIgnoreCase(field.getListName()).orElse(new OptionSet());
//        final List<DataOption> optionSetOptions = createOptionMap(options).get(field.getListName());
//        optionSet.setOptions(optionSetOptions);
//        optionSet.setName(field.getListName().toLowerCase());
//        return optionSetRepository.save(optionSet);
//    }
//
//    private FormDataElementConf createDataElementConf(DefaultField element, String dataElementUid, String dataElementCode) {
//        FormDataElementConf elementConf = new FormDataElementConf();
//        elementConf.setId(dataElementUid);
//        elementConf.setName(element.getName());
//        elementConf.setCode(dataElementCode);
//        elementConf.setParent(getDirectParent(element.getPath()));
//        elementConf.setPath(replaceLastElement(element.getPath(), dataElementUid));
//        elementConf.setType(element.getType());
//        elementConf.setDescription(element.getDescription());
//        elementConf.setLabel(element.getLabel());
//        elementConf.setMandatory(element.isMandatory());
//        elementConf.setDefaultValue(element.getDefaultValue());
//
//        elementConf.setAppearance(element.getAppearance());
//        elementConf.setCalculation(element.getCalculation());
//        elementConf.setConstraint(element.getConstraint());
//        elementConf.setConstraintMessage(element.getConstraintMessage());
//
//        final var errorRules = getErrorRules(element);
//        if (!errorRules.isEmpty()) {
//            elementConf.setConstraint(errorRules.stream().findFirst().orElseThrow().getExpression());
//            elementConf.setConstraintMessage(errorRules.stream().findFirst().orElseThrow().getMessage());
//        }
//
//        elementConf.setRules(element.getRules());
//        elementConf.setMainField(element.isMainField());
//        elementConf.setOrder(element.getOrder());
//        elementConf.setReadOnly(element.isReadOnly());
//
//        if (element instanceof OptionField field) {
//            final var de = dataElementRepository.findByUid(dataElementUid).orElseThrow();
//            elementConf.setOptionSet(de.getOptionSet().getUid());
//            elementConf.setChoiceFilter(field.getChoiceFilter());
//        }
//
//        if (element instanceof ScannedCodeField field) {
//            elementConf.setGs1Enabled(field.getGs1Enabled());
//            elementConf.setProperties(field.getProperties());
//        }
//
//        if (element instanceof ReferenceField field) {
//            elementConf.setResourceType(field.getResourceType());
//            elementConf.setResourceMetadataSchema(field.getResourceMetadataSchema());
//        }
//
//        return elementConf;
//    }
//
//    private List<DataFieldRule> getErrorRules(DefaultField element) {
//        return element.getRules().stream().filter(r -> r.getAction() == RuleAction.Error).toList();
//    }
//
//
//    private FormSectionConf createSectionConf(Section section) {
//        final FormSectionConf sectionConf = new FormSectionConf();
//        sectionConf.setPath(section.getPath());
//        sectionConf.setName(section.getName());
//        sectionConf.setAppearance(section.getAppearance());
//        sectionConf.setDescription(section.getDescription());
//        sectionConf.setLabel(section.getLabel());
//        sectionConf.setOrder(section.getOrder());
//        sectionConf.setRepeatable(section.getType().isRepeat());
//        sectionConf.setParent(getDirectParent(section.getPath()));
//        sectionConf.setRules(section.getRules());
//        return sectionConf;
//    }
//}
