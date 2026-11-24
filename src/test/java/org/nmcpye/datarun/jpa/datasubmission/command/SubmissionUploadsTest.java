package org.nmcpye.datarun.jpa.datasubmission.command;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionUploadsTest {

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
        submissionRepository = mock(DataSubmissionRepository.class);
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
            storedSubmissionGuard);

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

        doAnswer(invocation -> {
            List<DataSubmission> saved = invocation.getArgument(0);
            EntitySaveSummaryVM summary = invocation.getArgument(1);
            saved.forEach(submission -> summary.getCreated().add(submission.getUid()));
            return saved;
        }).when(submissionService).upsertAll(anyList(), any(EntitySaveSummaryVM.class));
    }

    @Test
    void eachSubmissionRunsInItsOwnNewTransaction() {
        DataSubmissionUploadV1Dto firstRequest = stubbedRequest("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = stubbedRequest("secondSub1");

        EntitySaveSummaryVM summary = upload(List.of(firstRequest, secondRequest));

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
        verify(submissionService, times(2)).upsertAll(
            anyList(), any(EntitySaveSummaryVM.class));
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

        verify(submissionService).upsertAll(submissionsWithUids("firstSub01"), any(EntitySaveSummaryVM.class));
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

        EntitySaveSummaryVM result = service.save(List.of(request), SubmissionActor.of(user), IntakeMode.RELEASE);

        verify(transactionStatus, never()).setRollbackOnly();
        verify(heldSubmissions, never()).hold(any(), any(), any(), any(), any());
        assertEquals(List.of("firstSub01"), result.getCreated());
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
        ordered.verify(submissionService).upsertAll(
            submissionsWithUids("firstSub01"),
            any(EntitySaveSummaryVM.class));
        ordered.verify(resolver).resolve(any(), eq(assignment), eq(template), any());
        ordered.verify(submissionService).upsertAll(
            submissionsWithUids("secondSub1"),
            any(EntitySaveSummaryVM.class));
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
        verify(submissionService, never()).upsertAll(any(), any());
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
        verify(submissionService, never()).upsertAll(any(), any());
    }

    @Test
    void anUploadThatReachesAnotherTeamsRecordIsReportedAndNotSaved() {
        DataSubmissionUploadV1Dto request = stubbedRequest("firstSub01");
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(user, assignment, "formUid0001")).thenReturn(true);
        doThrow(new IllegalQueryException(ErrorCode.E4124, "firstSub01"))
            .when(storedSubmissionGuard).check(any(), any());

        EntitySaveSummaryVM summary = upload(List.of(request));

        assertTrue(summary.getFailed().get("firstSub01").startsWith("E4124: "));
        verify(submissionService, never()).upsertAll(any(), any());
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

    @Test
    void legacyRouteSavesEachSubmissionSeparately() {
        DataSubmission first = submission("firstSub01");
        DataSubmission second = submission("secondSub1");
        AtomicReference<DataSubmission> validated = new AtomicReference<>();

        EntitySaveSummaryVM summary = service.saveLegacy(
            List.of(first, second),
            SubmissionActor.of(user),
            candidate -> {
                if (candidate == first) {
                    throw new IllegalQueryException("not allowed");
                }
                validated.set(candidate);
                return candidate;
            });

        assertEquals(Set.of("firstSub01"), summary.getFailed().keySet());
        assertEquals(List.of("secondSub1"), summary.getCreated());
        assertSame(second, validated.get());
        verify(submissionService).upsertAll(submissionsWithUids("secondSub1"), any(EntitySaveSummaryVM.class));
        verify(submissionService, never()).upsertAll(submissionsWithUids("firstSub01"), any(EntitySaveSummaryVM.class));
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

    private static List<DataSubmission> submissionsWithUids(String... uids) {
        return argThat(list -> list != null
            && list.stream().map(DataSubmission::getUid).toList().equals(List.of(uids)));
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
