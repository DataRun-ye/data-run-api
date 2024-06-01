package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.Warehouse;
import org.nmcpye.datarun.repository.WarehouseRepositoryCustom;
import org.nmcpye.datarun.service.custom.WarehouseServiceCustom;
import org.nmcpye.datarun.service.impl.WarehouseServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class WarehouseServiceCustomImpl
    extends WarehouseServiceImpl
    implements WarehouseServiceCustom {

    private final Logger log = LoggerFactory.getLogger(WarehouseServiceCustomImpl.class);

    final private WarehouseRepositoryCustom warehouseRepository;

    public WarehouseServiceCustomImpl(WarehouseRepositoryCustom warehouseRepository) {
        super(warehouseRepository);
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        if (warehouse.getUid() == null || warehouse.getUid().isEmpty()) {
            warehouse.setUid(CodeGenerator.generateUid());
        }
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Warehouse> findAll(Pageable pageable) {
        log.debug("Request to get all Warehouses");
        return warehouseRepository.findAllByUser(pageable);
    }

    public Page<Warehouse> findAllWithEagerRelationships(Pageable pageable) {
        return warehouseRepository.findAllWithEagerRelationshipsByUser(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findOne(Long id) {
        log.debug("Request to get Warehouse : {}", id);
        return warehouseRepository.findOneWithEagerRelationshipsByUser(id);
    }
}
