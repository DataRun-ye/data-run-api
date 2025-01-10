package org.nmcpye.datarun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionHistoryRepository;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.mongo.service.AssignmentSubmissionHistoryService;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.mongo.service.submissionmigration.SubmissionMaintenanceService;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.exception.PathUpdateException;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    extends IdentifiableMongoServiceImpl<DataFormSubmission>
    implements DataFormSubmissionService {

    private final Logger log = LoggerFactory.getLogger(DataFormSubmissionServiceImpl.class);

    private final DataFormSubmissionRepositoryCustom repository;
    private final DataFormSubmissionHistoryRepository historyRepository;
    private final ActivityRelationalRepositoryCustom activityRepository;
    private final AssignmentRelationalRepositoryCustom assignmentRepository;
    private final TeamRelationalRepositoryCustom teamRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SubmissionMaintenanceService maintenanceService;
    private final AssignmentSubmissionHistoryService historyService;
    private static final int MAX_HISTORY_VERSIONS = 3; // Keep only the last 10 versions

    public DataFormSubmissionServiceImpl(
        DataFormSubmissionRepositoryCustom repository,
        DataFormSubmissionHistoryRepository historyRepository,
        ActivityRelationalRepositoryCustom activityRepository, AssignmentRelationalRepositoryCustom assignmentRepository,
        TeamRelationalRepositoryCustom teamRepository,
        MongoTemplate mongoTemplate,
        SequenceGeneratorService sequenceGeneratorService, SubmissionMaintenanceService maintenanceService, AssignmentSubmissionHistoryService historyService) {
        super(repository, mongoTemplate);
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.activityRepository = activityRepository;
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
                    },
                    () -> {
                        log.error("submission by: {}, for form: {}, with assignment: {} not in the system",
                            SecurityUtils.getCurrentUserLogin().get(), newSubmission.getForm(),
                            newSubmission.getAssignment());
                        throw new PropertyNotFoundException("Assignment not found: " + newSubmission.getTeam());
                    });
        }

        teamRepository.findByUid(newSubmission.getTeam())
            .ifPresentOrElse((a) -> newSubmission.setTeam(a.getUid()),
                () -> {
                    log.error("submission by: {}, for form: {}, with team: {} not in the system",
                        SecurityUtils.getCurrentUserLogin().get(), newSubmission.getForm(), newSubmission.getTeam());
                    throw new PropertyNotFoundException("Team not found: " + newSubmission.getTeam());
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
            throw new PathUpdateException("Failed to update paths", e);
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
        Query query = new Query();
        query.with(pageable);
        List<DataFormSubmission> submissions = getFormSubmissions(query, queryRequest);
        long total = submissions.size();

        return new PageImpl<>(submissions, pageable, total);
    }

    @Override
    public void deleteByUid(String uid) {
        log.debug("request to soft delete DataFormSubmission : {}", uid);
        DataFormSubmission submission = repository.findByUid(uid).orElseThrow(() -> new IllegalArgumentException("Invalid id: " + uid));

        submission.setDeleted(true);
        repository.save(submission);
//        saveVersioning(submission);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void findAndFixFormDataSerialNumbers() {
        maintenanceService.findAndFixFormDataSerialNumbers();
    }
}
