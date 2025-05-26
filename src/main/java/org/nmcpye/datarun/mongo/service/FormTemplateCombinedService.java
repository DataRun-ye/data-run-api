package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Service Custom Interface for managing {@link FormTemplate}.
 */
public interface FormTemplateCombinedService {

    boolean existsByUid(String uid);

    Optional<SaveFormTemplateDto> findByUid(String uid);

    void deleteByUid(String uid);

    Page<SaveFormTemplateDto> findAllByUser(QueryRequest queryRequest, String jsonQueryBody);

    /**
     * Save an object.
     *
     * @param object the entity to save.
     * @return the persisted entity.
     */
    SaveFormTemplateDto save(SaveFormTemplateDto object);

    /**
     * Updates a object.
     *
     * @param object the entity to update.
     * @return the persisted entity.
     */
    SaveFormTemplateDto update(SaveFormTemplateDto object);

    /**
     * Delete an object.
     *
     * @param object the entity to delete.
     */
    void delete(SaveFormTemplateDto object);
}
