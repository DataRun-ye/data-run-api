package org.nmcpye.datarun.web.rest.common;

import jakarta.validation.Valid;
import org.nmcpye.datarun.common.IdentifiableRepository;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

//@RequestMapping("/api/custom")
public abstract class AbstractResourceReadWrite<T extends IdentifiableEntity<ID>, ID extends Serializable>
    extends AbstractResourceRead<T, ID> {

    protected final Logger log = LoggerFactory.getLogger(AbstractResourceReadWrite.class);

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    protected AbstractResourceReadWrite(IdentifiableService<T, ID> identifiableService,
                                        IdentifiableRepository<T, ID> repository) {
        super(identifiableService, repository);
    }

    @PostMapping("/bulk")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<T> entities) {
        log.debug("REST request to saveAll all {}", getName());
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
            log.debug("REST Error Saving submission {}: {}", e.toString(), entity.getCreatedBy());
            summary.getFailed().put(entity.getUid(), e.getMessage());
            throw new IllegalQueryException(e.getMessage());
        }
    }

    @DeleteMapping("/{uid}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("uid") String uid) {
        log.debug("REST request to delete from {}: {}", getName(), uid);
        identifiableService.findByUid(uid).ifPresent((entity) -> identifiableService.deleteByUid(entity.getUid()));
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), uid.toString())).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
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
}
