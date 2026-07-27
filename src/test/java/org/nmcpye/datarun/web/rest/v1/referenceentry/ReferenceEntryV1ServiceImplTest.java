package org.nmcpye.datarun.web.rest.v1.referenceentry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceTemplateCapabilityService;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.reference.ReferenceEntry;
import org.nmcpye.datarun.jpa.reference.ReferenceEntryRepository;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceEntryV1ServiceImplTest {

    private AssignmentService assignmentService;
    private ReferenceTemplateCapabilityService capabilityService;
    private AssignmentFormAccessService formAccessService;
    private ReferenceEntryRepository repository;
    private ReferenceEntryV1ServiceImpl service;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignmentService = mock(AssignmentService.class);
        capabilityService = mock(ReferenceTemplateCapabilityService.class);
        formAccessService = mock(AssignmentFormAccessService.class);
        repository = mock(ReferenceEntryRepository.class);
        service = new ReferenceEntryV1ServiceImpl(
            assignmentService,
            capabilityService,
            formAccessService,
            repository);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setId("org-unit-internal-id");
        orgUnit.setUid("o1234567890");

        assignment = new Assignment();
        assignment.setUid("assignment1");
        assignment.setOrgUnit(orgUnit);
        Team team = new Team();
        team.setUid("team0000001");
        assignment.setTeam(team);
        assignment.setForms(Set.of("reference01"));
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsOnlyAssignmentOrgUnitWithBoundedStablePaging() {
        ReferenceEntry entry = new ReferenceEntry();
        entry.setUid("a1234567890");
        entry.setDisplayName("Display Name");
        entry.setOrgUnit(assignment.getOrgUnit());

        when(assignmentService.findAccessibleByIdOrUid("assignment1"))
            .thenReturn(Optional.of(assignment));
        when(capabilityService.findReferenceTemplateUids(
            assignment.getForms()))
            .thenReturn(Set.of("reference01"));
        when(formAccessService.canAddSubmissions(
            any(CurrentUserDetails.class),
            eq(assignment),
            eq("reference01")))
            .thenReturn(true);
        when(repository.findAllByOrgUnitIdOrderByUidAsc(
            eq(assignment.getOrgUnit().getId()),
            any(Pageable.class)))
            .thenReturn(new PageImpl<>(
                List.of(entry),
                PageRequest.of(2, 500),
                1001));

        QueryRequest request = new QueryRequest()
            .setPage(2)
            .setSize(1000);
        var response = service.getForAssignment("assignment1", request);

        assertEquals(1, response.getItems().size());
        assertEquals("a1234567890", response.getItems().get(0).getUid());
        assertEquals("Display Name", response.getItems().get(0).getName());
        assertEquals(
            "o1234567890",
            response.getItems().get(0).getOrgUnitUid());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(
            Pageable.class);
        verify(repository).findAllByOrgUnitIdOrderByUidAsc(
            eq(assignment.getOrgUnit().getId()),
            pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(500, pageable.getValue().getPageSize());
        assertEquals(
            Sort.Direction.ASC,
            pageable.getValue().getSort().getOrderFor("uid").getDirection());
    }

    @Test
    void inaccessibleAssignmentDoesNotReadCatalog() {
        when(assignmentService.findAccessibleByIdOrUid("assignment1"))
            .thenReturn(Optional.empty());

        assertThrows(
            AccessDeniedException.class,
            () -> service.getForAssignment(
                "assignment1",
                new QueryRequest()));
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
            any(),
            any());
    }

    @Test
    void assignmentWithoutAddableReferenceFormDoesNotReadCatalog() {
        when(assignmentService.findAccessibleByIdOrUid("assignment1"))
            .thenReturn(Optional.of(assignment));
        when(capabilityService.findReferenceTemplateUids(
            assignment.getForms()))
            .thenReturn(Set.of("reference01"));
        when(formAccessService.canAddSubmissions(
            any(CurrentUserDetails.class),
            eq(assignment),
            eq("reference01")))
            .thenReturn(false);

        assertThrows(
            AccessDeniedException.class,
            () -> service.getForAssignment(
                "assignment1",
                new QueryRequest()));
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
            any(),
            any());
    }
}
