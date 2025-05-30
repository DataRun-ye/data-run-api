package org.nmcpye.datarun.web.rest.postgres.activity;

import org.nmcpye.datarun.activity.Activity;
import org.nmcpye.datarun.activity.repository.ActivityRepository;
import org.nmcpye.datarun.drun.postgres.service.ActivityService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

import static org.nmcpye.datarun.web.rest.postgres.activity.ActivityResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.postgres.activity.ActivityResource.V1;

/**
 * REST Extended controller for managing {@link Activity}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class ActivityResource extends JpaBaseResource<Activity> {

    protected static final String NAME = "/activities";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;
    private final Logger log = LoggerFactory.getLogger(ActivityResource.class);

    private final ActivityService activityService;

    private final ActivityRepository activityRepository;

    public ActivityResource(ActivityService activityService, ActivityRepository activityRepository) {
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
    public ResponseEntity<Activity> updateEntity(String uid, Activity entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Activity> entities) {
        return super.saveAll(entities);
    }
}
