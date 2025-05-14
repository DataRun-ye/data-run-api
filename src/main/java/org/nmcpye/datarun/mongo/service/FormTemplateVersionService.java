package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

/**
 * Service Custom Interface for managing {@link DataFormTemplate}.
 */
public interface FormTemplateVersionService
    extends AuditableObjectService<FormTemplateVersion, String> {
    FormTemplateVersionDto saveNewVersion(SaveFormTemplateDto saveFormTemplateDto);

    Page<FormTemplateVersionDto> findAllLatest(QueryRequest queryRequest, String jsonQueryBody);

    FormTemplateVersionDto findLatestByTemplate(String templateUid);
}
