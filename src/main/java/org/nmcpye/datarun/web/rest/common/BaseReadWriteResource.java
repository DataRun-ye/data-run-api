package org.nmcpye.datarun.web.rest.common;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.DRunApiVersion;
import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.common.IdentifiableObjectRepository;
import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.mvc.annotation.ApiVersion;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.jhipster.web.util.HeaderUtil;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

@ApiVersion({DRunApiVersion.DEFAULT, DRunApiVersion.ALL})
@Slf4j
public abstract class BaseReadWriteResource<T extends IdentifiableObject<ID>, ID extends Serializable>
    extends BaseReadResource<T, ID> {

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    protected BaseReadWriteResource(IdentifiableObjectService<T, ID> identifiableObjectService,
                                    IdentifiableObjectRepository<T, ID> repository) {
        super(identifiableObjectService, repository);
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

    protected T preProcess(T entity) {
        // do nothing
        return entity;
    }

    protected void saveEntity(T entity, EntitySaveSummaryVM summary) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        hasMinimalRightsOrThrow(user);
        var processedEntity = preProcess(entity);
        try {
            if (processedEntity.getUid() != null && identifiableObjectService.existsByUid(processedEntity.getUid())) {
                if (!aclService.canUpdate(entity, user)) {
                    processedEntity = identifiableObjectService.update(processedEntity);
                    summary.getUpdated().add(processedEntity.getUid());
                } else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no right to send things here");
                }
            } else {
                if (aclService.canAddNew(entity, user)) {
                    processedEntity = identifiableObjectService.saveWithRelations(processedEntity);
                    summary.getCreated().add(processedEntity.getUid());
                } else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no right to send things here");
                }
            }
        } catch (Exception e) {
            log.error("REST Error Saving entity {}:{}", getEntityClass().getSimpleName(), processedEntity.getUid());
            summary.getFailed().put(processedEntity.getUid(), e.getMessage());
            throw new IllegalQueryException(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("id") String id,
                                              @AuthenticationPrincipal CurrentUserDetails user) {
        hasMinimalRightsOrThrow(user);
        log.debug("REST request to delete from {}: {}", getName(), id);
        final var entity = identifiableObjectService.findByUid(id).orElseThrow();
        if (aclService.canDelete(entity, user)) {
            identifiableObjectService.delete(entity);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), id)).build();
    }

    @PutMapping("/{uid}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<T> updateEntity(
        @PathVariable(value = "uid", required = false) final String uid,
        @Valid @RequestBody T entity, @AuthenticationPrincipal CurrentUserDetails user
    ) throws URISyntaxException {
        hasMinimalRightsOrThrow(user);
        log.debug("REST request to delete from {}: {}", getName(), uid);
        if (entity.getUid() == null) {
            throw new BadRequestAlertException("Invalid uid", getName(), "uid is null");
        }

        if (!Objects.equals(uid, entity.getUid())) {
            throw new BadRequestAlertException("Invalid ID", getName(), "idinvalid");
        }

        if (aclService.canUpdate(entity, user)) {
            entity = identifiableObjectService.update(entity);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }


        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, getName(), entity.getId().toString()))
            .body(entity);
    }
}
