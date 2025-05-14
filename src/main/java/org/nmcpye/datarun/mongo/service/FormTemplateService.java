package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;

/**
 * Service Custom Interface for managing {@link FormTemplate}.
 */
public interface FormTemplateService
    extends AuditableObjectService<FormTemplate, String> {
}
