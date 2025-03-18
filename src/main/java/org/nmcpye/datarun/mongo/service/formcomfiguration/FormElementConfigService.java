package org.nmcpye.datarun.mongo.service.formcomfiguration;

import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.nmcpye.datarun.utils.PathUtil.buildPath;

@Service
@Transactional
public class FormElementConfigService {
    private final DataFormTemplateRepository templateRepository;
    private final FormElementConfigurator formElementConfigurator;

    public FormElementConfigService(DataFormTemplateRepository templateRepository,
                                    FormElementConfigurator formElementConfigurator) {
        this.templateRepository = templateRepository;
        this.formElementConfigurator = formElementConfigurator;
    }

    public void mergeFormElements(DataFormTemplate source) {
        source.setElements(formElementConfigurator.mergeElements(source));
    }

    public DataFormTemplate createDataForm(DataFormTemplate source) {
        source.setFields(source.getFields().stream().distinct().toList());
        source.setSections(source.getSections().stream().distinct().toList());

        configureSectionsPath(source.getSections());
        configureAndValidateFields(source.getFields(), source.getSections());
        return templateRepository.save(source);
    }

    /**
     * Stub for element configuration.
     * You may set paths or apply any other configuration needed.
     *
     * @param fields The form fields to configure
     */
    private void configureAndValidateFields(
        List<FormDataElementConf> fields, List<FormSectionConf> formSectionsConf) {
        for (FormDataElementConf element : fields) {
            element.setPath(buildPath(element, formSectionsConf));
            formElementConfigurator.overrideDataElementConf(element);
        }
    }

    private void configureSectionsPath(List<FormSectionConf> formSectionsConf) {
        for (FormSectionConf element : formSectionsConf) {
            element.setPath(buildPath(element, formSectionsConf));
        }
    }
}
