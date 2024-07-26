package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.Warehouse;
import org.nmcpye.datarun.service.WarehouseService;

/**
 * Custom Service Interface for managing {@link Warehouse}.
 */
public interface WarehouseServiceCustom
    extends IdentifiableService<Warehouse>, WarehouseService {
}
