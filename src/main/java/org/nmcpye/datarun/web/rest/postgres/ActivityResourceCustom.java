package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ActivityServiceCustom;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST Extended controller for managing {@link Activity}.
 */
@RestController
@RequestMapping("/api/custom/activities")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class ActivityResourceCustom extends AbstractRelationalResource<Activity> {

    private final Logger log = LoggerFactory.getLogger(ActivityResourceCustom.class);

    private final ActivityServiceCustom activityService;

    private final ActivityRelationalRepositoryCustom activityRepository;

    public ActivityResourceCustom(ActivityServiceCustom activityService, ActivityRelationalRepositoryCustom activityRepository) {
        super(activityService, activityRepository);
        this.activityRepository = activityRepository;
        this.activityService = activityService;
    }

    @Override
    protected String getName() {
        return "activities";
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveOne(Activity entity) {
        return super.saveOne(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> saveReturnSaved(Activity entity) {
        return super.saveReturnSaved(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Activity> updateEntity(Long aLong, Activity entity) throws URISyntaxException {
        return super.updateEntity(aLong, entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Activity> entities) {
        return super.saveAll(entities);
    }
}
