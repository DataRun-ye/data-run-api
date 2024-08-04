package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Warehouse;
import org.nmcpye.datarun.drun.postgres.repository.WarehouseRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.WarehouseServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class WarehouseServiceCustomImpl
    extends IdentifiableServiceImpl<Warehouse>
    implements WarehouseServiceCustom {

    private final Logger log = LoggerFactory.getLogger(WarehouseServiceCustomImpl.class);

    final private WarehouseRepositoryCustom warehouseRepository;

    public WarehouseServiceCustomImpl(WarehouseRepositoryCustom warehouseRepository) {
        super(warehouseRepository);
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public Optional<Warehouse> partialUpdate(Warehouse warehouse) {
        log.debug("Request to partially update Warehouse : {}", warehouse);

        return warehouseRepository
            .findById(warehouse.getId())
            .map(existingWarehouse -> {
                if (warehouse.getUid() != null) {
                    existingWarehouse.setUid(warehouse.getUid());
                }
                if (warehouse.getCode() != null) {
                    existingWarehouse.setCode(warehouse.getCode());
                }
                if (warehouse.getName() != null) {
                    existingWarehouse.setName(warehouse.getName());
                }
                if (warehouse.getDescription() != null) {
                    existingWarehouse.setDescription(warehouse.getDescription());
                }
                if (warehouse.getGpsCoordinate() != null) {
                    existingWarehouse.setGpsCoordinate(warehouse.getGpsCoordinate());
                }
                if (warehouse.getSupervisor() != null) {
                    existingWarehouse.setSupervisor(warehouse.getSupervisor());
                }
                if (warehouse.getSupervisorMobile() != null) {
                    existingWarehouse.setSupervisorMobile(warehouse.getSupervisorMobile());
                }
                if (warehouse.getCreatedBy() != null) {
                    existingWarehouse.setCreatedBy(warehouse.getCreatedBy());
                }
                if (warehouse.getCreatedDate() != null) {
                    existingWarehouse.setCreatedDate(warehouse.getCreatedDate());
                }
                if (warehouse.getLastModifiedBy() != null) {
                    existingWarehouse.setLastModifiedBy(warehouse.getLastModifiedBy());
                }
                if (warehouse.getLastModifiedDate() != null) {
                    existingWarehouse.setLastModifiedDate(warehouse.getLastModifiedDate());
                }

                return existingWarehouse;
            })
            .map(warehouseRepository::save);
    }

}
