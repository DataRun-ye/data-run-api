package org.nmcpye.datarun.web.rest.v1.referenceentry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.datatemplateprocessor.FormAccessService;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceTemplateCapabilityService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.reference.ReferenceEntry;
import org.nmcpye.datarun.jpa.reference.ReferenceEntryRepository;
import org.nmcpye.datarun.web.query.QueryRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceEntryV1ServiceImplTest {

    private AssignmentService assignmentService;
    private ReferenceTemplateCapabilityService capabilityService;
    private FormAccessService formAccessService;
    private ReferenceEntryRepository repository;
    private ReferenceEntryV1ServiceImpl service;
    private Assignment assignment;
    private OrgUnit orgUnit;

    @BeforeEach
    void setUp() {
        assignmentService = mock(AssignmentService.class);
        capabilityService = mock(ReferenceTemplateCapabilityService.class);
        formAccessService = mock(FormAccessService.class);
        repository = mock(ReferenceEntryRepository.class);
        service = new ReferenceEntryV1ServiceImpl(
                assignmentService,
                capabilityService,
                formAccessService,
                repository);

        orgUnit = new OrgUnit();
        orgUnit.setId("org-unit-id");
        orgUnit.setUid("orgunit0001");

        assignment = new Assignment();
        assignment.setUid("assignment1");
        assignment.setOrgUnit(orgUnit);
        assignment.setForms(Set.of("reference01"));
    }

    @Test
    void returnsOnlyTheAssignmentOrgUnitWithStableBoundedPaging() {
        when(assignmentService.findAccessibleByIdOrUid("assignment1"))
                .thenReturn(Optional.of(assignment));
        when(capabilityService.findReferenceTemplateUids(assignment.getForms()))
                .thenReturn(Set.of("reference01"));
        when(formAccessService.canAddSubmissions("reference01")).thenReturn(true);

        ReferenceEntry entry = new ReferenceEntry();
        entry.setUid("a1234567890");
        entry.setDisplayName("Test Name");
        entry.setOrgUnit(orgUnit);
        when(repository.findAllByOrgUnitIdOrderByUidAsc(
                org.mockito.ArgumentMatchers.eq("org-unit-id"),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        QueryRequest request = new QueryRequest().setPage(0).setSize(1000);
        var response = service.getForAssignment("assignment1", request);

        assertEquals("a1234567890", response.getItems().get(0).getUid());
        assertEquals("Test Name", response.getItems().get(0).getName());
        assertEquals("orgunit0001", response.getItems().get(0).getOrgUnit());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrgUnitIdOrderByUidAsc(
                org.mockito.ArgumentMatchers.eq("org-unit-id"),
                pageable.capture());
        assertEquals(500, pageable.getValue().getPageSize());
        assertEquals("uid: ASC", pageable.getValue().getSort().toString());
    }

    @Test
    void inaccessibleAssignmentDoesNotReadTheCatalog() {
        when(assignmentService.findAccessibleByIdOrUid("assignment1"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> service.getForAssignment("assignment1", new QueryRequest()));
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void assignmentWithoutAddPermissionDoesNotReadTheCatalog() {
        when(assignmentService.findAccessibleByIdOrUid("assignment1"))
                .thenReturn(Optional.of(assignment));
        when(capabilityService.findReferenceTemplateUids(assignment.getForms()))
                .thenReturn(Set.of("reference01"));
        when(formAccessService.canAddSubmissions("reference01")).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> service.getForAssignment("assignment1", new QueryRequest()));
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Pageable.class));
    }
}
