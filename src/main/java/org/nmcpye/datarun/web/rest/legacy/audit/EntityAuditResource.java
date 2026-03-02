package org.nmcpye.datarun.web.rest.legacy.audit;

import org.nmcpye.datarun.jpa.auditing.EntityAuditEvent;
import org.nmcpye.datarun.jpa.auditing.repository.EntityAuditEventRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

import java.util.List;

/**
 * REST controller for getting the audit events for entity
 */
@RestController
@RequestMapping("/api")
@Transactional
public class EntityAuditResource {

    private final Logger log = LoggerFactory.getLogger(EntityAuditResource.class);

    private final EntityAuditEventRepository entityAuditEventRepository;

    public EntityAuditResource(EntityAuditEventRepository entityAuditEventRepository) {
        this.entityAuditEventRepository = entityAuditEventRepository;
    }


    /**
     * NMC: Stream audit events since a given sequence ID.
     * Clients keep the last seen sequence and pass it back.
     */
    @GetMapping("/stream")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<EntityAuditEvent>> stream(
        @RequestParam(name = "sinceId", defaultValue = "0") Long sinceId,
        @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        var page = PageRequest.of(0, limit, Sort.by("id").ascending());
        Page<EntityAuditEvent> results = entityAuditEventRepository.findAllByIdGreaterThan(sinceId, page);

        // Tell client the highest ID they just received
        long maxId = results.stream()
            .mapToLong(EntityAuditEvent::getId)
            .max()
            .orElse(sinceId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Last-Id", Long.toString(maxId));

        return new ResponseEntity<>(results.getContent(), headers, HttpStatus.OK);
    }

    /**
     * fetches all the audited entity types
     *
     * @return
     */
    @GetMapping(value = "/audits/entity/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public List<String> getAuditedEntities() {
        return entityAuditEventRepository.findAllEntityTypes();
    }

    /**
     * fetches the last 100 change list for an entity class, if limit is passed fetches that many changes
     *
     */
    @GetMapping(value = "/audits/entity/changes", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<EntityAuditEvent>> getChanges(
        @RequestParam(value = "entityType") String entityType,
        @RequestParam(value = "limit") int limit
    ) {
        log.debug("REST request to get a page of EntityAuditEvents");
        Page<EntityAuditEvent> page = entityAuditEventRepository.findAllByEntityType(entityType, PageRequest.of(0, limit));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * fetches a previous version for an entity class and id
     *
     * @return
     */
    @GetMapping(value = "/audits/entity/changes/version/previous", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntityAuditEvent> getPrevVersion(
        @RequestParam(value = "qualifiedName") String qualifiedName,
        @RequestParam(value = "entityId") String entityId,
        @RequestParam(value = "commitVersion") Integer commitVersion
    ) {
        EntityAuditEvent prev = entityAuditEventRepository.findOneByEntityTypeAndEntityIdAndCommitVersion(
            qualifiedName,
            entityId,
            commitVersion
        );
        return new ResponseEntity<>(prev, HttpStatus.OK);
    }
}
