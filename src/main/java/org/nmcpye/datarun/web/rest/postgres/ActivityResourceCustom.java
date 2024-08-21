package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ActivityServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Extended controller for managing {@link Activity}.
 */
@RestController
@RequestMapping("/api/custom/activities")
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

}
