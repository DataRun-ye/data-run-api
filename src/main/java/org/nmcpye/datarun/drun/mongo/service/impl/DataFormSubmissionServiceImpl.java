package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
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
public class DataFormSubmissionServiceImpl
    extends IdentifiableMongoServiceImpl<DataFormSubmission>
    implements DataFormSubmissionService {

    private final Logger log = LoggerFactory.getLogger(DataFormSubmissionServiceImpl.class);

    private final DataFormSubmissionRepositoryCustom dataFormSubmissionRepository;
    private final ActivityRelationalRepositoryCustom activityRepository;
    private final OrgUnitRelationalRepositoryCustom orgUnitRelationalRepositoryCustom;
    private final TeamRelationalRepositoryCustom teamRepository;

    public DataFormSubmissionServiceImpl(
        DataFormSubmissionRepositoryCustom dataFormSubmissionRepository,
        ActivityRelationalRepositoryCustom activityRepository,
        OrgUnitRelationalRepositoryCustom orgUnitRelationalRepositoryCustom,
        TeamRelationalRepositoryCustom teamRepository) {
        super(dataFormSubmissionRepository);
        this.dataFormSubmissionRepository = dataFormSubmissionRepository;
        this.activityRepository = activityRepository;
        this.orgUnitRelationalRepositoryCustom = orgUnitRelationalRepositoryCustom;
        this.teamRepository = teamRepository;
    }

    @Override
    public DataFormSubmission saveWithRelations(DataFormSubmission dataFormSubmission) {
        activityRepository.findByUid(dataFormSubmission.getActivity())
            .ifPresentOrElse((a) -> dataFormSubmission.setActivity(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Activity not found: " + dataFormSubmission.getOrgUnit());
                });
        teamRepository.findByUid(dataFormSubmission.getTeam())
            .ifPresentOrElse((a) -> dataFormSubmission.setTeam(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Team not found: " + dataFormSubmission.getOrgUnit());
                });
        orgUnitRelationalRepositoryCustom.findByUid(dataFormSubmission.getOrgUnit())
            .ifPresentOrElse((a) -> dataFormSubmission.setOrgUnit(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("OrgUnit not found: " + dataFormSubmission.getOrgUnit());
                });


        return dataFormSubmissionRepository.save(dataFormSubmission);
    }
}
