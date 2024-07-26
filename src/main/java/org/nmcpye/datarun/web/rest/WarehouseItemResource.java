package org.nmcpye.datarun.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.nmcpye.datarun.domain.WarehouseItem;
import org.nmcpye.datarun.repository.WarehouseItemRepository;
import org.nmcpye.datarun.service.WarehouseItemService;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link org.nmcpye.datarun.domain.WarehouseItem}.
 */
@RestController
@RequestMapping("/api/warehouse-items")
public class WarehouseItemResource {

    private final Logger log = LoggerFactory.getLogger(WarehouseItemResource.class);

    private static final String ENTITY_NAME = "warehouseItem";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final WarehouseItemService warehouseItemService;

    private final WarehouseItemRepository warehouseItemRepository;

    public WarehouseItemResource(WarehouseItemService warehouseItemService, WarehouseItemRepository warehouseItemRepository) {
        this.warehouseItemService = warehouseItemService;
        this.warehouseItemRepository = warehouseItemRepository;
    }

    /**
     * {@code POST  /warehouse-items} : Create a new warehouseItem.
     *
     * @param warehouseItem the warehouseItem to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new warehouseItem, or with status {@code 400 (Bad Request)} if the warehouseItem has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<WarehouseItem> createWarehouseItem(@Valid @RequestBody WarehouseItem warehouseItem) throws URISyntaxException {
        log.debug("REST request to save WarehouseItem : {}", warehouseItem);
        if (warehouseItem.getId() != null) {
            throw new BadRequestAlertException("A new warehouseItem cannot already have an ID", ENTITY_NAME, "idexists");
        }
        warehouseItem = warehouseItemService.save(warehouseItem);
        return ResponseEntity.created(new URI("/api/warehouse-items/" + warehouseItem.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, warehouseItem.getId().toString()))
            .body(warehouseItem);
    }

    /**
     * {@code PUT  /warehouse-items/:id} : Updates an existing warehouseItem.
     *
     * @param id the id of the warehouseItem to save.
     * @param warehouseItem the warehouseItem to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated warehouseItem,
     * or with status {@code 400 (Bad Request)} if the warehouseItem is not valid,
     * or with status {@code 500 (Internal Server Error)} if the warehouseItem couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseItem> updateWarehouseItem(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody WarehouseItem warehouseItem
    ) throws URISyntaxException {
        log.debug("REST request to update WarehouseItem : {}, {}", id, warehouseItem);
        if (warehouseItem.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, warehouseItem.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!warehouseItemRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        warehouseItem = warehouseItemService.update(warehouseItem);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, warehouseItem.getId().toString()))
            .body(warehouseItem);
    }

    /**
     * {@code PATCH  /warehouse-items/:id} : Partial updates given fields of an existing warehouseItem, field will ignore if it is null
     *
     * @param id the id of the warehouseItem to save.
     * @param warehouseItem the warehouseItem to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated warehouseItem,
     * or with status {@code 400 (Bad Request)} if the warehouseItem is not valid,
     * or with status {@code 404 (Not Found)} if the warehouseItem is not found,
     * or with status {@code 500 (Internal Server Error)} if the warehouseItem couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<WarehouseItem> partialUpdateWarehouseItem(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody WarehouseItem warehouseItem
    ) throws URISyntaxException {
        log.debug("REST request to partial update WarehouseItem partially : {}, {}", id, warehouseItem);
        if (warehouseItem.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, warehouseItem.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!warehouseItemRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<WarehouseItem> result = warehouseItemService.partialUpdate(warehouseItem);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, warehouseItem.getId().toString())
        );
    }

    /**
     * {@code GET  /warehouse-items} : get all the warehouseItems.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of warehouseItems in body.
     */
    @GetMapping("")
    public ResponseEntity<List<WarehouseItem>> getAllWarehouseItems(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of WarehouseItems");
        Page<WarehouseItem> page = warehouseItemService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /warehouse-items/:id} : get the "id" warehouseItem.
     *
     * @param id the id of the warehouseItem to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the warehouseItem, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseItem> getWarehouseItem(@PathVariable("id") Long id) {
        log.debug("REST request to get WarehouseItem : {}", id);
        Optional<WarehouseItem> warehouseItem = warehouseItemService.findOne(id);
        return ResponseUtil.wrapOrNotFound(warehouseItem);
    }

    /**
     * {@code DELETE  /warehouse-items/:id} : delete the "id" warehouseItem.
     *
     * @param id the id of the warehouseItem to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouseItem(@PathVariable("id") Long id) {
        log.debug("REST request to delete WarehouseItem : {}", id);
        warehouseItemService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
