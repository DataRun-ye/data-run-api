package org.nmcpye.datarun.web.rest.postgres.orgunit;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitGroupRepository;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitGroupService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.postgres.AbstractJpaResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST Extended controller for managing {@link OrgUnitGroup}.
 */
@RestController
@RequestMapping("/api/custom/orgUnitGroups")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OrgUnitGroupResource extends AbstractJpaResource<OrgUnitGroup> {

    private final Logger log = LoggerFactory.getLogger(OrgUnitGroupResource.class);

    private final OrgUnitGroupService service;

    public OrgUnitGroupResource(OrgUnitGroupService service, OrgUnitGroupRepository repository) {
        super(service, repository);
        this.service = service;
    }

//    @Override
//    Specification<OrgUnitGroup> buildSpecification(QueryRequest queryRequest) {
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return super.buildSpecification(queryRequest);
//        } else if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
//            return null;
//        }
//        return super.buildSpecification(queryRequest).and(service.canRead());
//    }

    @Override
    protected String getName() {
        return "orgUnitGroups";
    }


    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<OrgUnitGroup> updateEntity(Long aLong, OrgUnitGroup entity) throws URISyntaxException {
        return super.updateEntity(aLong, entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(OrgUnitGroup entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(OrgUnitGroup entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<OrgUnitGroup> entities) {
        return super.saveAll(entities);
    }
}
