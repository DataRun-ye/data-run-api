package org.nmcpye.datarun.web.rest.v1.dataelementgroupset;

import org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet;
import org.nmcpye.datarun.jpa.dataelementgroupset.repository.DataElementGroupSetRepository;
import org.nmcpye.datarun.jpa.dataelementgroupset.service.DataElementGroupSetService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.v1.dataelementgroupset.DataElementGroupSetResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.v1.dataelementgroupset.DataElementGroupSetResource.V1;

/**
 * REST Extended controller for managing {@link DataElementGroupSet}.
 */
@RestController
@RequestMapping(value = {
    CUSTOM,
    V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataElementGroupSetResource extends JpaBaseResource<DataElementGroupSet> {
    protected static final String NAME = "/dataElementGroupSets";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final DataElementGroupSetService service;

    public DataElementGroupSetResource(DataElementGroupSetService service,
                                       DataElementGroupSetRepository repository) {
        super(service, repository);
        this.service = service;
    }

    @Override
    protected String getName() {
        return "dataElementGroupSets";
    }


//    @Override
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    public ResponseEntity<DataElementGroupSet> updateEntity(String uid, DataElementGroupSet entity) throws URISyntaxException {
//        return super.updateEntity(uid, entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<?> saveReturnSaved(DataElementGroupSet entity) {
//        return super.saveReturnSaved(entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataElementGroupSet entity) {
//        return super.saveOne(entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataElementGroupSet> entities) {
//        return super.saveAll(entities);
//    }
}
