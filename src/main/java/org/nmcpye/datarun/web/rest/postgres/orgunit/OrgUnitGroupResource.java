package org.nmcpye.datarun.web.rest.postgres.orgunit;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitGroupRepository;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitGroupService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
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
public class OrgUnitGroupResource extends JpaBaseResource<OrgUnitGroup> {

    public OrgUnitGroupResource(OrgUnitGroupService service, OrgUnitGroupRepository repository) {
        super(service, repository);
    }

    @Override
    protected String getName() {
        return "orgUnitGroups";
    }


    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<OrgUnitGroup> updateEntity(String uid, OrgUnitGroup entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
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
