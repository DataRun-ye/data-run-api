package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.Warehouse;
import org.nmcpye.datarun.drun.postgres.repository.WarehouseRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.WarehouseServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

import java.util.Optional;

/**
 * REST controller for managing {@link Warehouse}.
 */
@RestController
@RequestMapping("/api/custom/warehouses")
public class WarehouseResourceCustom extends AbstractResource<Warehouse> {

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

    /**
     * {@code GET  /warehouses/:id} : get the "id" warehouse.
     *
     * @param id the id of the warehouse to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the warehouse, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Warehouse> getOneByUser(@PathVariable("id") Long id) {
        log.debug("REST request to get Warehouse : {}", id);
        Optional<Warehouse> warehouse = warehouseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(warehouse);
    }
}
