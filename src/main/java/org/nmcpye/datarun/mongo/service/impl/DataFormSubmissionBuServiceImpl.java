package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionBuRepository;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionBuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
public class DataFormSubmissionBuServiceImpl
    extends IdentifiableMongoServiceImpl<DataFormSubmissionBu>
    implements DataFormSubmissionBuService {

    private final Logger log = LoggerFactory.getLogger(DataFormSubmissionBuServiceImpl.class);

    private final DataFormSubmissionBuRepository repository;
    private final ActivityRelationalRepositoryCustom activityRepository;
    private final AssignmentRelationalRepositoryCustom assignmentRepository;
    private final TeamRelationalRepositoryCustom teamRepository;
    private static final int MAX_HISTORY_VERSIONS = 3; // Keep only the last 10 versions

    public DataFormSubmissionBuServiceImpl(
        DataFormSubmissionBuRepository repository,
        ActivityRelationalRepositoryCustom activityRepository, AssignmentRelationalRepositoryCustom assignmentRepository,
        TeamRelationalRepositoryCustom teamRepository,
        MongoTemplate mongoTemplate,
        SequenceGeneratorService sequenceGeneratorService) {
        super(repository, mongoTemplate);
        this.repository = repository;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.teamRepository = teamRepository;
    }
}
