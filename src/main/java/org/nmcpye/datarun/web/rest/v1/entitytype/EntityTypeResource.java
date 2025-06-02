package org.nmcpye.datarun.web.rest.v1.entitytype;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityType.repository.EntityTypeRepository;
import org.nmcpye.datarun.jpa.entityType.service.EntityTypeService;
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
 * REST controller for managing {@link EntityType}.
 */
@RestController
@RequestMapping(value = {EntityTypeResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class EntityTypeResource extends JpaBaseResource<EntityType> {
    protected static final String NAME = "/entityTypes";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final EntityTypeService entityTypeService;

    protected EntityTypeResource(EntityTypeService service,
                                 EntityTypeRepository repository) {
        super(service, repository);
        this.entityTypeService = service;
    }

    @Override
    protected String getName() {
        return "entityTypes";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<EntityType> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(EntityType entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(EntityType entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }
}
