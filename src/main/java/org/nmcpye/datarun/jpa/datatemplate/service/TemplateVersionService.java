package org.nmcpye.datarun.jpa.datatemplate.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import java.util.Optional;

/**
 * Service Custom Interface for managing {@link TemplateVersion}.
 */
public interface TemplateVersionService
    extends JpaIdentifiableObjectService<TemplateVersion> {
    Optional<TemplateVersion> findLatestByTemplate(String templateUid);
}
