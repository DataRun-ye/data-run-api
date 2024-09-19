package org.nmcpye.datarun.web.rest.common;

import jakarta.validation.Valid;
import org.nmcpye.datarun.drun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequestMapping("/api/custom")
public abstract class AbstractResource<T extends IdentifiableObject<ID>, ID extends Serializable> {

    private final Logger log = LoggerFactory.getLogger(AbstractResource.class);

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    final protected IdentifiableService<T, ID> identifiableService;
    final protected CrudRepository<T, ID> repository;

    protected AbstractResource(IdentifiableService<T, ID> identifiableService,
                               CrudRepository<T, ID> repository) {
        this.identifiableService = identifiableService;
        this.repository = repository;
    }

    /**
     * {@code GET  /Ts} : get all the entities.
     *
     * @param pageable  the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is for internal use only and should not be set by clients)
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of assignments in body.
     */
    @GetMapping("")
    public ResponseEntity<PagedResponse<T>> getAllFiltered(@ParameterObject Pageable pageable,
                                                           @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
                                                           @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        if (!paging) {
            pageable = Pageable.unpaged();
        }
        Page<T> page = getList(pageable, eagerload);
        PagedResponse<T> response = initPageResponse(paging, page);
        return ResponseEntity.ok(response);
    }

    protected PagedResponse<T> initPageResponse(boolean paging, Page<T> page) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setPaging(paging);
        response.setPage(page.getNumber());
        response.setPageCount(page.getTotalPages());
        response.setTotal(page.getTotalElements());
        response.setPageSize(page.getSize());
        response.setItems(page.getContent());
        response.setNextPage(createNextPageLink(page));
        response.setEntityName(getName());
        return response;
    }

    private String createNextPageLink(Page<?> page) {
        if (page.hasNext()) {
            return ServletUriComponentsBuilder.fromCurrentRequest()
                .queryParam("page", page.getNumber() + 2) // page is 0-based, but we display it 1-based
                .toUriString();
        } else {
            return null;
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<T> entities) {
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        for (T entity : entities) {
            saveEntity(entity, summary);
        }
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    public ResponseEntity<EntitySaveSummaryVM> saveOne(@Valid @RequestBody T entity) {
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        saveEntity(entity, summary);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/return")
    public ResponseEntity<?> saveReturnSaved(@Valid @RequestBody T entity) {
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        saveEntity(entity, summary);
        if (entity.getId() != null) {
            return ResponseEntity.ok(entity);
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<T> getActivityById(@PathVariable("id") ID id) {
        log.debug("REST request to get from {}: {}", getName(), id);
        Optional<T> activity = identifiableService.findOne(id).or(() -> identifiableService.findByUid(id.toString()));
        return ResponseUtil.wrapOrNotFound(activity);
    }

    void saveEntity(T entity, EntitySaveSummaryVM summary) {
        try {
            if (identifiableService.existsByUid(entity.getUid())) {
                entity = identifiableService.update(entity);
                summary.getUpdated().add(entity.getUid());
            } else {
                entity = identifiableService.save(entity);
                summary.getCreated().add(entity.getUid());
            }
        } catch (Exception e) {
            summary.getFailed().put(entity.getUid() + ':' + entity.getCode() + ':' + entity.getName(), e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivityByIdUid(@PathVariable("id") ID id) {
        log.debug("REST request to delete from {}: {}", getName(), id);
        identifiableService.findOne(id).ifPresent(repository::delete);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), id.toString())).build();
    }


    protected Page<T> getList(Pageable pageable, boolean eagerload) {
//        if (eagerload) {
//            return identifiableService.findAllWithEagerRelationships(pageable);
//        }
//        return identifiableService.findAll(pageable);
        return identifiableService.findAllByUser(pageable);
    }

    @PutMapping("/{id}")
    public ResponseEntity<T> updateEntity(
        @PathVariable(value = "id", required = false) final ID id,
        @Valid @RequestBody T entity
    ) throws URISyntaxException {
        log.debug("REST request to delete from {}: {}", getName(), id);
        if (entity.getId() == null) {
            throw new BadRequestAlertException("Invalid id", getName(), "idnull");
        }
        if (!Objects.equals(id, entity.getId())) {
            if (!Objects.equals(id, entity.getUid())) {
                throw new BadRequestAlertException("Invalid ID", getName(), "idinvalid");
            }
        }

        if (!identifiableService.existsById(id)) {
            if (id instanceof String && !identifiableService.existsByUid(id.toString())) {
                throw new BadRequestAlertException("Entity not found", getName(), "idnotfound");
            }
        }
        entity = identifiableService.update(entity);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, getName(), entity.getId().toString()))
            .body(entity);
    }

    protected abstract String getName();
}
