package org.nmcpye.datarun.jpa.datasubmission.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.datasubmission.validation.StoredSubmissionGuard;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.GoLiveRule;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleRepository;
import org.nmcpye.datarun.jpa.intake.HeldContext;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.HoldReason;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SubmissionUploadsTest {

    private static final String USER_ID = "01JUSER0000000000000000001";
    private static final RevisionContext RELEASE =
        RevisionContext.heldRelease("01JADMIN000000000000000001", USER_ID, "area went live");

    private DataSubmissionService submissionService;
    private DataSubmissionUploadV1Mapper mapper;
    private AssignmentRepository assignmentRepository;
    private AssignmentFormAccessService formAccessService;
    private StoredSubmissionGuard storedSubmissionGuard;
    private TemplateVersionResolver templateVersionResolver;
    private ReferenceSubmissionResolver resolver;
    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus;
    private GoLiveRuleRepository goLiveRules;
    private HeldSubmissionRepository heldSubmissions;
    private DataSubmissionRepository submissionRepository;
    private SubmissionLock submissionLock;
    private SubmissionUploads service;
    private TemplateVersionContext templateContext;
    private DataTemplateInstanceDto template;
    private CurrentUserDetails user;
    private Assignment assignment;
    private final Map<String, DataSubmission> entities = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        submissionService = mock(DataSubmissionService.class);
        mapper = mock(DataSubmissionUploadV1Mapper.class);
        assignmentRepository = mock(AssignmentRepository.class);
        formAccessService = mock(AssignmentFormAccessService.class);
        storedSubmissionGuard = mock(StoredSubmissionGuard.class);
        templateVersionResolver = mock(TemplateVersionResolver.class);
        resolver = mock(ReferenceSubmissionResolver.class);
        transactionManager = mock(PlatformTransactionManager.class);
        transactionStatus = mock(TransactionStatus.class);
        goLiveRules = mock(GoLiveRuleRepository.class);
        heldSubmissions = mock(HeldSubmissionRepository.class);
        when(heldSubmissions.hold(any(), any(), any(), any(), any())).thenReturn(true);
        submissionRepository = mock(DataSubmissionRepository.class);
        submissionLock = mock(SubmissionLock.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(transactionStatus);
        service = new SubmissionUploads(
            submissionService,
            mapper,
            new ObjectMapper(),
            assignmentRepository,
            formAccessService,
            templateVersionResolver,
            resolver,
            transactionManager,
            new SubmissionIntake(goLiveRules, heldSubmissions, submissionRepository),
            storedSubmissionGuard,
            submissionLock);

        templateContext = mock(TemplateVersionContext.class);
        template = mock(DataTemplateInstanceDto.class);
        when(templateContext.getElementsByPath()).thenReturn(Map.of());
        when(templateContext.getTemplate()).thenReturn(template);
        when(template.getUid()).thenReturn("formUid0001");
        when(template.getVersionUid()).thenReturn("version0001");
        when(template.getVersionNumber()).thenReturn(1);

        Team team = new Team();
        team.setUid("team0000001");
        team.setCode("TEAM-1");
        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setUid("orgUnit0001");
        orgUnit.setCode("OU-1");
        orgUnit.setName("Org unit 1");
        Activity activity = new Activity();
        activity.setUid("activity001");

        assignment = new Assignment();
        assignment.setUid("assignment1");
        assignment.setTeam(team);
        assignment.setOrgUnit(orgUnit);
        assignment.setActivity(activity);
        assignment.setForms(Set.of("formUid0001"));

        user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(true);
        when(user.getId()).thenReturn(USER_ID);

        // As the persistence layer answers: created without a stored submission, else updated.
        doAnswer(invocation -> {
            DataSubmission saved = invocation.getArgument(0);
            EntitySaveSummaryVM summary = invocation.getArgument(2);
            (invocation.getArgument(1) == null ? summary.getCreated() : summary.getUpdated()).add(saved.getUid());
            return null;
        }).when(submissionService).upsert(any(DataSubmission.class), any(), any(EntitySaveSummaryVM.class),
            any(RevisionContext.class));
    }

    @Test
    void eachSubmissionRunsInItsOwnNewTransaction() {
        DataSubmissionUploadV1Dto firstRequest = stubbedRequest("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = stubbedRequest("secondSub1");

        EntitySaveSummaryVM summary = upload(List.of(firstRequest, secondRequest));

        verify(submissionService, times(2)).upsert(any(DataSubmission.class), any(), any(EntitySaveSummaryVM.class),
            eq(RevisionContext.upload(USER_ID)));
        ArgumentCaptor<TransactionDefinition> definitions =
            ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager, times(2)).getTransaction(definitions.capture());
        definitions.getAllValues().forEach(definition -> assertEquals(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW,
            definition.getPropagationBehavior()));
        verify(transactionManager, times(2)).commit(transactionStatus);
        assertEquals(List.of("firstSub01", "secondSub1"), summary.getCreated());
        assertTrue(summary.getFailed().isEmpty());
    }

    @Test
    void practiceUploadIsCheckedAndSavedThenRolledBack() {
        DataSubmissionUploadV1Dto firstRequest = stubbedRequest("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = stubbedRequest("secondSub1");

        EntitySaveSummaryVM summary =
            upload(List.of(firstRequest, secondRequest), true);

        verify(resolver, times(2)).resolve(any(), eq(assignment), eq(template), any());
        verify(submissionService, times(2)).upsert(
            any(DataSubmission.class), any(), any(EntitySaveSummaryVM.class), any(RevisionContext.class));
        verify(transactionStatus, times(2)).setRollbackOnly();
        verify(heldSubmissions).hold(eq("firstSub01"), eq(HoldReason.PRACTICE), eq(null), any(), any());
        verify(heldSubmissions).hold(eq("secondSub1"), eq(HoldReason.PRACTICE), eq(null), any(), any());
        assertEquals(List.of("firstSub01", "secondSub1"), summary.getCreated());
        assertTrue(summary.getFailed().isEmpty());
    }

    @Test
    void anUploadStartedBeforeItsCampaignsGoLiveIsCheckedThenHeld() {
        GoLiveRule rule = goLiveIn(Duration.ofDays(1));
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");

        EntitySaveSummaryVM summary = upload(List.of(request));

        verify(submissionService).upsert(submissionWithUid("firstSub01"), any(), any(EntitySaveSummaryVM.class),
            any(RevisionContext.class));
        verify(transactionStatus).setRollbackOnly();
        ArgumentCaptor<HeldContext> context = ArgumentCaptor.forClass(HeldContext.class);
        verify(heldSubmissions).hold(eq("firstSub01"), eq(HoldReason.BEFORE_GO_LIVE), eq(rule), context.capture(), any());
        assertEquals("activity001", context.getValue().activity());
        assertEquals("orgUnit0001", context.getValue().orgUnit());
        assertEquals(List.of("firstSub01"), summary.getCreated(), "the phone sees it as sent");
    }

    @Test
    void anUploadAfterItsCampaignsGoLiveIsSaved() {
        goLiveIn(Duration.ofDays(-1));

        EntitySaveSummaryVM summary = upload(List.of(stubbedRequest("firstSub01")));

        verify(transactionStatus, never()).setRollbackOnly();
        verify(heldSubmissions, never()).hold(any(), any(), any(), any(), any());
        assertEquals(List.of("firstSub01"), summary.getCreated());
    }

    /**
     * A re-send checked while its earlier copy was held, kept after an admin released that copy: the uid is real data
     * by then, so the re-send is saved as an update of it instead of being dropped while the phone is told it was
     * saved (A2 S5).
     */
    @Test
    void aReSendWhoseHeldCopyWasDecidedMeanwhileIsSavedAsAnUpdate() {
        goLiveIn(Duration.ofDays(1));
        when(heldSubmissions.hold(eq("firstSub01"), any(), any(), any(), any())).thenReturn(false);
        when(submissionRepository.existsByUid("firstSub01")).thenReturn(false, true);
        DataSubmission released = submission("firstSub01");
        when(submissionLock.take("firstSub01")).thenReturn(null, released);

        EntitySaveSummaryVM summary = upload(List.of(stubbedRequest("firstSub01")));

        verify(transactionManager, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(transactionStatus, times(1)).setRollbackOnly();
        verify(submissionService).upsert(submissionWithUid("firstSub01"), eq(null), any(EntitySaveSummaryVM.class),
            any(RevisionContext.class));
        verify(submissionService).upsert(submissionWithUid("firstSub01"), same(released),
            any(EntitySaveSummaryVM.class), any(RevisionContext.class));
        assertEquals(List.of("firstSub01"), summary.getUpdated(), "the outcome of the second save, once");
        assertTrue(summary.getCreated().isEmpty());
        assertTrue(summary.getFailed().isEmpty());
    }

    /** A practice upload is never saved: when its held copy was decided meanwhile, it is not kept either. */
    @Test
    void aPracticeReSendWhoseHeldCopyWasDecidedIsNotSaved() {
        when(heldSubmissions.hold(eq("firstSub01"), any(), any(), any(), any())).thenReturn(false);

        try (LogCapture logs = new LogCapture(SubmissionUploads.class)) {
            EntitySaveSummaryVM summary = upload(List.of(stubbedRequest("firstSub01")), true);

            verify(transactionManager, times(1)).getTransaction(any(TransactionDefinition.class));
            verify(transactionStatus).setRollbackOnly();
            assertEquals(List.of("firstSub01"), summary.getCreated(), "the outcome it was checked with");
            assertTrue(logs.lines().stream().anyMatch(line -> line.contains("firstSub01") && line.contains("not kept")),
                logs.lines()::toString);
        }
    }

    @Test
    void aHoldThatCannotBeKeptIsReportedAsFailedSoThePhoneSendsAgain() {
        goLiveIn(Duration.ofDays(1));
        doThrow(new IllegalStateException("database unavailable"))
            .when(heldSubmissions).hold(eq("firstSub01"), any(), any(), any(), any());

        EntitySaveSummaryVM summary = upload(List.of(stubbedRequest("firstSub01"), stubbedRequest("secondSub1")));

        assertEquals(SubmissionUploads.UNEXPECTED_FAILURE, summary.getFailed().get("firstSub01"));
        assertEquals(List.of("secondSub1"), summary.getCreated());
    }

    @Test
    void aReleasedUploadIsSavedEvenBeforeTheGoLive() {
        goLiveIn(Duration.ofDays(1));
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");

        SubmissionUploads.Released result = service.saveReleased(request, SubmissionActor.of(user), RELEASE, () -> true);

        verify(submissionService).upsert(submissionWithUid("firstSub01"), any(), any(EntitySaveSummaryVM.class),
            eq(RELEASE));
        verify(transactionStatus, never()).setRollbackOnly();
        verify(transactionManager).commit(transactionStatus);
        verify(heldSubmissions, never()).hold(any(), any(), any(), any(), any());
        assertEquals(new SubmissionUploads.Released(SubmissionUploads.ReleaseOutcome.RELEASED, null), result);
    }

    /** The release race (A2 S4): a decision that no longer applies takes the release's save back with it. */
    @Test
    void aReleaseWhoseDecisionNoLongerAppliesSavesNothing() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");

        SubmissionUploads.Released result = service.saveReleased(request, SubmissionActor.of(user), RELEASE, () -> false);

        verify(submissionService).upsert(submissionWithUid("firstSub01"), any(), any(EntitySaveSummaryVM.class),
            any(RevisionContext.class));
        verify(transactionStatus).setRollbackOnly();
        verify(heldSubmissions, never()).hold(any(), any(), any(), any(), any());
        assertEquals(new SubmissionUploads.Released(SubmissionUploads.ReleaseOutcome.CHANGED, null), result);
    }

    /**
     * A live upload created the uid after the release last looked: the replay's transaction found it stored, so the
     * save is rolled back and the held copy is superseded instead of overwriting real data (A2 S6).
     */
    @Test
    void aReleaseOfAUidThatBecameRealDataMeanwhileOverwritesNothing() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        DataSubmission stored = submission("firstSub01");
        when(submissionLock.take("firstSub01")).thenReturn(stored);
        java.util.concurrent.atomic.AtomicBoolean asked = new java.util.concurrent.atomic.AtomicBoolean();

        SubmissionUploads.Released result = service.saveReleased(request, SubmissionActor.of(user), RELEASE, () -> {
            asked.set(true);
            return true;
        });

        verify(transactionStatus).setRollbackOnly();
        assertEquals(new SubmissionUploads.Released(SubmissionUploads.ReleaseOutcome.SUPERSEDED, null), result);
        assertEquals(false, asked.get(), "no release decision for a superseded copy");
    }

    @Test
    void aRefusedReleaseIsNotDecided() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("otherTeam01"));
        java.util.concurrent.atomic.AtomicBoolean asked = new java.util.concurrent.atomic.AtomicBoolean();

        SubmissionUploads.Released result = service.saveReleased(request, SubmissionActor.of(user), RELEASE, () -> {
            asked.set(true);
            return true;
        });

        assertEquals("E4114", result.failure(), "the code only: a refusal's text can quote submitted values");
        assertEquals(SubmissionUploads.ReleaseOutcome.FAILED, result.outcome());
        assertEquals(false, asked.get(), "no decision is recorded for a refused release");
    }

    private GoLiveRule goLiveIn(Duration fromNow) {
        GoLiveRule rule = new GoLiveRule("goLiveRule1", "activity001", null, null,
            Instant.now().plus(fromNow), null, "admin", Instant.now());
        when(goLiveRules.findByActivity("activity001")).thenReturn(List.of(rule));
        return rule;
    }

    @Test
    void ordinaryUploadIsNeverRolledBackOnPurpose() {
        upload(List.of(stubbedRequest("firstSub01")));

        verify(transactionStatus, never()).setRollbackOnly();
    }

    @Test
    void persistsEachSubmissionInItsOwnCall() {
        DataSubmissionUploadV1Dto firstRequest = stubbedRequest("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = stubbedRequest("secondSub1");

        upload(List.of(firstRequest, secondRequest));

        var ordered = inOrder(resolver, submissionService);
        ordered.verify(resolver).resolve(any(), eq(assignment), eq(template), any());
        ordered.verify(submissionService).upsert(
            submissionWithUid("firstSub01"), any(),
            any(EntitySaveSummaryVM.class), any(RevisionContext.class));
        ordered.verify(resolver).resolve(any(), eq(assignment), eq(template), any());
        ordered.verify(submissionService).upsert(
            submissionWithUid("secondSub1"), any(),
            any(EntitySaveSummaryVM.class), any(RevisionContext.class));
    }

    /**
     * The transaction takes the submission lock as its first read, before the assignment is read; the stored-record
     * guard and the save get the locked submission. With no go-live rule to apply, nothing else reads the submission.
     */
    @Test
    void theSubmissionLockComesFirstAndTheGuardAndTheSaveUseTheLockedSubmission() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        DataSubmission stored = submission("firstSub01");
        when(submissionLock.take("firstSub01")).thenReturn(stored);
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(user, assignment, "formUid0001")).thenReturn(true);

        EntitySaveSummaryVM summary = upload(List.of(request));

        var ordered = inOrder(submissionLock, assignmentRepository, storedSubmissionGuard, submissionService);
        ordered.verify(submissionLock).take("firstSub01");
        ordered.verify(assignmentRepository).findByUid("assignment1");
        ordered.verify(storedSubmissionGuard).check(submissionWithUid("firstSub01"), same(stored), eq(user));
        ordered.verify(submissionService).upsert(submissionWithUid("firstSub01"), same(stored),
            any(EntitySaveSummaryVM.class), eq(RevisionContext.upload(USER_ID)));
        verifyNoMoreInteractions(submissionLock);
        verifyNoInteractions(submissionRepository);
        assertEquals(List.of("firstSub01"), summary.getUpdated());
    }

    /**
     * The legacy route takes the submission lock first too, before its form version and its repeat ids, and checks and
     * saves the locked submission.
     */
    @Test
    void theLegacyRouteTakesTheSubmissionLockFirstAndChecksAndSavesTheLockedSubmission() {
        DataSubmission incoming = submission("firstSub01");
        DataSubmission stored = submission("firstSub01");
        when(submissionLock.take("firstSub01")).thenReturn(stored);
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(user, assignment, "formUid0001")).thenReturn(true);
        legacyLookups();

        EntitySaveSummaryVM summary = service.saveLegacy(List.of(incoming), SubmissionActor.of(user));

        var ordered = inOrder(submissionLock, templateVersionResolver, storedSubmissionGuard, submissionService);
        ordered.verify(submissionLock).take("firstSub01");
        ordered.verify(templateVersionResolver).resolveByUid("formUid0001", "version0001");
        ordered.verify(storedSubmissionGuard).check(same(incoming), same(stored), eq(user));
        ordered.verify(submissionService).upsert(same(incoming), same(stored), any(EntitySaveSummaryVM.class),
            eq(RevisionContext.legacyUpload(USER_ID)));
        verifyNoMoreInteractions(submissionLock);
        verifyNoInteractions(submissionRepository);
        assertEquals(List.of("firstSub01"), summary.getUpdated());
    }

    /**
     * A new submission has no stored one: the save creates it. An upload item without a uid goes through the
     * submission lock too, which then only bounds its lock waits.
     */
    @Test
    void aNewSubmissionIsSavedWithoutAStoredOne() {
        upload(List.of(stubbedRequest("firstSub01"), stubbedRequest(null)));

        verify(submissionLock).take("firstSub01");
        verify(submissionLock).take(null);
        verify(submissionService).upsert(submissionWithUid("firstSub01"), eq(null), any(EntitySaveSummaryVM.class),
            any(RevisionContext.class));
        verify(submissionService).upsert(argThat(submission -> submission.getUid() == null), eq(null),
            any(EntitySaveSummaryVM.class), any(RevisionContext.class));
    }

    @Test
    void canonicalizesContextBeforeAuthorizationAndReferenceResolution() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        DataSubmission submission = entity("firstSub01");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(
            user,
            assignment,
            "formUid0001"))
            .thenReturn(true);

        upload(List.of(request));

        var ordered = inOrder(formAccessService, resolver);
        ordered.verify(formAccessService).canSubmitData(
            user,
            assignment,
            "formUid0001");
        ordered.verify(resolver).resolve(
            submission,
            assignment,
            template,
            request.getReferenceDefinitions());
        assertEquals("team0000001", submission.getTeam());
        assertEquals("TEAM-1", submission.getTeamCode());
        assertEquals("orgUnit0001", submission.getOrgUnit());
        assertEquals("OU-1", submission.getOrgUnitCode());
        assertEquals("Org unit 1", submission.getOrgUnitName());
        assertEquals("activity001", submission.getActivity());
        assertEquals("formUid0001", submission.getForm());
        assertEquals("version0001", submission.getFormVersion());
        assertEquals(1, submission.getVersion());
    }

    @Test
    void reportsANonDirectTeamPerSubmissionWithoutReachingReferenceOrPersistence() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("otherTeam01"));

        EntitySaveSummaryVM summary = upload(List.of(request));

        assertTrue(summary.getFailed().get("firstSub01").startsWith("E4114: "));
        assertTrue(summary.getCreated().isEmpty());
        verify(formAccessService, never()).canSubmitData(any(), any(), any());
        verify(resolver, never()).resolve(any(), any(), any(), any());
        verify(submissionService, never()).upsert(any(), any(), any(), any());
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void aManagerPassesTheTeamScopeOnAManagedTeamsAssignment() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("supervisor1"));
        when(user.getManagedTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(
            user,
            assignment,
            "formUid0001"))
            .thenReturn(true);

        EntitySaveSummaryVM summary = upload(List.of(request));

        assertTrue(summary.getFailed().isEmpty());
        assertTrue(summary.getCreated().contains("firstSub01"));
        verify(formAccessService).canSubmitData(user, assignment, "formUid0001");
    }

    @Test
    void aFormTheTeamMayNotSubmitIsReportedAsSuch() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(
            user,
            assignment,
            "formUid0001"))
            .thenReturn(false);

        EntitySaveSummaryVM summary = upload(List.of(request));

        assertEquals(
            "E4122: You may not submit form `formUid0001` for assignment `assignment1`",
            summary.getFailed().get("firstSub01"));
        verify(submissionService, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void anUploadThatReachesAnotherTeamsRecordIsReportedAndNotSaved() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(user, assignment, "formUid0001")).thenReturn(true);
        doThrow(new IllegalQueryException(ErrorCode.E4124, "firstSub01"))
            .when(storedSubmissionGuard).check(any(), any(), any());

        EntitySaveSummaryVM summary = upload(List.of(request));

        assertTrue(summary.getFailed().get("firstSub01").startsWith("E4124: "));
        verify(submissionService, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void oneRejectedSubmissionDoesNotBlockTheOthers() {
        DataSubmissionUploadV1Dto firstRequest = stubbedRequest("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = stubbedRequest("secondSub1");
        DataSubmissionUploadV1Dto thirdRequest = stubbedRequest("thirdSub01");
        doThrow(new DomainValidationException("invalid reference"))
            .when(resolver)
            .resolve(submissionWithUid("secondSub1"), any(), any(), any());

        EntitySaveSummaryVM summary = upload(List.of(firstRequest, secondRequest, thirdRequest));

        assertEquals(List.of("firstSub01", "thirdSub01"), summary.getCreated());
        assertEquals(Set.of("secondSub1"), summary.getFailed().keySet());
        assertTrue(summary.getFailed().get("secondSub1").contains("invalid reference"));
        verify(transactionManager, times(2)).commit(transactionStatus);
        verify(transactionManager, times(1)).rollback(transactionStatus);
    }

    @Test
    void reportsAnUnexpectedFailureWithoutAbortingTheRequest() {
        DataSubmissionUploadV1Dto firstRequest = stubbedRequest("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = stubbedRequest("secondSub1");
        doThrow(new IllegalStateException(
            "could not execute statement; SQL [insert into data_submission (uid) values (?)]"))
            .when(resolver)
            .resolve(submissionWithUid("firstSub01"), any(), any(), any());

        EntitySaveSummaryVM summary = upload(List.of(firstRequest, secondRequest));

        assertEquals(SubmissionUploads.UNEXPECTED_FAILURE, summary.getFailed().get("firstSub01"),
            "the phone must not show the server's internal error text");
        assertEquals(List.of("secondSub1"), summary.getCreated());
    }

    /**
     * Production keeps these logs. A failing upload's lines name the submission, its form and the user's id, and
     * describe the failure by its code or its exception types, never by a login, a submitted value or an exception's
     * message, which can quote both (a database error quoting the row, a refusal naming the value it refused).
     */
    @Test
    void failingUploadsLogIdentifiersAndShapesOnly() {
        String login = "field-login-7";
        String value = "Household of Amina Saleh";
        when(user.getUsername()).thenReturn(login);
        when(user.getId()).thenReturn("01JUSER0000000000000000007");
        DataSubmissionUploadV1Dto broken = stubbedRequest("firstSub01");
        DataSubmissionUploadV1Dto refused = stubbedRequest("secondSub1");
        broken.setForm("formUid0001");
        refused.setForm("formUid0001");
        doThrow(new DataIntegrityViolationException(
            "could not execute statement [" + value + "] for " + login,
            new SQLException("ERROR: duplicate key; Detail: Key (name)=(" + value + ") already exists", "23505")))
            .when(resolver).resolve(submissionWithUid("firstSub01"), any(), any(), any());
        doThrow(new DomainValidationException(ErrorCode.E4120, value))
            .when(resolver).resolve(submissionWithUid("secondSub1"), any(), any(), any());

        try (LogCapture logs = new LogCapture(SubmissionUploads.class)) {
            EntitySaveSummaryVM summary = upload(List.of(broken, refused));

            assertEquals(SubmissionUploads.UNEXPECTED_FAILURE, summary.getFailed().get("firstSub01"));
            assertTrue(summary.getFailed().get("secondSub1").startsWith("E4120: "),
                "the phone still gets the refusal's text");
            logs.assertNothingLeaks(value, login);
            assertTrue(logs.lines().stream().anyMatch(line -> line.contains("firstSub01")
                && line.contains("formUid0001") && line.contains("01JUSER0000000000000000007")
                && line.contains("DataIntegrityViolationException < SQLException, SQLState 23505")), logs.lines()::toString);
            assertTrue(logs.events().stream().anyMatch(event -> event.getLevel() == Level.ERROR
                    && event.getFormattedMessage().contains("firstSub01 (form formUid0001)")),
                "a unique violation without the uid constraint is unexpected: ERROR");
            assertTrue(logs.lines().stream().anyMatch(line -> line.contains("secondSub1")
                && line.contains("refused: E4120")), logs.lines()::toString);
        }
    }

    /**
     * Two saves of one new submission at once (a phone re-sending while its first request runs, or a held release
     * meeting a live copy): the later insert hits the uid's unique constraint. That is an expected outcome, logged at
     * WARN with ids and the SQLState; the phone still gets the same answer and sends again.
     */
    @Test
    void anotherSaveOfTheSameUidComingFirstIsAWarningNotAnError() {
        when(user.getId()).thenReturn("01JUSER0000000000000000007");
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        request.setForm("formUid0001");
        doThrow(uidRace()).when(submissionService).upsert(any(DataSubmission.class), any(),
            any(EntitySaveSummaryVM.class), any(RevisionContext.class));

        try (LogCapture logs = new LogCapture(SubmissionUploads.class)) {
            EntitySaveSummaryVM summary = upload(List.of(request));

            assertEquals(SubmissionUploads.UNEXPECTED_FAILURE, summary.getFailed().get("firstSub01"),
                "the phone's answer is unchanged");
            assertEquals(List.of(Level.WARN), logs.events().stream().map(ILoggingEvent::getLevel).toList());
            assertTrue(logs.lines().get(0).contains("Submission firstSub01 (form formUid0001) from user "
                    + "01JUSER0000000000000000007 not saved: another save of the same uid came first"),
                logs.lines()::toString);
            assertTrue(logs.lines().get(0).contains("PSQLException, SQLState 23505"), logs.lines()::toString);
            logs.assertNothingLeaks("secret-household");
        }
    }

    /** A held release that meets a live copy of its uid fails as before, with its type, and is logged at WARN. */
    @Test
    void aReleaseThatMeetsALiveCopyOfItsUidIsAWarningNotAnError() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        doThrow(uidRace()).when(submissionService).upsert(any(DataSubmission.class), any(),
            any(EntitySaveSummaryVM.class), any(RevisionContext.class));

        try (LogCapture logs = new LogCapture(SubmissionUploads.class)) {
            SubmissionUploads.Released result = service.saveReleased(request, SubmissionActor.of(user), RELEASE, () -> true);

            assertEquals(new SubmissionUploads.Released(SubmissionUploads.ReleaseOutcome.FAILED,
                "DataIntegrityViolationException"), result);
            assertEquals(List.of(Level.WARN), logs.events().stream().map(ILoggingEvent::getLevel).toList());
        }
    }

    /** Any other unique violation, or one whose constraint is unknown, is not that race: it stays an error. */
    @Test
    void anotherUniqueViolationStaysAnError() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        PSQLException otherConstraint = new PSQLException(serverError("uk_reference_entry_name", "reference_entry"));
        doThrow(new DataIntegrityViolationException("could not execute statement",
            new org.hibernate.exception.ConstraintViolationException("could not execute statement", otherConstraint,
                "uk_reference_entry_name")))
            .when(submissionService).upsert(any(DataSubmission.class), any(), any(EntitySaveSummaryVM.class),
                any(RevisionContext.class));

        try (LogCapture logs = new LogCapture(SubmissionUploads.class)) {
            upload(List.of(request));

            assertEquals(List.of(Level.ERROR), logs.events().stream().map(ILoggingEvent::getLevel).toList());
        }
    }

    /**
     * Another change held the submission past the submission lock's bound: that item alone fails, as any unexpected
     * failure does (the phone sends it again), with one WARN line naming it and the SQLState, and no ERROR.
     */
    @Test
    void anItemWhoseSubmissionLockTimesOutFailsAloneWithAWarning() {
        when(user.getId()).thenReturn("01JUSER0000000000000000007");
        DataSubmissionUploadV1Dto held = stubbedRequest("firstSub01");
        held.setForm("formUid0001");
        when(submissionLock.take("firstSub01")).thenThrow(new CannotAcquireLockException(
            "could not obtain lock", new SQLException("ERROR: canceling statement due to lock timeout", "55P03")));

        try (LogCapture logs = new LogCapture(SubmissionUploads.class)) {
            EntitySaveSummaryVM summary = upload(List.of(held, stubbedRequest("secondSub1")));

            assertEquals(SubmissionUploads.UNEXPECTED_FAILURE, summary.getFailed().get("firstSub01"));
            assertEquals(List.of("secondSub1"), summary.getCreated());
            assertEquals(List.of(Level.WARN), logs.events().stream().map(ILoggingEvent::getLevel).toList());
            assertTrue(logs.lines().get(0).contains("Submission firstSub01 (form formUid0001) from user "
                + "01JUSER0000000000000000007 not saved: another change held it past the submission lock's bound"),
                logs.lines()::toString);
            assertTrue(logs.lines().get(0).contains("SQLState 55P03"), logs.lines()::toString);
            logs.assertNothingLeaks("lock timeout");
        }
    }

    /** The exception chain Spring and Hibernate build for the race, with PostgreSQL's structured error. */
    private static DataIntegrityViolationException uidRace() {
        PSQLException duplicate = new PSQLException(serverError(DataSubmission.UID_CONSTRAINT, "data_submission"));
        return new DataIntegrityViolationException("could not execute statement [secret-household]",
            new org.hibernate.exception.ConstraintViolationException("could not execute statement", duplicate,
                DataSubmission.UID_CONSTRAINT));
    }

    /** A unique violation as PostgreSQL reports it: severity, SQLState, message, detail, constraint and table. */
    private static ServerErrorMessage serverError(String constraint, String table) {
        return new ServerErrorMessage("SERROR\0C23505\0Mduplicate key value violates unique constraint \""
            + constraint + "\"\0DKey (uid)=(secret-household) already exists.\0n" + constraint + "\0t" + table + "\0");
    }

    @Test
    void aPracticeUploadIsLoggedWithoutTheLogin() {
        when(user.getUsername()).thenReturn("field-login-7");
        when(user.getId()).thenReturn("01JUSER0000000000000000007");

        try (LogCapture logs = new LogCapture(SubmissionUploads.class)) {
            upload(List.of(stubbedRequest("firstSub01")), true);

            logs.assertNothingLeaks("field-login-7");
            assertTrue(logs.lines().stream().anyMatch(line -> line.contains("01JUSER0000000000000000007")),
                logs.lines()::toString);
        }
    }

    @Test
    void legacyRouteSavesEachSubmissionSeparately() {
        DataSubmission first = submission("firstSub01");
        first.setFormVersion("unknownVer1");
        DataSubmission second = submission("secondSub1");
        legacyLookups();

        EntitySaveSummaryVM summary = service.saveLegacy(List.of(first, second), SubmissionActor.of(user));

        assertEquals(Set.of("firstSub01"), summary.getFailed().keySet());
        assertEquals(List.of("secondSub1"), summary.getCreated());
        assertEquals("team0000001", second.getTeam(), "the route fills in the context from the assignment");
        assertEquals(1, second.getVersion(), "and from the version");
        verify(submissionService).upsert(submissionWithUid("secondSub1"), any(), any(EntitySaveSummaryVM.class),
            eq(RevisionContext.legacyUpload(USER_ID)));
        verify(submissionService, never()).upsert(submissionWithUid("firstSub01"), any(),
            any(EntitySaveSummaryVM.class), any(RevisionContext.class));
        verify(resolver, never()).resolve(any(), any(), any(), any());
        verify(heldSubmissions, never()).hold(any(), any(), any(), any(), any());
    }

    @Test
    void legacyRouteChecksTheVersionFirstAndNamesTheClaimedTeam() {
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("otherTeam01"));
        DataSubmission unknownVersion = submission("firstSub01");
        unknownVersion.setFormVersion("unknownVer1");
        DataSubmission otherTeam = submission("secondSub1");
        DataSubmission unknownAssignment = submission("thirdSub01");
        unknownAssignment.setAssignment("noSuchAsgn1");
        legacyLookups();

        EntitySaveSummaryVM summary = service.saveLegacy(
            List.of(unknownVersion, otherTeam, unknownAssignment), SubmissionActor.of(user));

        assertEquals("Form version unknownVer1 of form formUid0001 is not on the server",
            summary.getFailed().get("firstSub01"));
        assertEquals("E4114: your user is not part of the team null that created the submission: secondSub1",
            summary.getFailed().get("secondSub1"));
        assertEquals("E4110: Submission validation Error: Assignment is required",
            summary.getFailed().get("thirdSub01"));
        verify(submissionService, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void legacyRouteTellsAnAdminAnUnknownAssignmentWasNotFound() {
        DataSubmission unknownAssignment = submission("firstSub01");
        unknownAssignment.setAssignment("noSuchAsgn1");
        legacyLookups();

        EntitySaveSummaryVM summary = service.saveLegacy(List.of(unknownAssignment), SubmissionActor.of(user));

        assertEquals("E4110: Submission validation Error: Assignment not found: noSuchAsgn1",
            summary.getFailed().get("firstSub01"));
    }

    private void legacyLookups() {
        when(templateVersionResolver.resolveByUid("formUid0001", "unknownVer1")).thenThrow(
            new IllegalQueryException("Form version unknownVer1 of form formUid0001 is not on the server"));
        when(templateVersionResolver.resolveByUid("formUid0001", "version0001")).thenReturn(templateContext);
        when(assignmentRepository.findByUid("assignment1")).thenReturn(Optional.of(assignment));
        when(assignmentRepository.findByUid("noSuchAsgn1")).thenReturn(Optional.empty());
    }

    private EntitySaveSummaryVM upload(List<DataSubmissionUploadV1Dto> requests) {
        return upload(requests, false);
    }

    private EntitySaveSummaryVM upload(
        List<DataSubmissionUploadV1Dto> requests,
        boolean practice) {
        return service.save(requests, SubmissionActor.of(user), practice ? IntakeMode.PRACTICE : IntakeMode.LIVE);
    }

    private DataSubmissionUploadV1Dto stubbedRequest(String uid) {
        DataSubmissionUploadV1Dto request = new DataSubmissionUploadV1Dto();
        request.setUid(uid);
        request.setReferenceDefinitions(List.of());
        DataSubmission entity = submission(uid);
        entities.put(uid, entity);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(assignmentRepository.findByUid(entity.getAssignment()))
            .thenReturn(Optional.of(assignment));
        when(templateVersionResolver.resolveByUid(
            entity.getForm(),
            entity.getFormVersion()))
            .thenReturn(templateContext);
        return request;
    }

    // DataSubmission equality is by database id, which is null for every unsaved
    // submission here, so matchers compare uids instead.
    private static DataSubmission submissionWithUid(String uid) {
        return argThat(submission -> submission != null && uid.equals(submission.getUid()));
    }

    private DataSubmission entity(String uid) {
        return entities.get(uid);
    }

    private DataSubmission submission(String uid) {
        DataSubmission submission = new DataSubmission();
        submission.setUid(uid);
        submission.setForm("formUid0001");
        submission.setFormVersion("version0001");
        submission.setAssignment("assignment1");
        submission.setFormData(new ObjectMapper().createObjectNode());
        return submission;
    }
}
