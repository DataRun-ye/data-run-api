package org.nmcpye.datarun.web.rest.postgres.orgunit;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitGroupRepository;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitGroupService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

import static org.nmcpye.datarun.web.rest.postgres.orgunit.OrgUnitGroupResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.postgres.orgunit.OrgUnitGroupResource.V1;

/**
 * REST Extended controller for managing {@link OrgUnitGroup}.
 */
@RestController
@RequestMapping(value = {
    CUSTOM,
    V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OrgUnitGroupResource extends JpaBaseResource<OrgUnitGroup> {
    protected static final String NAME = "/orgUnitGroups";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

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
