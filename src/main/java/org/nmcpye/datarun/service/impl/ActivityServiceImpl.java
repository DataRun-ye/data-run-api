package org.nmcpye.datarun.service.impl;

import java.util.Optional;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.repository.ActivityRepository;
import org.nmcpye.datarun.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.Activity}.
 */
@Service
@Transactional
public class ActivityServiceImpl implements ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepository activityRepository;

    public ActivityServiceImpl(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public Activity save(Activity activity) {
        log.debug("Request to save Activity : {}", activity);
        return activityRepository.save(activity);
    }

    @Override
    public Activity update(Activity activity) {
        log.debug("Request to update Activity : {}", activity);
        activity.setIsPersisted();
        return activityRepository.save(activity);
    }

    @Override
    public Optional<Activity> partialUpdate(Activity activity) {
        log.debug("Request to partially update Activity : {}", activity);

        return activityRepository
            .findById(activity.getId())
            .map(existingActivity -> {
                if (activity.getUid() != null) {
                    existingActivity.setUid(activity.getUid());
                }
                if (activity.getCode() != null) {
                    existingActivity.setCode(activity.getCode());
                }
                if (activity.getName() != null) {
                    existingActivity.setName(activity.getName());
                }
                if (activity.getStartDate() != null) {
                    existingActivity.setStartDate(activity.getStartDate());
                }
                if (activity.getEndDate() != null) {
                    existingActivity.setEndDate(activity.getEndDate());
                }
                if (activity.getDisabled() != null) {
                    existingActivity.setDisabled(activity.getDisabled());
                }
                if (activity.getDeleteClientData() != null) {
                    existingActivity.setDeleteClientData(activity.getDeleteClientData());
                }
                if (activity.getCreatedBy() != null) {
                    existingActivity.setCreatedBy(activity.getCreatedBy());
                }
                if (activity.getCreatedDate() != null) {
                    existingActivity.setCreatedDate(activity.getCreatedDate());
                }
                if (activity.getLastModifiedBy() != null) {
                    existingActivity.setLastModifiedBy(activity.getLastModifiedBy());
                }
                if (activity.getLastModifiedDate() != null) {
                    existingActivity.setLastModifiedDate(activity.getLastModifiedDate());
                }

                return existingActivity;
            })
            .map(activityRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> findAll(Pageable pageable) {
        log.debug("Request to get all Activities");
        return activityRepository.findAll(pageable);
    }

    public Page<Activity> findAllWithEagerRelationships(Pageable pageable) {
        return activityRepository.findAllWithEagerRelationships(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Activity> findOne(Long id) {
        log.debug("Request to get Activity : {}", id);
        return activityRepository.findOneWithEagerRelationships(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Activity : {}", id);
        activityRepository.deleteById(id);
    }
}
