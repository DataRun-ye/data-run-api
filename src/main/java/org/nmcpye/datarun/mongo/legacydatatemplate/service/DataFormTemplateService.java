package org.nmcpye.datarun.mongo.legacydatatemplate.service;


import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;

/**
 * Service Custom Interface for managing {@link DataFormTemplate}.
 */
public interface DataFormTemplateService
    extends AuditableObjectService<DataFormTemplate, String> {
}
