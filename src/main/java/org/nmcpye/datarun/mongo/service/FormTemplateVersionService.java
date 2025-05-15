package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Custom Interface for managing {@link DataFormTemplate}.
 */
public interface FormTemplateVersionService {
    SaveFormTemplateDto saveNewVersion(SaveFormTemplateDto saveFormTemplateDto);

    @Transactional
    void migrateDataFormTemplateVersion(DataFormTemplate formTemplate);

    Page<SaveFormTemplateDto> findAllLatest(QueryRequest queryRequest, String jsonQueryBody);

    Optional<SaveFormTemplateDto> findLatestByTemplate(String templateUid);
}
