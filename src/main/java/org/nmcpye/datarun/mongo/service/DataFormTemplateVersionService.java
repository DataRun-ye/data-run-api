package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplateVersion;

/**
 * Service Custom Interface for managing {@link DataFormTemplate}.
 */
public interface DataFormTemplateVersionService
    extends AuditableObjectService<DataFormTemplateVersion, String> {
}
