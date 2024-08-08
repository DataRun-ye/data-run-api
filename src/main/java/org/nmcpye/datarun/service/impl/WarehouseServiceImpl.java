package org.nmcpye.datarun.service.impl;

import java.util.Optional;
import org.nmcpye.datarun.domain.Warehouse;
import org.nmcpye.datarun.repository.WarehouseRepository;
import org.nmcpye.datarun.service.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.Warehouse}.
 */
@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseServiceImpl.class);

    private final WarehouseRepository warehouseRepository;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        log.debug("Request to save Warehouse : {}", warehouse);
        return warehouseRepository.save(warehouse);
    }

    @Override
    public Warehouse update(Warehouse warehouse) {
        log.debug("Request to update Warehouse : {}", warehouse);
        warehouse.setIsPersisted();
        return warehouseRepository.save(warehouse);
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

    @Override
    @Transactional(readOnly = true)
    public Page<Warehouse> findAll(Pageable pageable) {
        log.debug("Request to get all Warehouses");
        return warehouseRepository.findAll(pageable);
    }

    public Page<Warehouse> findAllWithEagerRelationships(Pageable pageable) {
        return warehouseRepository.findAllWithEagerRelationships(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findOne(Long id) {
        log.debug("Request to get Warehouse : {}", id);
        return warehouseRepository.findOneWithEagerRelationships(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Warehouse : {}", id);
        warehouseRepository.deleteById(id);
    }
}
