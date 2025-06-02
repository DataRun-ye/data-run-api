package org.nmcpye.datarun.web.rest.v1.assignmenttype;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.assignmenttype.AssignmentType;
import org.nmcpye.datarun.jpa.assignmenttype.repository.AssignmentTypeRepository;
import org.nmcpye.datarun.jpa.assignmenttype.service.AssignmentTypeService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * REST controller for managing {@link AssignmentType}.
 */
@RestController
@RequestMapping(value = {AssignmentTypeResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class AssignmentTypeResource extends JpaBaseResource<AssignmentType> {
    protected static final String NAME = "/assignmentTypes";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final AssignmentTypeService assignmentTypeService;

    protected AssignmentTypeResource(AssignmentTypeService service,
                                     AssignmentTypeRepository repository) {
        super(service, repository);
        this.assignmentTypeService = service;
    }

    @Override
    protected String getName() {
        return "assignmentTypes";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<AssignmentType> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(AssignmentType entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(AssignmentType entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }
}
