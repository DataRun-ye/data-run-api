package org.nmcpye.datarun.jpa.datatemplate.service;

import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Service Custom Interface for managing {@link DataTemplate}.
 */
public interface DataTemplateInstanceService {

    boolean existsByUid(String uid);

    Optional<DataTemplateInstanceDto> findByUid(String uid);

    void deleteByUid(String uid);

    Page<DataTemplateInstanceDto> findAllByUser(QueryRequest queryRequest, String jsonQueryBody);

    /**
     * Save an object.
     *
     * @param object the entity to save.
     * @return the persisted entity.
     */
    DataTemplateInstanceDto save(DataTemplateInstanceDto object);

    /**
     * Updates a object.
     *
     * @param object the entity to update.
     * @return the persisted entity.
     */
    DataTemplateInstanceDto update(DataTemplateInstanceDto object);

    /**
     * Delete an object.
     *
     * @param object the entity to delete.
     */
    void delete(DataTemplateInstanceDto object);

    DataTemplateInstanceDto saveNewVersion(DataTemplateInstanceDto dataTemplateInstanceDto);

//    void migrateDataFormTemplateVersionToLegacy(DataFormTemplate formTemplate);

    void migrateDataFormTemplateVersion(DataFormTemplate formTemplate);

    Optional<DataTemplateInstanceDto> findLatestByTemplate(String templateUid);
    Optional<DataTemplateInstanceDto> findByTemplateAndVersion(String templateUid, String versionUid);
    Optional<DataTemplateInstanceDto> findByTemplateAndVersionNumber(String templateUid, Integer version);
}
