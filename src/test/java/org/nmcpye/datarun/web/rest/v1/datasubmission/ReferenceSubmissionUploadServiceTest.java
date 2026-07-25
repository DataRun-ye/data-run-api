package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datasubmission.validation.SubmissionAccessValidator;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.nmcpye.datarun.web.rest.v1.datasubmission.service.ReferenceSubmissionUploadService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.Map;

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

class ReferenceSubmissionUploadServiceTest {

    private DataSubmissionService submissionService;
    private DataSubmissionUploadV1Mapper mapper;
    private CompositeSubmissionValidator compositeValidator;
    private SubmissionAccessValidator accessValidator;
    private TemplateElementService templateElementService;
    private ReferenceSubmissionResolver resolver;
    private ReferenceSubmissionUploadService service;
    private TemplateElementMap templateMap;
    private DataTemplateInstanceDto template;
    private CurrentUserDetails user;

    @BeforeEach
    void setUp() {
        submissionService = mock(DataSubmissionService.class);
        mapper = mock(DataSubmissionUploadV1Mapper.class);
        compositeValidator = mock(CompositeSubmissionValidator.class);
        accessValidator = mock(SubmissionAccessValidator.class);
        templateElementService = mock(TemplateElementService.class);
        resolver = mock(ReferenceSubmissionResolver.class);
        service = new ReferenceSubmissionUploadService(
            submissionService,
            mapper,
            new ObjectMapper(),
            compositeValidator,
            accessValidator,
            templateElementService,
            resolver);
        templateMap = mock(TemplateElementMap.class);
        template = mock(DataTemplateInstanceDto.class);
        when(templateMap.getElementByIdPathMap()).thenReturn(Map.of());
        when(templateMap.getTemplateInstanceDto()).thenReturn(template);
        user = mock(CurrentUserDetails.class);
    }

    @Test
    void transactionStartsAtPublicCoordinator() throws Exception {
        assertNotNull(
            ReferenceSubmissionUploadService.class
                .getMethod("upsertAll", List.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    void resolvesEverySubmissionBeforeCallingPersistence() {
        DataSubmissionUploadV1Dto firstRequest = request("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = request("secondSub1");
        DataSubmission first = submission(firstRequest);
        DataSubmission second = submission(secondRequest);
        when(mapper.toEntity(firstRequest)).thenReturn(first);
        when(mapper.toEntity(secondRequest)).thenReturn(second);
        stubValidation(first);
        stubValidation(second);

        try (MockedStatic<SecurityUtils> security =
                 org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetailsOrThrow)
                .thenReturn(user);
            service.upsertAll(List.of(firstRequest, secondRequest));
        }

        var ordered = inOrder(resolver, submissionService);
        ordered.verify(resolver, times(2)).resolve(
            any(),
            any(),
            any());
        ordered.verify(submissionService).upsertAll(
            eq(List.of(first, second)),
            eq(user),
            any(EntitySaveSummaryVM.class));

        org.mockito.ArgumentCaptor<DataSubmission> submissions =
            org.mockito.ArgumentCaptor.forClass(DataSubmission.class);
        verify(resolver, times(2)).resolve(
            submissions.capture(),
            any(),
            any());
        assertSame(first, submissions.getAllValues().get(0));
        assertSame(second, submissions.getAllValues().get(1));
    }

    @Test
    void resolutionFailureDoesNotReachSubmissionPersistence() {
        DataSubmissionUploadV1Dto firstRequest = request("firstSub01");
        DataSubmissionUploadV1Dto secondRequest = request("secondSub1");
        DataSubmission first = submission(firstRequest);
        DataSubmission second = submission(secondRequest);
        when(mapper.toEntity(firstRequest)).thenReturn(first);
        when(mapper.toEntity(secondRequest)).thenReturn(second);
        stubValidation(first);
        stubValidation(second);
        doThrow(new DomainValidationException("invalid reference"))
            .when(resolver)
            .resolve(
                second,
                template,
                secondRequest.getReferenceDefinitions());

        try (MockedStatic<SecurityUtils> security =
                 org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetailsOrThrow)
                .thenReturn(user);
            assertThrows(
                DomainValidationException.class,
                () -> service.upsertAll(
                    List.of(firstRequest, secondRequest)));
        }

        verify(submissionService, never()).upsertAll(
            any(),
            any(),
            any());
    }

    @Test
    void resolverFailureRollsBackTheOuterTransaction() {
        DataSubmissionUploadV1Dto request = request("firstSub01");
        DataSubmission submission = submission(request);
        when(mapper.toEntity(request)).thenReturn(submission);
        stubValidation(submission);
        doThrow(new DomainValidationException("invalid reference"))
            .when(resolver)
            .resolve(
                submission,
                template,
                request.getReferenceDefinitions());

        PlatformTransactionManager transactionManager =
            mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus =
            mock(TransactionStatus.class);
        when(transactionManager.getTransaction(
            any(TransactionDefinition.class)))
            .thenReturn(transactionStatus);
        TransactionInterceptor interceptor = new TransactionInterceptor(
            transactionManager,
            new AnnotationTransactionAttributeSource());
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(interceptor);
        ReferenceSubmissionUploadService transactionalService =
            (ReferenceSubmissionUploadService) proxyFactory.getProxy();

        try (MockedStatic<SecurityUtils> security =
                 org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetailsOrThrow)
                .thenReturn(user);
            assertThrows(
                DomainValidationException.class,
                () -> transactionalService.upsertAll(List.of(request)));
        }

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(transactionStatus);
    }

    private void stubValidation(DataSubmission submission) {
        when(templateElementService.getTemplateElementMap(
            submission.getForm(),
            submission.getFormVersion()))
            .thenReturn(templateMap);
        when(accessValidator.validateAccess(submission, user))
            .thenReturn(submission);
        when(compositeValidator.validateAndEnrich(submission))
            .thenReturn(submission);
    }

    private DataSubmissionUploadV1Dto request(String uid) {
        DataSubmissionUploadV1Dto request =
            new DataSubmissionUploadV1Dto();
        request.setUid(uid);
        request.setReferenceDefinitions(List.of());
        return request;
    }

    private DataSubmission submission(
        DataSubmissionUploadV1Dto request) {
        DataSubmission submission = new DataSubmission();
        submission.setUid(request.getUid());
        submission.setForm("formUid0001");
        submission.setFormVersion("version0001");
        submission.setFormData(new ObjectMapper().createObjectNode());
        return submission;
    }
}
