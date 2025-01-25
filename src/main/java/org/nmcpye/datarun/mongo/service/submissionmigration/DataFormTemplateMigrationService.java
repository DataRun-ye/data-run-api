package org.nmcpye.datarun.mongo.service.submissionmigration;

import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.DataOption;
import org.nmcpye.datarun.mongo.domain.OptionSet;
import org.nmcpye.datarun.mongo.domain.dataelement.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.datafield.*;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataElementRepository;
import org.nmcpye.datarun.mongo.repository.DataFormRepository;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.repository.OptionSetRepository;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

import static org.nmcpye.datarun.utils.OptionSetUtil.createOptionMap;
import static org.nmcpye.datarun.utils.PathUtil.getDirectParent;
import static org.nmcpye.datarun.utils.PathUtil.replaceLastElement;

//@Component
//@Transactional
public class DataFormTemplateMigrationService implements CommandLineRunner {

    private final DataFormRepository dataFormRepository;
    private final DataFormTemplateRepository dataFormTemplateRepository;
    private final DataElementRepository dataElementRepository;
    private final OptionSetRepository optionSetRepository;

    public DataFormTemplateMigrationService(DataFormRepository dataFormRepository,
                                            DataFormTemplateRepository dataFormTemplateRepository,
                                            DataElementRepository dataElementRepository,
                                            OptionSetRepository optionSetRepository) {
        this.dataFormRepository = dataFormRepository;
        this.dataFormTemplateRepository = dataFormTemplateRepository;
        this.dataElementRepository = dataElementRepository;
        this.optionSetRepository = optionSetRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        List<DataForm> dataForms = dataFormRepository.findAll();
        for (DataForm dataForm : dataForms) {
            migrateDataForm(dataForm);
        }
    }

    private void migrateDataForm(DataForm dataForm) {
        // Extract fields and create DataElement documents
        final DataFormTemplate template = createDataFormTemplate(dataForm);
        for (AbstractField field : dataForm.getFlattenedFields()) {
            if (field instanceof Section section) {
                // Create DataFormSectionConf for section fields
                FormSectionConf sectionConf = createSectionConf(section);

//                template.getSections().put(sectionConf.getId(), sectionConf);
                template.getSections().add(sectionConf);
            } else {
                // Update DataForm with DataElement configurations
                DataElement dataElement = createDataElement((DefaultField) field, dataForm);
                FormDataElementConf elementConf = createDataElementConf((DefaultField) field, dataElement.getUid());
//                template.getFields().put(dataElement.getUid(), elementConf);
                template.getFields().add(elementConf);
            }
        }

//        dataForm.setOptionSets(createOptionSets(dataForm.getOptions()));
        // Clear old fields
        // dataForm.getFields().clear();
//        dataFormTemplateRepository.save();
        // Save updated DataForm
        dataFormTemplateRepository.save(template);
    }

    private DataFormTemplate createDataFormTemplate(DataForm form) {
        DataFormTemplate template = dataFormTemplateRepository.findByUid(form.getUid()).orElse(new DataFormTemplate());
        template.setUid(form.getUid());
        template.setName(form.getName());
        template.setDescription(form.getDescription());
        template.setDeleted(form.getDeleted());
        template.setDisabled(form.getDisabled());
        template.setVersion(form.getVersion());
        template.setLabel(form.getLabel());
        template.setDefaultLocale(form.getDefaultLocal());
        return template;
    }

    private DataElement createDataElement(DefaultField element, DataForm dataForm) {
        DataElement dataElement = dataElementRepository.findFirstByNameIgnoreCase(element.getName()).orElse(new DataElement());
        dataElement.setName(element.getName().toLowerCase());
        dataElement.setType(element.getType());
        dataElement.setDescription(element.getDescription());
        dataElement.setDefaultValue(element.getDefaultValue());
        dataElement.setMandatory(element.getMandatory());
        dataElement.setLabel(element.getLabel());
        if (element instanceof OptionField field) {
            final OptionSet optionSet = createOptionSet(field, dataForm.getOptions());
            dataElement.setOptionSet(optionSet.getUid());
        }

        if (element instanceof ScannedCodeField field) {
            dataElement.setGs1Enabled(field.getGs1Enabled());
            dataElement.setProperties(field.getProperties());
        }

        if (element instanceof ReferenceField field) {
            dataElement.setResourceType(field.getResourceType());
            dataElement.setResourceMetadataSchema(field.getResourceMetadataSchema());
        }

        return dataElementRepository.save(dataElement);
    }

    private OptionSet createOptionSet(OptionField field, List<DataOption> options) {
        final OptionSet optionSet = optionSetRepository.findFirstByNameIgnoreCase(field.getListName()).orElse(new OptionSet());
        final List<DataOption> optionSetOptions = createOptionMap(options).get(field.getListName());
        optionSet.setOptions(optionSetOptions);
        optionSet.setName(field.getListName().toLowerCase());
        return optionSetRepository.save(optionSet);
    }

    private FormDataElementConf createDataElementConf(DefaultField element, String dataElementUid) {
        FormDataElementConf elementConf = new FormDataElementConf();
        elementConf.setId(dataElementUid);
        elementConf.setName(element.getName());
        elementConf.setParent(getDirectParent(element.getPath()));
        elementConf.setPath(replaceLastElement(element.getPath(), dataElementUid));
        elementConf.setType(element.getType());
        elementConf.setDescription(element.getDescription());
        elementConf.setLabel(element.getLabel());
        elementConf.setMandatory(element.getMandatory());
        elementConf.setDefaultValue(element.getDefaultValue());

        elementConf.setAppearance(element.getAppearance());
        elementConf.setCalculation(element.getCalculation());
        elementConf.setConstraint(element.getConstraint());
        elementConf.setConstraintMessage(element.getConstraintMessage());
        elementConf.setRules(element.getRules());
        elementConf.setMainField(element.getMainField());
        elementConf.setOrder(element.getOrder());
        elementConf.setReadOnly(element.getReadOnly());

        if (element instanceof OptionField field) {
            final var de = dataElementRepository.findByUid(dataElementUid).orElseThrow();
            elementConf.setOptionSet(de.getOptionSet());
            elementConf.setChoiceFilter(field.getChoiceFilter());
        }

        if (element instanceof ScannedCodeField field) {
            elementConf.setGs1Enabled(field.getGs1Enabled());
            elementConf.setProperties(field.getProperties());
        }

        if (element instanceof ReferenceField field) {
            elementConf.setResourceType(field.getResourceType());
            elementConf.setResourceMetadataSchema(field.getResourceMetadataSchema());
        }

        return elementConf;
    }

    private FormSectionConf createSectionConf(Section section) {
        final FormSectionConf sectionConf = new FormSectionConf();
        sectionConf.setPath(section.getPath());
        sectionConf.setName(section.getName());
        sectionConf.setId(section.getName());
        sectionConf.setAppearance(section.getAppearance());
        sectionConf.setDescription(section.getDescription());
        sectionConf.setLabel(section.getLabel());
        sectionConf.setOrder(section.getOrder());
        sectionConf.setRepeatable(section.getType().isRepeat());
        sectionConf.setParent(getDirectParent(section.getPath()));
        sectionConf.setRules(section.getRules());
        return sectionConf;
    }
}
