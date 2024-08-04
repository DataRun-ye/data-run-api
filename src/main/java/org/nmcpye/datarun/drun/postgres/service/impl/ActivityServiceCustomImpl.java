package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ActivityServiceCustom;
import org.nmcpye.datarun.service.impl.ActivityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class ActivityServiceCustomImpl
    extends IdentifiableServiceImpl<Activity>
    implements ActivityServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepositoryCustom activityRepository;


    public ActivityServiceCustomImpl(ActivityRepositoryCustom activityRepository) {
        super(activityRepository);
        this.activityRepository = activityRepository;
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
}
