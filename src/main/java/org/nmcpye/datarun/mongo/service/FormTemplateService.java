package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Service Custom Interface for managing {@link FormTemplate}.
 */
public interface FormTemplateService {

    boolean existsByUid(String uid);

    Optional<FormTemplateVersionDto> findByUid(String uid);

    void deleteByUid(String uid);

    Page<FormTemplateVersionDto> findAllByUser(QueryRequest queryRequest, String jsonQueryBody);

    /**
     * Save an object.
     *
     * @param object the entity to save.
     * @return the persisted entity.
     */
    FormTemplateVersionDto save(SaveFormTemplateDto object);

    /**
     * Updates a object.
     *
     * @param object the entity to update.
     * @return the persisted entity.
     */
    FormTemplateVersionDto update(SaveFormTemplateDto object);

    /**
     * Delete an object.
     *
     * @param object the entity to delete.
     */
    void delete(SaveFormTemplateDto object);
}
