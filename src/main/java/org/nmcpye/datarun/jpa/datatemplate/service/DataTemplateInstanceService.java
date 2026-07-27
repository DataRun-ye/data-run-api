package org.nmcpye.datarun.jpa.datatemplate.service;

import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.apiquery.QueryRequest;
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
     * Publish an immutable template version and move the template's latest
     * pointer to it.
     *
     * @param template the validated and processed template definition.
     * @return the persisted template with its new version identity.
     */
    DataTemplateInstanceDto publishVersion(
        DataTemplateInstanceDto template);

    Optional<DataTemplateInstanceDto> findLatestByTemplate(String templateUid);

    Optional<DataTemplateInstanceDto> findByTemplateAndVersionUid(String templateUid, String versionUid);

    Optional<DataTemplateInstanceDto> findByTemplateAndVersionNo(String templateUid, Integer version);
}
