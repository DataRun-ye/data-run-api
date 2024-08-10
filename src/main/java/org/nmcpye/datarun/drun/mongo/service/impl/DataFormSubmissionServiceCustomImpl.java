package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.AssignmentRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionServiceCustom;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

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
    private final ActivityRelationalRepositoryCustom activityRepository;
    private final AssignmentRepositoryCustom assignmentRepository;
    private final TeamRelationalRepositoryCustom teamRepository;

    public DataFormSubmissionServiceCustomImpl(
        DataFormSubmissionRepositoryCustom dataFormSubmissionRepository,
        ActivityRelationalRepositoryCustom activityRepository,
        AssignmentRepositoryCustom assignmentRepository,
        TeamRelationalRepositoryCustom teamRepository) {
        super(dataFormSubmissionRepository);
        this.dataFormSubmissionRepository = dataFormSubmissionRepository;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public DataFormSubmission saveWithRelations(DataFormSubmission dataFormSubmission) {
        activityRepository.findByUid(dataFormSubmission.getActivity())
            .ifPresentOrElse((a) -> dataFormSubmission.setActivity(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Activity not found: " + dataFormSubmission.getAssignment());
                });
        teamRepository.findByUid(dataFormSubmission.getTeam())
            .ifPresentOrElse((a) -> dataFormSubmission.setTeam(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Team not found: " + dataFormSubmission.getAssignment());
                });
        assignmentRepository.findByUid(dataFormSubmission.getAssignment())
            .ifPresentOrElse((a) -> dataFormSubmission.setAssignment(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Assignment not found: " + dataFormSubmission.getAssignment());
                });


        return dataFormSubmissionRepository.save(dataFormSubmission);
    }
}
