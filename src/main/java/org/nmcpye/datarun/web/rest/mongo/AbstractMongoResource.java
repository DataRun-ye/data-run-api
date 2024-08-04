package org.nmcpye.datarun.web.rest.mongo;

import jakarta.validation.Valid;
import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.nmcpye.datarun.drun.mongo.service.IdentifiableMongoService;
import org.nmcpye.datarun.service.dto.drun.SaveSummary;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequestMapping("/api/custom")
public abstract class AbstractMongoResource<T extends IdentifiableObject<String>> {

    private final Logger log = LoggerFactory.getLogger(AbstractResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    final protected IdentifiableMongoService<T> identifiableService;
    final protected MongoRepository<T, String> repository;

    protected AbstractMongoResource(IdentifiableMongoService<T> identifiableService,
                                    MongoRepository<T, String> repository) {
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
    public ResponseEntity<PagedResponse<T>> getAllByCurrentUser(@ParameterObject Pageable pageable,
                                                                @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
                                                                @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        if (!paging) {
            pageable = Pageable.unpaged();
        }
        Page<T> page = getList(pageable, eagerload);
        PagedResponse<T> response = new PagedResponse<>();
        response.setPaging(paging);
        response.setPage(page.getNumber());
        response.setPageCount(page.getTotalPages());
        response.setTotal(page.getTotalElements());
        response.setPageSize(page.getSize());
        response.setItems(page.getContent());
        response.setNextPage(createNextPageLink(page));
        response.setEntityName(getName());
        return ResponseEntity.ok(response);
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
    public ResponseEntity<SaveSummary> saveAll(@Valid @RequestBody List<T> entities) {
        SaveSummary summary = new SaveSummary();
        for (T entity : entities) {
            saveEntity(entity, summary);
        }
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    public ResponseEntity<SaveSummary> saveOne(@Valid @RequestBody T entity) {
        SaveSummary summary = new SaveSummary();
            saveEntity(entity, summary);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<T> getActivityById(@PathVariable("id") String id) {
        log.debug("REST request to get from {}: {}", getName(), id);
        Optional<T> activity = identifiableService.findOne(id).or(() -> identifiableService.findByUid(id.toString()));
        return ResponseUtil.wrapOrNotFound(activity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivityByIdUid(@PathVariable("id") String id) {
        log.debug("REST request to delete from {}: {}", getName(), id);
        identifiableService.findOne(id).ifPresent(repository::delete);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), id.toString())).build();
    }


    protected Page<T> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return identifiableService.findAllWithEagerRelationships(pageable);
        }
        return identifiableService.findAll(pageable);
    }

    @PutMapping("/{id}")
    public ResponseEntity<T> updateEntity(
        @PathVariable(value = "id", required = false) final String id,
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
            if (!identifiableService.existsByUid(id)) {
                throw new BadRequestAlertException("Entity not found", getName(), "idnotfound");
            }
        }
        entity = identifiableService.update(entity);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, getName(), entity.getId().toString()))
            .body(entity);
    }

    void saveEntity(T entity, SaveSummary summary) {
        try {
            if (identifiableService.existsByUid(entity.getUid())) {
                identifiableService.update(entity);
                summary.getUpdated().add(entity.getUid());
            } else {
                identifiableService.save(entity);
                summary.getCreated().add(entity.getUid());
            }
        } catch (Exception e) {
            summary.getFailed().put(entity.getUid(), e.getMessage());
        }
    }

    protected abstract String getName();
}
