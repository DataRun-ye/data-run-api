package org.nmcpye.datarun.datatemplateversion;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Service Custom Interface for managing {@link DataFormTemplate}.
 */
public interface FormTemplateVersionService
    extends AuditableObjectService<DataTemplateTemplateVersion, String> {
    DataTemplateInstanceDto saveNewVersion(DataTemplateInstanceDto dataTemplateInstanceDto);

//    void migrateDataFormTemplateVersionToLegacy(DataFormTemplate formTemplate);

    void migrateDataFormTemplateVersion(DataFormTemplate formTemplate);

    Page<DataTemplateInstanceDto> findAllLatest(QueryRequest queryRequest, String jsonQueryBody);

    Optional<DataTemplateInstanceDto> findLatestByTemplate(String templateUid);
}
