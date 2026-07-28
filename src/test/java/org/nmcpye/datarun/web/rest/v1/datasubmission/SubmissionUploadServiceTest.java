package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private SubmissionUploadService service;
    private TemplateVersionContext templateContext;
    private DataTemplateInstanceDto template;
    private CurrentUserDetails user;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        submissionService = mock(DataSubmissionService.class);
        mapper = mock(DataSubmissionUploadV1Mapper.class);
        assignmentRepository = mock(AssignmentRepository.class);
        formAccessService = mock(AssignmentFormAccessService.class);
        templateVersionResolver = mock(TemplateVersionResolver.class);
        resolver = mock(ReferenceSubmissionResolver.class);
        service = new SubmissionUploadService(
            submissionService,
            mapper,
            new ObjectMapper(),
            assignmentRepository,
            formAccessService,
            templateVersionResolver,
            resolver);

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
    }

    @Test
    void transactionStartsAtPublicCoordinator() throws Exception {
        assertNotNull(
            SubmissionUploadService.class
                .getMethod("upsertAll", List.class)
                .getAnnotation(Transactional.class));
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

        var ordered = inOrder(resolver, submissionService);
        ordered.verify(resolver, times(2)).resolve(
            any(),
            eq(assignment),
            eq(template),
            any());
        ordered.verify(submissionService).upsertAll(
            eq(List.of(first, second)),
            any(EntitySaveSummaryVM.class));
        verify(assignmentRepository, times(2)).findByUid("assignment1");
        verify(templateVersionResolver, times(2)).resolveByUid(
            "formUid0001",
            "version0001");
    }

    @Test
    void canonicalizesContextBeforeAuthorizationAndReferenceResolution() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(formAccessService.canSubmitData(
            user,
            assignment,
            "formUid0001"))
            .thenReturn(true);

        withCurrentUser(() -> service.upsertAll(List.of(request)));

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
    void rejectsNonDirectTeamBeforeReferenceAndPersistence() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubContext(submission);
        when(user.isSuper()).thenReturn(false);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("otherTeam01"));

        assertThrows(
            IllegalQueryException.class,
            () -> withCurrentUser(
                () -> service.upsertAll(List.of(request))));

        verify(formAccessService, never()).canSubmitData(any(), any(), any());
        verify(resolver, never()).resolve(any(), any(), any(), any());
        verify(submissionService, never()).upsertAll(any(), any());
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

        verify(submissionService, never()).upsertAll(any(), any());
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

    private void withCurrentUser(Runnable operation) {
        try (MockedStatic<SecurityUtils> security =
                 org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetailsOrThrow)
                .thenReturn(user);
            operation.run();
        }
    }

    private DataSubmissionUploadV1Dto request(String uid) {
        DataSubmissionUploadV1Dto request = new DataSubmissionUploadV1Dto();
        request.setUid(uid);
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
}
