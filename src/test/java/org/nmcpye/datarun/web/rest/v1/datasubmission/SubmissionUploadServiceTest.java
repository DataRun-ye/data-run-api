package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureAuthorityUnavailableException;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureEventGrant;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureEventReadPort;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureEventSnapshot;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureScopeFactory;
import org.nmcpye.datarun.assignmentshadow.AssignmentLifecycleState;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowCheckpoint;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowIdentities;
import org.nmcpye.datarun.assignmentshadow.BaselineVersionedUploadCompatibilityAdapter;
import org.nmcpye.datarun.assignmentshadow.CanonicalCaptureFormResolver;
import org.nmcpye.datarun.assignmentshadow.LatestAssignmentGrantReader;
import org.nmcpye.datarun.assignmentshadow.VersionedUploadEventAuthorizer;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.captureshadow.VersionedCaptureCommand;
import org.nmcpye.datarun.captureshadow.VersionedCaptureSubmissionCommand;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
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
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionUploadServiceTest {

    private VersionedCaptureCommand captureCommand;
    private DataSubmissionUploadV1Mapper mapper;
    private AssignmentRepository assignmentRepository;
    private AssignmentFormAccessService formAccessService;
    private TemplateVersionResolver templateVersionResolver;
    private ReferenceSubmissionResolver resolver;
    private AssignmentCaptureEventReadPort eventReader;
    private SubmissionUploadService service;
    private ObjectMapper objectMapper;
    private TemplateVersionContext templateContext;
    private DataTemplateInstanceDto template;
    private CurrentUserDetails user;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        captureCommand = mock(VersionedCaptureCommand.class);
        mapper = mock(DataSubmissionUploadV1Mapper.class);
        assignmentRepository = mock(AssignmentRepository.class);
        formAccessService = mock(AssignmentFormAccessService.class);
        templateVersionResolver = mock(TemplateVersionResolver.class);
        resolver = mock(ReferenceSubmissionResolver.class);
        eventReader = mock(AssignmentCaptureEventReadPort.class);
        objectMapper = new ObjectMapper();
        CanonicalCaptureFormResolver captureForms =
            new CanonicalCaptureFormResolver(objectMapper);
        AssignmentCaptureScopeFactory scopeFactory =
            new AssignmentCaptureScopeFactory();
        BaselineVersionedUploadCompatibilityAdapter compatibility =
            new BaselineVersionedUploadCompatibilityAdapter(
                scopeFactory,
                captureForms,
                formAccessService,
                Clock.systemUTC()
            );
        VersionedUploadEventAuthorizer eventAuthorizer =
            new VersionedUploadEventAuthorizer(
                latestGrantReader(eventReader),
                scopeFactory,
                compatibility
            );
        service = new SubmissionUploadService(
            captureCommand,
            mapper,
            objectMapper,
            assignmentRepository,
            templateVersionResolver,
            resolver,
            eventAuthorizer);

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
        activity.setDisabled(false);
        team.setActivity(activity);
        team.setDisabled(false);

        assignment = new Assignment();
        assignment.setUid("assignment1");
        assignment.setTeam(team);
        assignment.setOrgUnit(orgUnit);
        assignment.setActivity(activity);
        assignment.setForms(Set.of("formUid0001"));

        user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn("user0000001");
        when(user.isSuper()).thenReturn(true);
    }

    @Test
    void transactionStartsAtPublicCoordinator() throws Exception {
        assertNotNull(
            SubmissionUploadService.class
                .getMethod("upsertAll", List.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    void duplicateUidFailsBeforeAuthorizationOrPreparation() {
        DataSubmissionUploadV1Dto first = request("firstSub01");
        DataSubmissionUploadV1Dto duplicate = request("firstSub01");

        DomainValidationException failure = assertThrows(
            DomainValidationException.class,
            () -> service.upsertAll(List.of(first, duplicate))
        );

        assertTrue(failure.getMessage().contains("firstSub01"));
        verify(mapper, never()).toEntity(any());
        verify(eventReader, never()).readAssignments(any(), anyCollection());
        verify(captureCommand, never()).execute(any(), any());
    }

    @Test
    void preparesEverySubmissionBeforeCallingPersistence() {
        DataSubmissionUploadV1Dto firstRequest = request("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = request("secondSub1");
        DataSubmission first = submission(firstRequest);
        DataSubmission second = submission(secondRequest);
        when(mapper.toEntity(firstRequest)).thenReturn(first);
        when(mapper.toEntity(secondRequest)).thenReturn(second);
        stubContext(first);
        stubContext(second);

        withCurrentUser(() -> service.upsertAll(
            List.of(firstRequest, secondRequest)));

        var ordered = inOrder(resolver, captureCommand);
        ordered.verify(resolver, times(2)).resolve(
            any(),
            eq(assignment),
            eq(template),
            any());
        ordered.verify(captureCommand).execute(
            commands(first, second),
            any(EntitySaveSummaryVM.class));
        verify(assignmentRepository, times(2)).findByUid("assignment1");
        verify(templateVersionResolver, times(2)).resolveByUid(
            "formUid0001",
            "version0001");
        verify(eventReader, never()).readAssignments(any(), anyCollection());
    }

    @Test
    void canonicalizesContextBeforeAuthorizationAndReferenceResolution() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        stubActiveEvent();

        withCurrentUser(() -> service.upsertAll(List.of(request)));

        var ordered = inOrder(eventReader, resolver);
        ordered.verify(eventReader).readAssignments(
            eq("user0000001"),
            anyCollection()
        );
        ordered.verify(resolver).resolve(
            submission,
            assignment,
            template,
            request.getReferenceDefinitions());
        verify(formAccessService, never()).canSubmitData(any(), any(), any());
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
    void rejectsNonDirectTeamBeforeReferenceAndPersistence() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("otherTeam01"));
        stubNoEventGrants();

        IllegalQueryException failure = assertThrows(
            IllegalQueryException.class,
            () -> withCurrentUser(
                () -> service.upsertAll(List.of(request))));

        assertEquals(ErrorCode.E4114, failure.getErrorCode());
        verify(formAccessService, never()).canSubmitData(any(), any(), any());
        verify(resolver, never()).resolve(any(), any(), any(), any());
        verify(captureCommand, never()).execute(any(), any());
    }

    @Test
    void earlierAuthorizationFailureWinsBeforePreparingLaterRequests() {
        DataSubmissionUploadV1Dto deniedRequest = request("firstSub01");
        DataSubmissionUploadV1Dto laterRequest = request("secondSub1");
        laterRequest.setAssignment(null);
        laterRequest.setFormVersion(null);
        DataSubmission denied = submission(deniedRequest);
        when(mapper.toEntity(deniedRequest)).thenReturn(denied);
        stubContext(denied);
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("otherTeam01"));
        stubNoEventGrants();

        IllegalQueryException failure = assertThrows(
            IllegalQueryException.class,
            () -> withCurrentUser(
                () -> service.upsertAll(List.of(deniedRequest, laterRequest))));

        assertEquals(ErrorCode.E4114, failure.getErrorCode());
        verify(mapper, never()).toEntity(laterRequest);
        verify(eventReader).readAssignments(
            eq("user0000001"),
            anyCollection()
        );
        verify(resolver, never()).resolve(any(), any(), any(), any());
        verify(captureCommand, never()).execute(any(), any());
    }

    @Test
    void fieldUserBulkReadsEventsOnceForSharedAssignment() {
        DataSubmissionUploadV1Dto firstRequest = request("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = request("secondSub1");
        DataSubmission first = submission(firstRequest);
        DataSubmission second = submission(secondRequest);
        when(mapper.toEntity(firstRequest)).thenReturn(first);
        when(mapper.toEntity(secondRequest)).thenReturn(second);
        stubContext(first);
        stubContext(second);
        when(user.isSuper()).thenReturn(false);
        stubActiveEvent();
        doAnswer(invocation -> {
            EntitySaveSummaryVM summary = invocation.getArgument(1);
            summary.getCreated().addAll(
                List.of("firstSub01", "secondSub1")
            );
            return null;
        }).when(captureCommand).execute(any(), any());

        EntitySaveSummaryVM result = withCurrentUser(() ->
            service.upsertAll(List.of(firstRequest, secondRequest))
        );

        assertEquals(
            List.of("firstSub01", "secondSub1"),
            result.getCreated()
        );
        verify(eventReader, times(1)).readAssignments(
            eq("user0000001"),
            anyCollection()
        );
        verify(captureCommand).execute(
            commands(first, second),
            any(EntitySaveSummaryVM.class)
        );
    }

    @Test
    void activeAuthorizationPreservesWholeJsonRepeatPreparation() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        var formData = objectMapper.createObjectNode();
        formData.put("answer", "preserved");
        formData.putArray("visits")
            .addObject()
            .put("value", "first");
        submission.setFormData(formData);
        FormSectionConf repeat = new FormSectionConf();
        repeat.setRepeatable(true);
        when(templateContext.getElementsByPath())
            .thenReturn(Map.of("visits", repeat));
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        when(user.isSuper()).thenReturn(false);
        stubActiveEvent();

        withCurrentUser(() -> service.upsertAll(List.of(request)));

        assertEquals(
            "preserved",
            submission.getFormData().path("answer").asText()
        );
        var repeatRow = submission.getFormData()
            .path("visits")
            .path(0);
        assertEquals("first", repeatRow.path("value").asText());
        assertTrue(repeatRow.path("_id").isTextual());
        assertEquals("firstSub01", repeatRow.path("_parentId").asText());
        assertEquals(
            "firstSub01",
            repeatRow.path("_submissionUid").asText()
        );
        verify(resolver).resolve(
            submission,
            assignment,
            template,
            request.getReferenceDefinitions()
        );
        verify(captureCommand).execute(
            commands(submission),
            any(EntitySaveSummaryVM.class)
        );
    }

    @Test
    void unavailableAuthorityStopsBeforeReferenceAndPersistence() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        when(user.isSuper()).thenReturn(false);
        when(eventReader.readAssignments(
            eq("user0000001"),
            anyCollection()
        )).thenReturn(AssignmentCaptureEventSnapshot.unavailable());

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> withCurrentUser(
                () -> service.upsertAll(List.of(request)))
        );

        verify(resolver, never()).resolve(any(), any(), any(), any());
        verify(captureCommand, never()).execute(any(), any());
    }

    @Test
    void eventReadFailureStopsBeforeReferenceAndPersistence() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        when(user.isSuper()).thenReturn(false);
        when(eventReader.readAssignments(
            eq("user0000001"),
            anyCollection()
        )).thenThrow(new IllegalStateException("event store unavailable"));

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> withCurrentUser(
                () -> service.upsertAll(List.of(request)))
        );

        verify(resolver, never()).resolve(any(), any(), any(), any());
        verify(captureCommand, never()).execute(any(), any());
    }

    @Test
    void resolutionFailureDoesNotReachSubmissionPersistence() {
        DataSubmissionUploadV1Dto firstRequest = request("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = request("secondSub1");
        DataSubmission first = submission(firstRequest);
        DataSubmission second = submission(secondRequest);
        when(mapper.toEntity(firstRequest)).thenReturn(first);
        when(mapper.toEntity(secondRequest)).thenReturn(second);
        stubContext(first);
        stubContext(second);
        doThrow(new DomainValidationException("invalid reference"))
            .when(resolver)
            .resolve(
                second,
                assignment,
                template,
                secondRequest.getReferenceDefinitions());

        assertThrows(
            DomainValidationException.class,
            () -> withCurrentUser(
                () -> service.upsertAll(
                    List.of(firstRequest, secondRequest))));

        verify(captureCommand, never()).execute(any(), any());
    }

    @Test
    void resolverFailureRollsBackTheOuterTransaction() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        doThrow(new DomainValidationException("invalid reference"))
            .when(resolver)
            .resolve(
                submission,
                assignment,
                template,
                request.getReferenceDefinitions());

        PlatformTransactionManager transactionManager =
            mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(
            any(TransactionDefinition.class)))
            .thenReturn(transactionStatus);
        TransactionInterceptor interceptor = new TransactionInterceptor(
            transactionManager,
            new AnnotationTransactionAttributeSource());
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(interceptor);
        SubmissionUploadService transactionalService =
            (SubmissionUploadService) proxyFactory.getProxy();

        assertThrows(
            DomainValidationException.class,
            () -> withCurrentUser(
                () -> transactionalService.upsertAll(List.of(request))));

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(transactionStatus);
    }

    private void stubContext(DataSubmission submission) {
        when(assignmentRepository.findByUid(submission.getAssignment()))
            .thenReturn(Optional.of(assignment));
        when(templateVersionResolver.resolveByUid(
            submission.getForm(),
            submission.getFormVersion()))
            .thenReturn(templateContext);
    }

    private LatestAssignmentGrantReader latestGrantReader(
        AssignmentCaptureEventReadPort reader
    ) {
        AssignmentShadowCheckpoint checkpoint =
            mock(AssignmentShadowCheckpoint.class);
        when(checkpoint.existsAndIsExact()).thenReturn(true);
        return new LatestAssignmentGrantReader(reader, checkpoint);
    }

    private void stubActiveEvent() {
        AssignmentCaptureEventGrant grant = new AssignmentCaptureEventGrant(
            assignment.getUid(),
            AssignmentShadowIdentities.actorId("user0000001"),
            0,
            assignment.getActivity().getUid(),
            AssignmentShadowIdentities.orgUnitId(
                assignment.getOrgUnit().getUid()
            ),
            assignment.getOrgUnit().getUid(),
            List.of("formUid0001"),
            AssignmentLifecycleState.ACTIVE
        );
        when(eventReader.readAssignments(
            eq("user0000001"),
            anyCollection()
        )).thenReturn(AssignmentCaptureEventSnapshot.available(
            AssignmentShadowIdentities.actorId("user0000001"),
            List.of(grant)
        ));
    }

    private void stubNoEventGrants() {
        when(eventReader.readAssignments(
            eq("user0000001"),
            anyCollection()
        )).thenReturn(AssignmentCaptureEventSnapshot.available(
            AssignmentShadowIdentities.actorId("user0000001"),
            List.of()
        ));
    }

    private <T> T withCurrentUser(Supplier<T> operation) {
        try (MockedStatic<SecurityUtils> security =
                 org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetailsOrThrow)
                .thenReturn(user);
            return operation.get();
        }
    }

    private DataSubmissionUploadV1Dto request(String uid) {
        DataSubmissionUploadV1Dto request = new DataSubmissionUploadV1Dto();
        request.setUid(uid);
        request.setAssignment("assignment1");
        request.setReferenceDefinitions(List.of());
        return request;
    }

    private DataSubmission submission(DataSubmissionUploadV1Dto request) {
        DataSubmission submission = new DataSubmission();
        submission.setUid(request.getUid());
        submission.setForm("formUid0001");
        submission.setFormVersion("version0001");
        submission.setAssignment("assignment1");
        submission.setFormData(new ObjectMapper().createObjectNode());
        return submission;
    }

    private List<VersionedCaptureSubmissionCommand> commands(
        DataSubmission... expected
    ) {
        List<DataSubmission> expectedSubmissions = List.of(expected);
        return argThat(actual ->
            actual != null
                && actual.stream()
                .map(VersionedCaptureSubmissionCommand::submission)
                .toList()
                .equals(expectedSubmissions)
        );
    }
}
