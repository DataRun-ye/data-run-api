package org.nmcpye.datarun.web.rest.v1.dataelementgroup;

import org.nmcpye.datarun.drun.postgres.domain.DataElementGroup;
import org.nmcpye.datarun.drun.postgres.repository.DataElementGroupRepository;
import org.nmcpye.datarun.drun.postgres.service.DataElementGroupService;
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

import static org.nmcpye.datarun.web.rest.v1.dataelementgroup.DataElementGroupResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.v1.dataelementgroup.DataElementGroupResource.V1;

/**
 * REST Extended controller for managing {@link DataElementGroup}.
 */
@RestController
@RequestMapping(value = {
    CUSTOM,
    V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataElementGroupResource extends JpaBaseResource<DataElementGroup> {
    protected static final String NAME = "/dataElementGroups";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    public DataElementGroupResource(DataElementGroupService service, DataElementGroupRepository repository) {
        super(service, repository);
    }

    @Override
    protected String getName() {
        return "dataElementGroups";
    }


    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<DataElementGroup> updateEntity(String uid, DataElementGroup entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(DataElementGroup entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataElementGroup entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataElementGroup> entities) {
        return super.saveAll(entities);
    }
}
