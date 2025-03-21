package org.nmcpye.datarun.web.rest.postgres.orgunit;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitGroupSetRepository;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitGroupSetService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.postgres.AbstractJpaResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST Extended controller for managing {@link OrgUnitGroupSet}.
 */
@RestController
@RequestMapping("/api/custom/orgUnitGroupSets")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OrgUnitGroupSetResource extends AbstractJpaResource<OrgUnitGroupSet> {

    private final OrgUnitGroupSetService service;

    public OrgUnitGroupSetResource(OrgUnitGroupSetService service, OrgUnitGroupSetRepository repository) {
        super(service, repository);
        this.service = service;
    }

    @Override
    protected String getName() {
        return "orgUnitGroupSets";
    }


    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<OrgUnitGroupSet> updateEntity(Long aLong, OrgUnitGroupSet entity) throws URISyntaxException {
        return super.updateEntity(aLong, entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(OrgUnitGroupSet entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(OrgUnitGroupSet entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<OrgUnitGroupSet> entities) {
        return super.saveAll(entities);
    }
}
