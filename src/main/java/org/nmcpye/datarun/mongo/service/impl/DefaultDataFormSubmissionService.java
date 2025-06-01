package org.nmcpye.datarun.mongo.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.common.FindExistingSubmissionsDto;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.mongo.DefaultMongoSoftDeleteService;
import org.nmcpye.datarun.common.security.UserFormAccess;
import org.nmcpye.datarun.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionHistoryService;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.security.useraccess.dataform.FormAccessService;
import org.nmcpye.datarun.startupmigration.mongo.SubmissionMaintenanceService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
@Slf4j
public class DefaultDataFormSubmissionService
    extends DefaultMongoSoftDeleteService<DataFormSubmission>
    implements DataFormSubmissionService {

    private final DataFormSubmissionRepository repository;
    private final DataFormSubmissionHistoryService submissionHistoryService;
    private final AssignmentRepository assignmentRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SubmissionMaintenanceService maintenanceService;
    private final DataTemplateVersionRepository versionRepository;
    private final FormAccessService formAccessService;

    public DefaultDataFormSubmissionService(
        DataFormSubmissionRepository repository,
        CacheManager cacheManager,
        DataFormSubmissionHistoryService submissionHistoryService,
        AssignmentRepository assignmentRepository,
        SequenceGeneratorService sequenceGeneratorService,
        SubmissionMaintenanceService maintenanceService,
        DataTemplateVersionRepository versionRepository,
        FormAccessService formAccessService) {
        super(repository, cacheManager);
        this.repository = repository;
        this.submissionHistoryService = submissionHistoryService;
        this.assignmentRepository = assignmentRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.maintenanceService = maintenanceService;
        this.versionRepository = versionRepository;
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
        repository.findByUid(submission.getUid())
            .ifPresentOrElse(existingSubmission -> {
                submissionHistoryService.saveToHistory(existingSubmission);
                // Increment version number and update the current document
                submission.setSubmissionVersion(existingSubmission.getSubmissionVersion() + 1);
                submission.setSerialNumber(existingSubmission.getSerialNumber());

            }, () -> {
                if (submission.getSerialNumber() == null) {
                    // Generate a unique serial number for new submissions
                    long serialNumber = sequenceGeneratorService.getNextSequence("dataFormSubmissionId");
                    submission.setSerialNumber(serialNumber);
                }
            });

        final var savedSubmission = repository.save(submission);

        if (submission.getAssignment() != null) {
            addEntryToHistory(submission);
        }

        return savedSubmission;
    }

    public void preSave(DataFormSubmission submission) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();

        if (!formAccessService.canSubmitData(submission.getForm())) {
            log.info("User {} cannot submit data", user.getUsername());
            throw new IllegalQueryException(ErrorCode.E1112, submission.getTeam());
        }

        versionRepository.findByUid(submission.getFormVersion())
            .ifPresentOrElse((f) -> {
                submission.setFormVersion(f.getUid());
            }, () -> {
                log.error("form {} with version {} is not in the system",
                    submission.getForm(), submission.getVersion());
                throw new IllegalQueryException(
                    new ErrorMessage(ErrorCode.E1114, "Form", submission.getForm() + ":" + submission.getVersion()));
            });

        assignmentRepository.findByUid(submission.getAssignment())
            .ifPresentOrElse((a) -> {
                    submission.setAssignment(a.getUid());
                    submission.setOrgUnit(a.getOrgUnit().getUid());
                    submission.setOrgUnitCode(a.getOrgUnit().getCode());
                    submission.setOrgUnitName(a.getOrgUnit().getName());
                    submission.setActivity(a.getActivity().getUid());
                    submission.setTeam(a.getTeam().getUid());
                    submission.setTeamCode(a.getTeam().getCode());
                },
                () -> {
                    log.error("assignment: `{}` for form: {}, not in the system",
                        submission.getAssignment(),
                        submission.getForm());
                    throw new IllegalQueryException(
                        new ErrorMessage(ErrorCode.E1106, "Assignment", submission.getAssignment()));
                });
        submission
            .createSubmission()
            .checkAttributes();
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
        final var addForms = user.getFormAccess().stream()
            .filter(UserFormAccess::canAddSubmission)
            .map(UserFormAccess::getForm)
            .toList();

        if (viewForms.isEmpty()) {
            // No accessible forms, prevent any matches
            query.addCriteria(Criteria.where("_id").exists(false));
            return;
        }

        query.addCriteria(Criteria.where("form").in(viewForms)
            .and("team").in(user.getUserTeamsUIDs()));
    }

    private void addEntryToHistory(DataFormSubmission dataFormSubmission) {
        assignmentRepository.findByUid(dataFormSubmission.getAssignment())
            .ifPresentOrElse((assignment -> {
                assignment.setStatus(dataFormSubmission.getStatus());
                assignmentRepository.save(assignment);
            }), () -> {
                throw new IllegalQueryException(ErrorCode.E1004, getClazz().getSimpleName(),
                    dataFormSubmission.getUid());
            });
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void findAndFixFormDataSerialNumbers() {
        maintenanceService.findAndFixFormDataSerialNumbers();
    }
}
