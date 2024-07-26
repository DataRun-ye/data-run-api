package org.nmcpye.datarun.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.nmcpye.datarun.domain.ChvSupply;
import org.nmcpye.datarun.repository.ChvSupplyRepository;
import org.nmcpye.datarun.service.ChvSupplyService;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link org.nmcpye.datarun.domain.ChvSupply}.
 */
@RestController
@RequestMapping("/api/chv-supplies")
public class ChvSupplyResource {

    private final Logger log = LoggerFactory.getLogger(ChvSupplyResource.class);

    private static final String ENTITY_NAME = "chvSupply";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ChvSupplyService chvSupplyService;

    private final ChvSupplyRepository chvSupplyRepository;

    public ChvSupplyResource(ChvSupplyService chvSupplyService, ChvSupplyRepository chvSupplyRepository) {
        this.chvSupplyService = chvSupplyService;
        this.chvSupplyRepository = chvSupplyRepository;
    }

    /**
     * {@code POST  /chv-supplies} : Create a new chvSupply.
     *
     * @param chvSupply the chvSupply to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new chvSupply, or with status {@code 400 (Bad Request)} if the chvSupply has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ChvSupply> createChvSupply(@Valid @RequestBody ChvSupply chvSupply) throws URISyntaxException {
        log.debug("REST request to save ChvSupply : {}", chvSupply);
        if (chvSupply.getId() != null) {
            throw new BadRequestAlertException("A new chvSupply cannot already have an ID", ENTITY_NAME, "idexists");
        }
        chvSupply = chvSupplyService.save(chvSupply);
        return ResponseEntity.created(new URI("/api/chv-supplies/" + chvSupply.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, chvSupply.getId().toString()))
            .body(chvSupply);
    }

    /**
     * {@code PUT  /chv-supplies/:id} : Updates an existing chvSupply.
     *
     * @param id the id of the chvSupply to save.
     * @param chvSupply the chvSupply to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated chvSupply,
     * or with status {@code 400 (Bad Request)} if the chvSupply is not valid,
     * or with status {@code 500 (Internal Server Error)} if the chvSupply couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChvSupply> updateChvSupply(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ChvSupply chvSupply
    ) throws URISyntaxException {
        log.debug("REST request to update ChvSupply : {}, {}", id, chvSupply);
        if (chvSupply.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, chvSupply.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!chvSupplyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        chvSupply = chvSupplyService.update(chvSupply);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, chvSupply.getId().toString()))
            .body(chvSupply);
    }

    /**
     * {@code PATCH  /chv-supplies/:id} : Partial updates given fields of an existing chvSupply, field will ignore if it is null
     *
     * @param id the id of the chvSupply to save.
     * @param chvSupply the chvSupply to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated chvSupply,
     * or with status {@code 400 (Bad Request)} if the chvSupply is not valid,
     * or with status {@code 404 (Not Found)} if the chvSupply is not found,
     * or with status {@code 500 (Internal Server Error)} if the chvSupply couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ChvSupply> partialUpdateChvSupply(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ChvSupply chvSupply
    ) throws URISyntaxException {
        log.debug("REST request to partial update ChvSupply partially : {}, {}", id, chvSupply);
        if (chvSupply.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, chvSupply.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!chvSupplyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ChvSupply> result = chvSupplyService.partialUpdate(chvSupply);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, chvSupply.getId().toString())
        );
    }

    /**
     * {@code GET  /chv-supplies} : get all the chvSupplies.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of chvSupplies in body.
     */
    @GetMapping("")
    public List<ChvSupply> getAllChvSupplies(@RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        log.debug("REST request to get all ChvSupplies");
        return chvSupplyService.findAll();
    }

    /**
     * {@code GET  /chv-supplies/:id} : get the "id" chvSupply.
     *
     * @param id the id of the chvSupply to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the chvSupply, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChvSupply> getChvSupply(@PathVariable("id") Long id) {
        log.debug("REST request to get ChvSupply : {}", id);
        Optional<ChvSupply> chvSupply = chvSupplyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(chvSupply);
    }

    /**
     * {@code DELETE  /chv-supplies/:id} : delete the "id" chvSupply.
     *
     * @param id the id of the chvSupply to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChvSupply(@PathVariable("id") Long id) {
        log.debug("REST request to delete ChvSupply : {}", id);
        chvSupplyService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
