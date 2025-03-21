package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.MetadataSchema;

/**
 * Service Interface for managing {@link DataFormSubmission}.
 */
public interface MetadataSchemaService
    extends AuditableObjectService<MetadataSchema, String> {
}
