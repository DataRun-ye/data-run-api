package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.WarehouseTransaction;
import org.nmcpye.datarun.drun.postgres.repository.WarehouseTransactionRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.WarehouseTransactionServiceCustom;
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
 * REST custom controller for managing {@link WarehouseTransaction}.
 */
@RestController
@RequestMapping("/api/custom/warehouseTransactions")
public class WarehouseTransactionResourceCustom
    extends AbstractResource<WarehouseTransaction> {

    private final Logger log = LoggerFactory.getLogger(WarehouseTransactionResourceCustom.class);

    private final WarehouseTransactionServiceCustom warehouseTransactionService;

    private final WarehouseTransactionRepositoryCustom warehouseTransactionRepository;

    public WarehouseTransactionResourceCustom(
        WarehouseTransactionServiceCustom warehouseTransactionService,
        WarehouseTransactionRepositoryCustom warehouseTransactionRepository
    ) {
        super(warehouseTransactionService, warehouseTransactionRepository);
        this.warehouseTransactionService = warehouseTransactionService;
        this.warehouseTransactionRepository = warehouseTransactionRepository;
    }

    @Override
    protected Page<WarehouseTransaction> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return warehouseTransactionService.findAllWithEagerRelationships(pageable);
        } else {
            return warehouseTransactionService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "warehouseTransactions";
    }

    /**
     * {@code GET  /warehouse-transactions/:id} : get the "id" warehouseTransaction.
     *
     * @param id the id of the warehouseTransaction to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the warehouseTransaction, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseTransaction> getOneByUser(@PathVariable("id") Long id) {
        log.debug("REST request to get WarehouseTransaction : {}", id);
        Optional<WarehouseTransaction> warehouseTransaction = warehouseTransactionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(warehouseTransaction);
    }
}
