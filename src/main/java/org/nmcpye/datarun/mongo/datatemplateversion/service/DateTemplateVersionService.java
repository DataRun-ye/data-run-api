package org.nmcpye.datarun.mongo.datatemplateversion.service;

import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Custom Interface for managing {@link DataTemplateVersion}.
 */
public interface DateTemplateVersionService
    extends IdentifiableObjectService<DataTemplateVersion, String> {
    Optional<DataTemplateVersion> findLatestByTemplate(String templateUid);

    FormTemplateVersionDto findByVersion(String masterUid, int version);

    Page<FormTemplateVersionDto> pageVersions(String templateId, Pageable pageable);
}
