package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.repository.ActivityRepositoryCustom;
import org.nmcpye.datarun.service.custom.ActivityServiceCustom;
import org.nmcpye.datarun.service.custom.ProjectServiceCustom;
import org.nmcpye.datarun.service.custom.dto.ActivityDTO;
import org.nmcpye.datarun.service.impl.ActivityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ActivityServiceCustomImpl
    implements ActivityServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepositoryCustom activityRepository;

    private final ProjectServiceCustom projectService;

    public ActivityServiceCustomImpl(ActivityRepositoryCustom activityRepository,
                                     ProjectServiceCustom projectService) {
        this.activityRepository = activityRepository;
        this.projectService = projectService;
    }

//    public ActivityDTO saveOrUpdate(ActivityDTO activityDTO) {
//        Activity activity;
//        if (activityDTO.getId() == null || activityDTO.getId().isEmpty()) {
//            // Create new activity
//            activity = activityMapper.toEntity(activityDTO);
//            activity.setUid(UUID.randomUUID().toString());  // Generate a new UID
//        } else {
//            // Update existing activity
//            Activity existingActivity = activityRepository.findByUid(activityDTO.getId())
//                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
//            activityMapper.updateActivityFromDto(activityDTO, existingActivity);
//            activity = existingActivity;
//        }
//        // Set the project
//        Project project = projectService.findByUid(activityDTO.getProjectId().orElse(null));
//        activity.setProject(project);
//
//        Activity savedActivity = activityRepository.save(activity);
//        return activityMapper.toDTO(savedActivity);
//    }
//
//    public ActivityDTO findByUid(String uid) {
//        Activity activity = activityRepository.findByUid(uid)
//            .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
//        return activityMapper.toDTO(activity);
//    }
//
//    public Page<ActivityDTO> findAll(Pageable pageable) {
//        Page<Activity> activities = activityRepository.findAll(pageable);
//        return activities.stream().map(activityMapper::toDTO).collect(Collectors.toList());
//    }
//
//    public void deleteByUid(String uid) {
//        Activity activity = activityRepository.findByUid(uid)
//            .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
//        activityRepository.delete(activity);
//    }
}

