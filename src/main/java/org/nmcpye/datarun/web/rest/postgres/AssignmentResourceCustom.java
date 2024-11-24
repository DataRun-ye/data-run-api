package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.drun.common.AssignmentSpecifications;
import org.nmcpye.datarun.drun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.AssignmentServiceCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

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
    protected Specification<Assignment> buildSpecification(Map<String, Object> params) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return super.buildSpecification(params);
        } else if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return null;
        }
        return super.buildSpecification(params)
            .and(AssignmentSpecifications.hasUserWithUsername(SecurityUtils.getCurrentUserLogin().get()));
    }

    @Override
    protected String getName() {
        return "assignments";
    }


    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveOne(Assignment entity) {
        return super.saveOne(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> saveReturnSaved(Assignment entity) {
        return super.saveReturnSaved(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteActivityByIdUid(Long aLong) {
        return super.deleteActivityByIdUid(aLong);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Assignment> updateEntity(Long aLong, Assignment entity) throws URISyntaxException {
        return super.updateEntity(aLong, entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Assignment> entities) {
        return super.saveAll(entities);
    }
}
