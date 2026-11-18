package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.nmcpye.datarun.web.rest.v1.datasubmission.service.SubmissionUploadService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

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

class SubmissionUploadServiceTest {

    private DataSubmissionService submissionService;
    private DataSubmissionUploadV1Mapper mapper;
    private AssignmentRepository assignmentRepository;
    private AssignmentFormAccessService formAccessService;
    private TemplateVersionResolver templateVersionResolver;
    private ReferenceSubmissionResolver resolver;
    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus;
    private SubmissionUploadService service;
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
        templateVersionResolver = mock(TemplateVersionResolver.class);
        resolver = mock(ReferenceSubmissionResolver.class);
        transactionManager = mock(PlatformTransactionManager.class);
        transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(transactionStatus);
        service = new SubmissionUploadService(
            submissionService,
            mapper,
            new ObjectMapper(),
            assignmentRepository,
            formAccessService,
            templateVersionResolver,
            resolver,
            transactionManager);

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
        assertEquals(List.of("firstSub01", "secondSub1"), summary.getCreated());
        assertTrue(summary.getFailed().isEmpty());
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
        doThrow(new IllegalStateException("database unavailable"))
            .when(resolver)
            .resolve(submissionWithUid("firstSub01"), any(), any(), any());

        EntitySaveSummaryVM summary = upload(List.of(firstRequest, secondRequest));

        assertEquals("database unavailable", summary.getFailed().get("firstSub01"));
        assertEquals(List.of("secondSub1"), summary.getCreated());
    }

    @Test
    void legacyRouteSavesEachSubmissionSeparately() {
        DataSubmission first = submission("firstSub01");
        DataSubmission second = submission("secondSub1");
        AtomicReference<DataSubmission> validated = new AtomicReference<>();

        EntitySaveSummaryVM summary = service.upsertAllLegacy(
            List.of(first, second),
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
        AtomicReference<EntitySaveSummaryVM> result = new AtomicReference<>();
        try (MockedStatic<SecurityUtils> security =
                 org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetailsOrThrow)
                .thenReturn(user);
            result.set(service.upsertAll(requests, practice));
        }
        return result.get();
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
