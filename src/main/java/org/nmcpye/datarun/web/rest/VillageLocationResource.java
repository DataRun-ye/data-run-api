package org.nmcpye.datarun.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.nmcpye.datarun.domain.VillageLocation;
import org.nmcpye.datarun.repository.VillageLocationRepository;
import org.nmcpye.datarun.service.VillageLocationService;
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
 * REST controller for managing {@link org.nmcpye.datarun.domain.VillageLocation}.
 */
@RestController
@RequestMapping("/api/village-locations")
public class VillageLocationResource {

    private static final Logger log = LoggerFactory.getLogger(VillageLocationResource.class);

    private static final String ENTITY_NAME = "villageLocation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final VillageLocationService villageLocationService;

    private final VillageLocationRepository villageLocationRepository;

    public VillageLocationResource(VillageLocationService villageLocationService, VillageLocationRepository villageLocationRepository) {
        this.villageLocationService = villageLocationService;
        this.villageLocationRepository = villageLocationRepository;
    }

    /**
     * {@code POST  /village-locations} : Create a new villageLocation.
     *
     * @param villageLocation the villageLocation to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new villageLocation, or with status {@code 400 (Bad Request)} if the villageLocation has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<VillageLocation> createVillageLocation(@Valid @RequestBody VillageLocation villageLocation)
        throws URISyntaxException {
        log.debug("REST request to save VillageLocation : {}", villageLocation);
        if (villageLocation.getId() != null) {
            throw new BadRequestAlertException("A new villageLocation cannot already have an ID", ENTITY_NAME, "idexists");
        }
        villageLocation = villageLocationService.save(villageLocation);
        return ResponseEntity.created(new URI("/api/village-locations/" + villageLocation.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, villageLocation.getId().toString()))
            .body(villageLocation);
    }

    /**
     * {@code PUT  /village-locations/:id} : Updates an existing villageLocation.
     *
     * @param id the id of the villageLocation to save.
     * @param villageLocation the villageLocation to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated villageLocation,
     * or with status {@code 400 (Bad Request)} if the villageLocation is not valid,
     * or with status {@code 500 (Internal Server Error)} if the villageLocation couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<VillageLocation> updateVillageLocation(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody VillageLocation villageLocation
    ) throws URISyntaxException {
        log.debug("REST request to update VillageLocation : {}, {}", id, villageLocation);
        if (villageLocation.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, villageLocation.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!villageLocationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        villageLocation = villageLocationService.update(villageLocation);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, villageLocation.getId().toString()))
            .body(villageLocation);
    }

    /**
     * {@code PATCH  /village-locations/:id} : Partial updates given fields of an existing villageLocation, field will ignore if it is null
     *
     * @param id the id of the villageLocation to save.
     * @param villageLocation the villageLocation to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated villageLocation,
     * or with status {@code 400 (Bad Request)} if the villageLocation is not valid,
     * or with status {@code 404 (Not Found)} if the villageLocation is not found,
     * or with status {@code 500 (Internal Server Error)} if the villageLocation couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<VillageLocation> partialUpdateVillageLocation(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody VillageLocation villageLocation
    ) throws URISyntaxException {
        log.debug("REST request to partial update VillageLocation partially : {}, {}", id, villageLocation);
        if (villageLocation.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, villageLocation.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!villageLocationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<VillageLocation> result = villageLocationService.partialUpdate(villageLocation);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, villageLocation.getId().toString())
        );
    }

    /**
     * {@code GET  /village-locations} : get all the villageLocations.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of villageLocations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<VillageLocation>> getAllVillageLocations(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of VillageLocations");
        Page<VillageLocation> page = villageLocationService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /village-locations/:id} : get the "id" villageLocation.
     *
     * @param id the id of the villageLocation to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the villageLocation, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VillageLocation> getVillageLocation(@PathVariable("id") Long id) {
        log.debug("REST request to get VillageLocation : {}", id);
        Optional<VillageLocation> villageLocation = villageLocationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(villageLocation);
    }

    /**
     * {@code DELETE  /village-locations/:id} : delete the "id" villageLocation.
     *
     * @param id the id of the villageLocation to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVillageLocation(@PathVariable("id") Long id) {
        log.debug("REST request to delete VillageLocation : {}", id);
        villageLocationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
