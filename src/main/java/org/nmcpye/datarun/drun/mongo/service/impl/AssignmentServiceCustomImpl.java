package org.nmcpye.datarun.drun.mongo.service.impl;

import org.nmcpye.datarun.drun.mongo.domain.AssignmentMongo;
import org.nmcpye.datarun.drun.mongo.repository.AssignmentRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.AssignmentServiceCustom;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service Implementation for managing {@link AssignmentMongo}.
 */
@Service
@Primary
public class AssignmentServiceCustomImpl
    extends IdentifiableMongoServiceImpl<AssignmentMongo>
    implements AssignmentServiceCustom {

    private final Logger log = LoggerFactory.getLogger(AssignmentServiceCustomImpl.class);

    private final AssignmentRepositoryCustom dataFormRepository;

    private final ActivityRelationalRepositoryCustom activityRepository;

    private final TeamRelationalRepositoryCustom teamRepository;

    public AssignmentServiceCustomImpl(AssignmentRepositoryCustom dataFormRepository, ActivityRelationalRepositoryCustom activityRepository, TeamRelationalRepositoryCustom teamRepository) {
        super(dataFormRepository);
        this.dataFormRepository = dataFormRepository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public Optional<AssignmentMongo> partialUpdate(AssignmentMongo dataForm) {
        log.debug("Request to partially update Assignment : {}", dataForm);

        return dataFormRepository
            .findById(dataForm.getId())
            .map(existingAssignment -> {
                if (dataForm.getUid() != null) {
                    existingAssignment.setUid(dataForm.getUid());
                }
                if (dataForm.getCode() != null) {
                    existingAssignment.setCode(dataForm.getCode());
                }
                if (dataForm.getDisabled() != null) {
                    existingAssignment.setDisabled(dataForm.getDisabled());
                }

                return existingAssignment;
            })
            .map(dataFormRepository::save);
    }
}
