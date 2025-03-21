package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.common.TeamSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepository;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionHistoryRepository;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.mongo.service.AssignmentSubmissionHistoryService;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.mongo.service.submissionmigration.SubmissionMaintenanceService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
public class DataFormSubmissionServiceImpl
    extends DefaultMongoAuditableObjectService<DataFormSubmission>
    implements DataFormSubmissionService {

    private final Logger log = LoggerFactory.getLogger(DataFormSubmissionServiceImpl.class);

    private final DataFormSubmissionRepositoryCustom repository;
    private final DataFormSubmissionHistoryRepository historyRepository;
    private final AssignmentRepository assignmentRepository;
    private final TeamRepository teamRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SubmissionMaintenanceService maintenanceService;
    private final AssignmentSubmissionHistoryService historyService;
    private static final int MAX_HISTORY_VERSIONS = 3; // Keep only the last 10 versions

    public DataFormSubmissionServiceImpl(
        DataFormSubmissionRepositoryCustom repository,
        CacheManager cacheManager,
        DataFormSubmissionHistoryRepository historyRepository,
        AssignmentRepository assignmentRepository,
        TeamRepository teamRepository,
        MongoTemplate mongoTemplate,
        SequenceGeneratorService sequenceGeneratorService, SubmissionMaintenanceService maintenanceService, AssignmentSubmissionHistoryService historyService) {
        super(repository, cacheManager, mongoTemplate);
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.assignmentRepository = assignmentRepository;
        this.teamRepository = teamRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.maintenanceService = maintenanceService;
        this.historyService = historyService;
    }

    @Override
    public DataFormSubmission saveVersioning(DataFormSubmission submission) {
        DataFormSubmission existingSubmission = repository.findById(submission.getId()).orElse(null);
        if (existingSubmission != null) {
            // Create a new version of the current data
            DataFormSubmissionHistory history = new DataFormSubmissionHistory(
                existingSubmission,
                Instant.now()
            );

            // Save the old version in the history collection
            historyRepository.save(history);

            // Increment version number and update the current document
            submission.setVersion(existingSubmission.getVersion() + 1);

            // Prune old versions
            pruneOldVersions(existingSubmission.getId());
        }

        return repository.save(submission);
    }

    @Override
    public DataFormSubmission saveWithRelations(DataFormSubmission newSubmission) {
        if (newSubmission.getAssignment() != null) {
            assignmentRepository.findByUid(newSubmission.getAssignment())
                .ifPresentOrElse((a) -> {
                        newSubmission.setAssignment(a.getUid());
                        newSubmission.setOrgUnit(a.getOrgUnit().getUid());
                        newSubmission.setOrgUnitCode(a.getOrgUnit().getCode());
                        newSubmission.setOrgUnitName(a.getOrgUnit().getName());
                        newSubmission.setActivity(a.getActivity().getUid());
                    },
                    () -> {
                        log.error("submission by: for form: {}, with assignment: {} not in the system",
                            newSubmission.getForm(),
                            newSubmission.getAssignment());
                        throw new IllegalQueryException(
                            new ErrorMessage(ErrorCode.E1106, "Assignment", newSubmission.getAssignment()));
                    });
        }

        teamRepository.findByUid(newSubmission.getTeam())
            .ifPresentOrElse((a) -> {
                    newSubmission.setTeam(a.getUid());
                    newSubmission.setTeamCode(a.getCode());
                },
                () -> {
                    log.error("submission by for form: {}, with team: {} not in the system",
                        newSubmission.getForm(), newSubmission.getTeam());
                    throw new IllegalQueryException(
                        new ErrorMessage(ErrorCode.E1106, "Team", newSubmission.getTeam()));
                });

        if (newSubmission.getSerialNumber() == null) {
            // Generate a unique serial number for new submissions
            long serialNumber = sequenceGeneratorService.getNextSequence("dataFormSubmissionId");
            newSubmission.setSerialNumber(serialNumber);
        }

        final DataFormSubmission dataFormSubmission = newSubmission
            .createSubmission()
            .populateFormDataAttributes();

        DataFormSubmission savedSubmission = repository.save(dataFormSubmission);

        if (savedSubmission.getAssignment() != null) {
            addEntryToHistory(savedSubmission);
        }

        return savedSubmission;
    }

    private void addEntryToHistory(DataFormSubmission dataFormSubmission) {
        try {
            historyService.updateSubmissionHistory(dataFormSubmission);
        } catch (Exception e) {
            log.error("Error saving submission history,  {}:", dataFormSubmission.getUid(), e);
            throw new IllegalQueryException("Failed to update paths");
        }
    }

    private void pruneOldVersions(String submissionId) {
        Query query = new Query(Criteria.where("submissionId").is(submissionId));
        query.with(Sort.by(Sort.Direction.DESC, "timestamp")); // Sort by timestamp in descending order

        List<DataFormSubmissionHistory> historyList = mongoTemplate.find(query, DataFormSubmissionHistory.class);
        if (historyList.size() > MAX_HISTORY_VERSIONS) {
            List<String> idsToRemove = historyList.stream()
                .skip(MAX_HISTORY_VERSIONS)
                .map(DataFormSubmissionHistory::getId)
                .collect(Collectors.toList());

            Query removeQuery = new Query(Criteria.where("_id").in(idsToRemove));
            mongoTemplate.remove(removeQuery, DataFormSubmissionHistory.class);
        }
    }

    @Override
    public Page<DataFormSubmission> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        Specification<Team> spec = TeamSpecifications.canRead();
        if (!queryRequest.isIncludeDisabled()) {
            spec = spec.and(TeamSpecifications.isEnabled());
        }
        Query query = new Query();
        if (!SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            final var userTeams = teamRepository.fetchBagRelationships(teamRepository.findAll(spec)).stream().map(Team::getUid).toList();
            final var username = SecurityUtils.getCurrentUserLogin();
            query = Query.query(Criteria.where("team").in(userTeams));

            if (username.isPresent()) {
                query = Query.query(Criteria.where("team").in(userTeams).orOperator(Criteria.where("formData._username").is(username.get())));
            }
        }

        query.with(pageable);
        List<DataFormSubmission> submissions = mongoTemplate.find(query, getClazz());
        long total = submissions.size();

        return new PageImpl<>(submissions, pageable, total);
    }

    @Override
    public void deleteByUid(String uid) {
        log.debug("request to soft delete DataFormSubmission : {}", uid);
        DataFormSubmission submission = repository.findByUid(uid)
            .orElseThrow(() ->
                new IllegalQueryException(new ErrorMessage(ErrorCode.E1106, "Id", uid)));

        submission.setDeleted(true);
        repository.save(submission);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void findAndFixFormDataSerialNumbers() {
        maintenanceService.findAndFixFormDataSerialNumbers();
    }
}
