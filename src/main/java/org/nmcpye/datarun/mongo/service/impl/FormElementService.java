package org.nmcpye.datarun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.repository.DataElementRepository;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.repository.MetadataSchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class FormElementService {
    private final Logger log = LoggerFactory.getLogger(FormElementService.class);

    private final DataFormTemplateRepository templateRepository;
    private final DataElementRepository dataElementRepository;
    private final MetadataSchemaRepository metadataSchemaRepository;

    public FormElementService(DataFormTemplateRepository templateRepository, DataElementRepository dataElementRepository, MetadataSchemaRepository metadataSchemaRepository) {
        this.templateRepository = templateRepository;
        this.dataElementRepository = dataElementRepository;
        this.metadataSchemaRepository = metadataSchemaRepository;
    }

    public DataFormTemplate createDataForm(DataFormTemplate source) {
        DataFormTemplate target = templateRepository.findByUid(source.getUid())
            .orElseGet(() -> new DataFormTemplate().uid(source.getUid()));
        target.setVersion(target.getVersion() == null ? 1 : target.getVersion() + 1);

                target.setName(source.getName());
        target.setLabel(source.getLabel());
        target.setDescription(source.getDescription());
        target.setDefaultLocale(source.getDefaultLocale());
        target.setDisabled(Optional.ofNullable(source.getDisabled()).orElse(target.getDisabled()));
        target.setDeleted(source.getDeleted());

        populateDataElementsConf(source.getFields(), target);
        populateSectionsConf(source.getSections(), target);

        return templateRepository.save(target);
    }

    private void populateDataElementsConf(Collection<FormDataElementConf> fields, DataFormTemplate dataForm) {
        for (final FormDataElementConf elementConf : fields) {
            DataElement dataElement = dataElementRepository.findByUid(elementConf.getId())
                .orElseThrow(() -> new EntityNotFoundException("DataElement not found: " + elementConf.getId()));
            if (elementConf.getType().isReference()) {
                metadataSchemaRepository.findByUid(elementConf.getResourceMetadataSchema())
                    .orElseThrow(() -> {
                        log.error("Form's Reference field: {}, is referencing non existing metadata schema",
                            elementConf.getResourceMetadataSchema());
                        return new PropertyNotFoundException("Form's Reference field: " +
                            elementConf.getResourceMetadataSchema() + " is referencing non existing metadata schema: ");
                    });
            }

            overrideDataElementConf(dataElement, elementConf);
            elementConf.setPath(buildPath(elementConf, dataForm.getSections()));
            dataForm.getFields().add(elementConf);
        }
    }

    private void overrideDataElementConf(DataElement source, FormDataElementConf target) {
        target.setName(source.getName());
        target.setCode(source.getCode());
        target.setType(source.getType());
        target.setDescription(Optional.ofNullable(target.getDescription()).orElse(source.getDescription()));
        target.setLabel(Optional.ofNullable(target.getLabel()).orElse(source.getLabel()));
        target.setMandatory(Optional.ofNullable(target.getMandatory()).orElse(source.isMandatory()));
        target.setDefaultValue(Optional.ofNullable(target.getDefaultValue()).orElse(source.getDefaultValue()));
        target.setGs1Enabled(Optional.ofNullable(target.getGs1Enabled()).orElse(source.getGs1Enabled()));
        target.setProperties(Optional.ofNullable(target.getProperties()).orElse(source.getProperties()));
        target.setResourceType(Optional.ofNullable(target.getResourceType()).orElse(source.getResourceType()));
        target.setResourceMetadataSchema(Optional.ofNullable(target.getResourceMetadataSchema()).orElse(source.getResourceMetadataSchema()));
    }

    private void populateSectionsConf(Collection<FormSectionConf> sections, DataFormTemplate dataForm) {
        for (final FormSectionConf elementConf : sections) {
            elementConf.setPath(buildPath(elementConf, dataForm.getSections()));
            dataForm.getSections().add(elementConf);
        }
    }

    private <T extends FormElementConf> String buildPath(T element, List<FormSectionConf> sections) {
        final List<String> pathParts = new ArrayList<>();
        pathParts.add(element.getId());
        FormSectionConf current = getSection(element.getParent(), sections);

        while (current != null) {
            pathParts.add(0, current.getId());
            current = current.getParent() != null ? getSection(element.getParent(), sections) : null;
        }

        return String.join(".", pathParts);
    }

    private FormSectionConf getSection(String sectionId, List<FormSectionConf> sections) {
        final FormSectionConf section = sections.stream().filter((s) -> s.getId().equals(sectionId)).findFirst().orElse(null);
        return section;
    }


//    private void updateDataFormFields(DataFormTemplate source, DataFormTemplate target) {
//        target.setName(Optional.ofNullable(source.getName()).orElse(target.getCode()));
//        target.setLabel(Optional.ofNullable(source.getLabel()).orElse(target.getLabel()));
//        target.setDescription(Optional.ofNullable(source.getDescription()).orElse(target.getDescription()));
//        target.setDefaultLocal(Optional.ofNullable(source.getDefaultLocal()).orElse(target.getDefaultLocal()));
//        target.setDisabled(Optional.ofNullable(source.getDisabled()).orElse(target.getDisabled()));
//        target.setDeleted(source.getDeleted());
//        target.setOptions(Optional.ofNullable(source.getOptions()).orElse(target.getOptions()));
//    }

//    public DataForm creatOptionSet(List<DataOption> formOptions, DataForm dataForm) {
//        dataForm.setOptions(Optional.ofNullable(formOptions).orElse(dataForm.getOptions()));
//
//        dataForm.setOptionSets(Optional.ofNullable(formOptions)
//            .map(options -> new ArrayList<>(options.stream()
//                .collect(Collectors.groupingBy(
//                    DataOption::getListName,
//                    Collectors.collectingAndThen(
//                        Collectors.toSet(),
//                        optionSet -> {
//                            OptionSet set = new OptionSet();
//                            set.setName(optionSet.iterator().next().getListName());
//                            set.setOptions(optionSet);
//                            return set;
//                        }
//                    )
//                ))
//                .values())
//            )
//            .orElse(null)
//        );
//
//        return dataForm;
//    }

}
