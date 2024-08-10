package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.domain.Warehouse;
import org.nmcpye.datarun.drun.postgres.repository.WarehouseRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.WarehouseServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link Warehouse}.
 */
@RestController
@RequestMapping("/api/custom/warehouses")
public class WarehouseResourceCustom extends AbstractRelationalResource<Warehouse> {

    private final Logger log = LoggerFactory.getLogger(WarehouseResourceCustom.class);

    private final WarehouseServiceCustom warehouseService;

    private final WarehouseRelationalRepositoryCustom warehouseRepository;

    public WarehouseResourceCustom(WarehouseServiceCustom warehouseService,
                                   WarehouseRelationalRepositoryCustom warehouseRepository) {
        super(warehouseService, warehouseRepository);
        this.warehouseService = warehouseService;
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    protected Page<Warehouse> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return warehouseService.findAllWithEagerRelationships(pageable);
        } else {
            return warehouseService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "warehouses";
    }
}
