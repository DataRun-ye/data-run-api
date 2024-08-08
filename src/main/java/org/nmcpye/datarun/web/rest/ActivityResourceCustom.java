package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ActivityServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

import java.util.Optional;

/**
 * REST Extended controller for managing {@link Activity}.
 */
@RestController
@RequestMapping("/api/custom/activities")
public class ActivityResourceCustom extends AbstractResource<Activity> {

    private final Logger log = LoggerFactory.getLogger(ActivityResourceCustom.class);

    private final ActivityServiceCustom activityService;

    private final ActivityRelationalRepositoryCustom activityRepository;

    public ActivityResourceCustom(ActivityServiceCustom activityService, ActivityRelationalRepositoryCustom activityRepository) {
        super(activityService, activityRepository);
        this.activityRepository = activityRepository;
        this.activityService = activityService;
    }

    /**
     * {@code GET  /activities/:id} : get the "id" activity.
     *
     * @param id the id of the assignment to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the activity, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Activity> getOneByUser(@PathVariable("id") Long id) {
        log.debug("REST request to get Activity : {}", id);
        Optional<Activity> activity = activityService.findOne(id);
        return ResponseUtil.wrapOrNotFound(activity);
    }

    @Override
    protected Page<Activity> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return activityService.findAllWithEagerRelationships(pageable);
        } else {
            return activityService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "activities";
    }

}
