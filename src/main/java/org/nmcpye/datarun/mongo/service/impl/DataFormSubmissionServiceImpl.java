package org.nmcpye.datarun.mongo.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.FindExistingSubmissionsDto;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.common.security.UserFormAccess;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepository;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.mapper.DataFormSubmissionHistoryMapper;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.AssignmentSubmissionHistoryService;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionHistoryService;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.security.useraccess.dataform.FormAccessService;
import org.nmcpye.datarun.startupmigration.mongo.SubmissionMaintenanceService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
@Slf4j
public class DataFormSubmissionServiceImpl
    extends DefaultMongoAuditableObjectService<DataFormSubmission>
    implements DataFormSubmissionService {

    private final DataFormSubmissionRepository repository;
    private final DataFormSubmissionHistoryService submissionHistoryService;
    private final MongoTemplate mongoTemplate;
    private final AssignmentRepository assignmentRepository;
    private final TeamRepository teamRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SubmissionMaintenanceService maintenanceService;
    private final AssignmentSubmissionHistoryService historyService;
    private final DataFormTemplateRepository formTemplateRepository;
    private final FormAccessService formAccessService;

    public DataFormSubmissionServiceImpl(
        DataFormSubmissionRepository repository,
        CacheManager cacheManager,
        DataFormSubmissionHistoryService submissionHistoryService, MongoTemplate mongoTemplate,
        AssignmentRepository assignmentRepository,
        TeamRepository teamRepository,
        SequenceGeneratorService sequenceGeneratorService,
        SubmissionMaintenanceService maintenanceService,
        AssignmentSubmissionHistoryService historyService,
        DataFormTemplateRepository formTemplateRepository,
        FormAccessService formAccessService, DataFormSubmissionHistoryMapper historyMapper) {
        super(repository, cacheManager);
        this.repository = repository;
        this.submissionHistoryService = submissionHistoryService;
        this.mongoTemplate = mongoTemplate;
        this.assignmentRepository = assignmentRepository;
        this.teamRepository = teamRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.maintenanceService = maintenanceService;
        this.historyService = historyService;
        this.formTemplateRepository = formTemplateRepository;
        this.formAccessService = formAccessService;
    }

    @Transactional
    @Override
    public DataFormSubmission saveWithRelations(DataFormSubmission submission) {

        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        if (user.getUsername().startsWith("test")) {
            log.info("Pass a Test user save request `{}` save", user.getUsername());
            // pass
            return submission;
        }

        preSave(submission);

        // archive existing submission
        repository.findById(submission.getId())
            .ifPresent(existingSubmission ->
                submissionHistoryService.saveToHistory(submission));

        // Increment version number and update the current document
        submission.setVersion(submission.getVersion() + 1);

        if (submission.getAssignment() != null) {
            addEntryToHistory(submission);
        }

        return repository.save(submission);
    }

    public void preSave(DataFormSubmission newSubmission) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();

        if (!formAccessService.canSubmitData(newSubmission.getForm())) {
            log.info("User {} cannot submit data", user.getUsername());
            throw new IllegalQueryException(ErrorCode.E1112, newSubmission.getTeam());
        }

        formTemplateRepository.findByUid(newSubmission.getForm()).orElseThrow(() -> {
            log.error("form {} is not in the system for submission by {}  is not in the system",
                newSubmission.getForm(), user.getUsername());
            return new IllegalQueryException(
                new ErrorMessage(ErrorCode.E1106, "Team", newSubmission.getTeam()));
        });

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
    }

    private void addEntryToHistory(DataFormSubmission dataFormSubmission) {
        try {
            historyService.updateSubmissionHistory(dataFormSubmission);
        } catch (Exception e) {
            log.error("Error saving submission history,  {}:", dataFormSubmission.getUid(), e);
            throw new IllegalQueryException("Failed to update paths");
        }
    }

    @Override
    public FindExistingSubmissionsDto findExistingAndMissingOrgUnitCodes(List<String> codes, String form) {
        // 1. Query for distinct orgUnitCode values in the DB that are in our list
        Query query = new Query(Criteria.where("orgUnitCode").in(codes).and("form").is(form));
        List<String> existing = mongoTemplate
            .findDistinct(query, "orgUnitCode", DataFormSubmission.class, String.class);  // :contentReference[oaicite:0]{index=0}

        // 2. Compute missing codes
        List<String> missing = codes.stream()
            .filter(c -> !existing.contains(c))
            .collect(Collectors.toList());

        return FindExistingSubmissionsDto.builder()
            .existing(existing).missing(missing).build();
    }

    @Override
    public void applySecurityConstraints(Query query) {
        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
        final var viewForms = user.getFormAccess().stream()
            .filter(UserFormAccess::canViewSubmission)
            .map(UserFormAccess::getForm)
            .toList();

        if (viewForms.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(
                Criteria.where("form").in(Collections.emptyList()) // always false
            ));
            return;
        }

        query.addCriteria(new Criteria().andOperator(
            Criteria.where("form").in(viewForms),
            Criteria.where("team").in(user.getUserTeamsUIDs())
        ));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void findAndFixFormDataSerialNumbers() {
        maintenanceService.findAndFixFormDataSerialNumbers();
    }
}
