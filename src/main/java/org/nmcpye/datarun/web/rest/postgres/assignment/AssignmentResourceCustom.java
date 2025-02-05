package org.nmcpye.datarun.web.rest.postgres.assignment;

import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.AssignmentServiceCustom;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.nmcpye.datarun.web.rest.exception.PathUpdateException;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.postgres.AbstractRelationalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST Extended controller for managing {@link Assignment}.
 */
@RestController
@RequestMapping("/api/custom/assignments")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class AssignmentResourceCustom
    extends AbstractRelationalResource<Assignment> {

    private final Logger log = LoggerFactory.getLogger(AssignmentResourceCustom.class);

    private final AssignmentServiceCustom assignmentService;

    private final AssignmentRelationalRepositoryCustom assignmentRepository;

    public AssignmentResourceCustom(AssignmentServiceCustom assignmentService, AssignmentRelationalRepositoryCustom assignmentRepository) {
        super(assignmentService, assignmentRepository);
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
    }

    @Override
    protected Page<Assignment> getList(Pageable pageable, QueryRequest queryRequest) {
        if (queryRequest.getFilters().isEmpty()) {
            return assignmentService.getAllUserAccessible(pageable);
        }

        return super.getList(pageable, queryRequest);
    }

//    @Override
//    public Specification<Assignment> buildSpecification(QueryRequest queryRequest) {
//        Specification<Assignment> spec = super.buildSpecification(queryRequest);
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return spec;
//        } else if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
//            return null;
//        }
//        return spec
//            .and(assignmentService.canRead());
//    }

    @Override
    protected String getName() {
        return "assignments";
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveOne(Assignment entity) {
        log.debug("REST request to saveOne {}", getName());
        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
        }
        return super.saveOne(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> saveReturnSaved(Assignment entity) {
        log.debug("REST request to saveOne, return saved {}", getName());
        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
        }
        return super.saveReturnSaved(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Assignment> updateEntity(Long aLong, Assignment entity) throws URISyntaxException {
        return super.updateEntity(aLong, entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Assignment> entities) {
        log.debug("REST request to saveAll {}", getName());
        var withIds = entities.stream().filter((entity) -> entity.getId() != null).map(Identifiable::getId).toList();
        if (!withIds.isEmpty()) {
            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + withIds, "idexists");
        }

        return super.saveAll(entities);
    }

    @GetMapping("/updatePaths")
    public ResponseEntity<String> updatePaths(
        @RequestParam(name = "forceUpdate", required = false, defaultValue = "false") boolean forceUpdate) {
        log.debug("REST request to update orgUnit Paths");

        try {
            if (forceUpdate) {
                assignmentService.forceUpdatePaths();
            } else {
                assignmentService.updatePaths();
            }
            return ResponseEntity.ok("Paths updated successfully");
        } catch (Exception e) {
            log.error("Error occurred while updating paths", e);
            throw new PathUpdateException("Failed to update paths", e);
        }
    }
}
