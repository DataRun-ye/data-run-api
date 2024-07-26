package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.Assignment;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionServiceCustom;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepositoryCustom;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
public class DataFormSubmissionServiceCustomImpl
    extends IdentifiableMongoServiceImpl<DataFormSubmission>
    implements DataFormSubmissionServiceCustom {

    private final Logger log = LoggerFactory.getLogger(DataFormSubmissionServiceCustomImpl.class);

    private final DataFormSubmissionRepositoryCustom dataFormSubmissionRepository;
    private final ActivityRepositoryCustom activityRepository;
    private final AssignmentRepositoryCustom assignmentRepository;
    private final TeamRepositoryCustom teamRepository;

    public DataFormSubmissionServiceCustomImpl(
        DataFormSubmissionRepositoryCustom dataFormSubmissionRepository,
        ActivityRepositoryCustom activityRepository,
        AssignmentRepositoryCustom assignmentRepository,
        TeamRepositoryCustom teamRepository) {
        super(dataFormSubmissionRepository);
        this.dataFormSubmissionRepository = dataFormSubmissionRepository;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public DataFormSubmission saveWithRelations(DataFormSubmission dataFormSubmission) {
        // Fetch related entities by UID
        Activity activity = activityRepository.findByUid(dataFormSubmission.getActivity())
            .orElseThrow(() -> new PropertyNotFoundException("Activity not found: " + dataFormSubmission.getActivity()));

        Team team = teamRepository.findByUid(dataFormSubmission.getTeam())
            .orElseThrow(() -> new PropertyNotFoundException("Team not found: " + dataFormSubmission.getTeam()));

        Assignment assignment = null;
        if (dataFormSubmission.getAssignment() != null) {
            assignment = assignmentRepository.findByUid(dataFormSubmission.getAssignment())
                .orElseThrow(() -> new PropertyNotFoundException("Assignment not found: " + dataFormSubmission.getAssignment()));
        }

        // Set the fetched entities
        dataFormSubmission.setActivity(activity.getUid());
        dataFormSubmission.setTeam(team.getUid());
        if (assignment != null) {
            dataFormSubmission.setAssignment(assignment.getUid());
        }

        // Generate UID if not present
        if (dataFormSubmission.getUid() == null || dataFormSubmission.getUid().isEmpty()) {
            dataFormSubmission.setUid(CodeGenerator.generateUid());
        }

        // Save the DataFormSubmission entity
        return dataFormSubmissionRepository.save(dataFormSubmission);
    }

    @Override
    public Optional<DataFormSubmission> partialUpdate(DataFormSubmission dataFormSubmission) {
        log.debug("Request to partially update DataFormSubmission : {}", dataFormSubmission);

        return dataFormSubmissionRepository
            .findById(dataFormSubmission.getId())
            .map(existingDataFormSubmission -> {
                if (dataFormSubmission.getUid() != null) {
                    existingDataFormSubmission.setUid(dataFormSubmission.getUid());
                }
                if (dataFormSubmission.getDeleted() != null) {
                    existingDataFormSubmission.setDeleted(dataFormSubmission.getDeleted());
                }
                if (dataFormSubmission.getStartEntryTime() != null) {
                    existingDataFormSubmission.setStartEntryTime(dataFormSubmission.getStartEntryTime());
                }
                if (dataFormSubmission.getFinishedEntryTime() != null) {
                    existingDataFormSubmission.setFinishedEntryTime(dataFormSubmission.getFinishedEntryTime());
                }
                if (dataFormSubmission.getComment() != null) {
                    existingDataFormSubmission.setComment(dataFormSubmission.getComment());
                }
                if (dataFormSubmission.getStatus() != null) {
                    existingDataFormSubmission.setStatus(dataFormSubmission.getStatus());
                }

                return existingDataFormSubmission;
            })
            .map(dataFormSubmissionRepository::save);
    }
}
