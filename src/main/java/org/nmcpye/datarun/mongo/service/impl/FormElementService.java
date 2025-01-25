package org.nmcpye.datarun.mongo.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.mongo.domain.dataelement.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataElementRepository;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class FormElementService {

    private final DataFormTemplateRepository templateRepository;
    private final DataElementRepository dataElementRepository;

    public FormElementService(DataFormTemplateRepository templateRepository, DataElementRepository dataElementRepository) {
        this.templateRepository = templateRepository;
        this.dataElementRepository = dataElementRepository;
    }

    public DataFormTemplate createDataForm(DataFormTemplate dataForm) {
        DataFormTemplate dataFormToConfigure = templateRepository.findByUid(dataForm.getUid())
            .orElseGet(() -> new DataFormTemplate().uid(dataForm.getUid()));
        dataFormToConfigure.setVersion(dataFormToConfigure.getVersion() == null ? 1 : dataFormToConfigure.getVersion() + 1);

        populateDataElementsConf(dataForm.getFields(), dataFormToConfigure);
        populateSectionsConf(dataForm.getSections(), dataFormToConfigure);

        return templateRepository.save(dataFormToConfigure);
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

    private void populateDataElementsConf(Collection<FormDataElementConf> fields, DataFormTemplate dataForm) {
        for (final FormDataElementConf elementConf : fields) {
            DataElement dataElement = dataElementRepository.findByUid(elementConf.getId())
                .orElseThrow(() -> new EntityNotFoundException("DataElement not found: " + elementConf.getId()));

            overrideDataElementConf(dataElement, elementConf);
            elementConf.setPath(buildPath(elementConf, dataForm.getSections()));
//            dataForm.getFields().put(elementConf.getId(), elementConf);
            dataForm.getFields().add(elementConf);
        }
    }

    private void overrideDataElementConf(DataElement source, FormDataElementConf target) {
        target.setName(source.getName());
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
//            dataForm.getSections().put(elementConf.getId(), elementConf);
            dataForm.getSections().add(elementConf);
        }
    }

    private <T extends FormElementConf> String buildPath(T element, List<FormSectionConf> sections) {
        final List<String> pathParts = new ArrayList<>();
        pathParts.add(element.getId());
        FormSectionConf current = getSection(element.getParent(), sections);
//        FormSectionConf current = sections.get(element.getParent());

        while (current != null) {
            pathParts.add(0, current.getId());
//            current = current.getParent() != null ? sections.get(element.getParent()) : null;
            current = current.getParent() != null ? getSection(element.getParent(), sections) : null;
        }

        return String.join(".", pathParts);
    }

    private FormSectionConf getSection(String sectionId, List<FormSectionConf> sections) {
        final FormSectionConf section = sections.stream().filter((s) -> s.getId().equals(sectionId)).findFirst().orElse(null);
        return section;
    }
}
