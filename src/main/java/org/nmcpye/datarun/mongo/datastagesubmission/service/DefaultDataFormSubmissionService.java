package org.nmcpye.datarun.mongo.datastagesubmission.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.FindExistingSubmissionsDto;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DefaultDataSubmissionService;
import org.nmcpye.datarun.jpa.etl.dao.IRepeatInstancesDao;
import org.nmcpye.datarun.jpa.etl.dao.ISubmissionValuesDao;
import org.nmcpye.datarun.mongo.accessfilter.FormAccessService;
import org.nmcpye.datarun.mongo.common.DefaultMongoSoftDeleteService;
import org.nmcpye.datarun.mongo.datastagesubmission.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.migration.SubmissionMaintenanceService;
import org.nmcpye.datarun.mongo.service.impl.SequenceGeneratorService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    private final ISubmissionValuesDao submissionValuesDao;
    private final IRepeatInstancesDao repeatInstancesDao;
    private final DefaultDataSubmissionService submissionService;

    public DefaultDataFormSubmissionService(
        DataFormSubmissionRepository repository,
        CacheManager cacheManager,
        DataFormSubmissionHistoryService submissionHistoryService,
        AssignmentRepository assignmentRepository,
        SequenceGeneratorService sequenceGeneratorService,
        SubmissionMaintenanceService maintenanceService,
        DataTemplateVersionRepository versionRepository,
        FormAccessService formAccessService, ISubmissionValuesDao submissionValuesDao, IRepeatInstancesDao repeatInstancesDao, DefaultDataSubmissionService submissionService) {
        super(repository, cacheManager);
        this.repository = repository;
        this.submissionHistoryService = submissionHistoryService;
        this.assignmentRepository = assignmentRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.maintenanceService = maintenanceService;
        this.versionRepository = versionRepository;
        this.formAccessService = formAccessService;
        this.submissionValuesDao = submissionValuesDao;
        this.repeatInstancesDao = repeatInstancesDao;
        this.submissionService = submissionService;
    }

    @Override
    public DataFormSubmission update(DataFormSubmission submission) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        if (user.getUsername().startsWith("test")) {
            log.info("Pass a Test user save request `{}` save", user.getUsername());
            // pass
            return submission;
        }

        // update logic
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), submission.getId());
        DataFormSubmission updatedSubmission = super.update(submission);
        // preSaveHook(submission); // called from super update


//        submissionService.saveLegacyMongoSubmission(updatedSubmission);
        return updatedSubmission;
    }

    @Override
    public void preSaveHook(DataFormSubmission submission) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        if (user.getUsername().startsWith("test")) {
            log.info("Pass a Test user save request `{}` save", user.getUsername());
            // pass
            return;
        }

        preSave(submission);

        // archive existing submission
        repository.findByUid(submission.getUid())
            .ifPresentOrElse(existingSubmission -> {
                if(existingSubmission.getFormVersion() == null) {
                    existingSubmission.setFormVersion(submission.getFormVersion());
                }
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
    }

    @Transactional
    @Override
    public DataFormSubmission save(DataFormSubmission submission) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        if (user.getUsername().startsWith("test")) {
            log.info("Pass a Test user save request `{}` save", user.getUsername());
            // pass
            return submission;
        }

        preSaveHook(submission);
        final var savedSubmission = repository.save(submission);
//        submissionService.saveLegacyMongoSubmission(savedSubmission);
        return savedSubmission;
    }

    public Optional<DataTemplateVersion> findByVersionNoOrVersionUid(String template, String version, Integer versionNum) {
        return Optional.ofNullable(version)
            .flatMap(versionRepository::findByUid)
            .or(() -> Optional.ofNullable(versionNum)
                .flatMap(v -> versionRepository.findByTemplateUidAndVersionNumber(template, versionNum)));
    }

    public void preSave(DataFormSubmission submission) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();

        if (!formAccessService.canSubmitData(submission.getForm())) {
            log.info("User {} cannot submit data", user.getUsername());
            throw new IllegalQueryException(ErrorCode.E1112, submission.getTeam());
        }

        findByVersionNoOrVersionUid(submission.getForm(),
            submission.getFormVersion(), submission.getVersion())
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
        if (SecurityUtils.isSuper()) {
            return;
        }
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

    @Transactional
    @Override
    public void deleteByUid(String uid) {
        // check and make sure the batch process of normalize
        // and upsert will not run after this and
        // reverse the effect, (soft delete here is an update)
        super.deleteByUid(uid);
        repeatInstancesDao.markAllAsDeletedForSubmission(uid);
        submissionValuesDao.markAllAsDeletedForSubmission(uid);
    }
}
