package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.AssignmentServiceCustom;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST Extended controller for managing {@link Assignment}.
 */
@RestController
@RequestMapping("/api/custom/assignments")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class AssignmentResourceCustom extends AbstractRelationalResource<Assignment> {

    private final Logger log = LoggerFactory.getLogger(AssignmentResourceCustom.class);

    private final AssignmentServiceCustom assignmentService;

    private final AssignmentRelationalRepositoryCustom assignmentRepository;

    public AssignmentResourceCustom(AssignmentServiceCustom assignmentService, AssignmentRelationalRepositoryCustom assignmentRepository) {
        super(assignmentService, assignmentRepository);
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
    }

    @Override
    protected Specification<Assignment> buildSpecification(QueryRequest queryRequest) {
        Specification<Assignment> spec = super.buildSpecification(queryRequest);
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return spec;
        } else if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return null;
        }
        return spec
            .and(assignmentService.hasAccess());
    }

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
}
