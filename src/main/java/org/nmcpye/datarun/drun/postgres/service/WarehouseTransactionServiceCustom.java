package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.WarehouseTransaction;
import org.nmcpye.datarun.service.WarehouseTransactionService;

/**
 * Service Interface for managing {@link WarehouseTransaction}.
 */
public interface WarehouseTransactionServiceCustom
    extends IdentifiableService<WarehouseTransaction>, WarehouseTransactionService {
}
