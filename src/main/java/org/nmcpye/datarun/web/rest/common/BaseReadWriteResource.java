package org.nmcpye.datarun.web.rest.common;

import jakarta.validation.Valid;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
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

public abstract class BaseReadWriteResource<T extends AuditableObject<ID>, ID extends Serializable>
    extends BaseReadResource<T, ID> {

    protected final Logger log = LoggerFactory.getLogger(BaseReadWriteResource.class);

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    protected BaseReadWriteResource(AuditableObjectService<T, ID> auditableObjectService,
                                    AuditableObjectRepository<T, ID> repository) {
        super(auditableObjectService, repository);
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
        log.info("Request to save and return {}:{}", entity.getClass().getSimpleName(), entity.getUid());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        saveEntity(entity, summary);
        if (entity.getUid() != null) {
            return ResponseEntity.ok(entity);
        }
        return ResponseEntity.ok(summary);
    }

    protected void saveEntity(T entity, EntitySaveSummaryVM summary) {
        try {
            if (entity.getUid() != null && auditableObjectService.existsByUid(entity.getUid())) {
                entity = auditableObjectService.update(entity);
                summary.getUpdated().add(entity.getUid());
            } else {
                entity = auditableObjectService.saveWithRelations(entity);
                summary.getCreated().add(entity.getUid());
            }
        } catch (Exception e) {
            log.error("REST Error Saving entity {}:{}", getEntityClass().getSimpleName(), entity.getUid());
            summary.getFailed().put(entity.getUid(), e.getMessage());
            throw new IllegalQueryException(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("id") String id) {
        log.debug("REST request to delete from {}: {}", getName(), id);
        auditableObjectService.findByUid(id).ifPresent(auditableObjectService::delete);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), id.toString())).build();
    }

    @PutMapping("/{uid}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<T> updateEntity(
        @PathVariable(value = "uid", required = false) final String uid,
        @Valid @RequestBody T entity
    ) throws URISyntaxException {
        log.debug("REST request to delete from {}: {}", getName(), uid);
        if (entity.getUid() == null) {
            throw new BadRequestAlertException("Invalid uid", getName(), "uid is null");
        }

        if (!Objects.equals(uid, entity.getUid())) {
            throw new BadRequestAlertException("Invalid ID", getName(), "idinvalid");
        }

        entity = auditableObjectService.update(entity);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, getName(), entity.getId().toString()))
            .body(entity);
    }
}
