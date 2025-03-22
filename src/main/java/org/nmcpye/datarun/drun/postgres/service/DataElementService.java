package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;

/**
 * Service Interface for managing {@link DataElement}.
 */
public interface DataElementService
    extends JpaAuditableObjectService<DataElement> {
}
