package org.nmcpye.datarun.web.rest.v1.referenceentry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureEventGrant;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureAuthorityUnavailableException;
import org.nmcpye.datarun.assignmentshadow.AssignmentLifecycleState;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowIdentities;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadAuthority;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadScope;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceTemplateCapabilityService;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.reference.ReferenceEntry;
import org.nmcpye.datarun.jpa.reference.ReferenceEntryRepository;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
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

    private static final String USER_UID = "Usr00000001";
    private static final String ASSIGNMENT_UID = "Asg00000001";
    private static final String EVENT_ORG_UNIT_UID = "Org00000002";
    private static final String REFERENCE_FORM_UID = "Frm00000001";

    private AssignmentService assignmentService;
    private ReferenceTemplateCapabilityService capabilityService;
    private AssignmentFormAccessService formAccessService;
    private ReferenceEntryRepository repository;
    private OrgUnitRepository orgUnitRepository;
    private ReleasedWorkReadAuthority releasedWorkAuthority;
    private ReferenceEntryV1ServiceImpl service;
    private Assignment assignment;
    private CurrentUserDetails user;
    private OrgUnit eventOrgUnit;

    @BeforeEach
    void setUp() {
        assignmentService = mock(AssignmentService.class);
        capabilityService = mock(ReferenceTemplateCapabilityService.class);
        formAccessService = mock(AssignmentFormAccessService.class);
        repository = mock(ReferenceEntryRepository.class);
        orgUnitRepository = mock(OrgUnitRepository.class);
        releasedWorkAuthority = mock(ReleasedWorkReadAuthority.class);
        service = new ReferenceEntryV1ServiceImpl(
            assignmentService,
            capabilityService,
            formAccessService,
            repository,
            orgUnitRepository,
            releasedWorkAuthority
        );

        OrgUnit assignmentOrgUnit = new OrgUnit();
        assignmentOrgUnit.setId("assignment-org-unit-id");
        assignmentOrgUnit.setUid("Org00000001");
        eventOrgUnit = new OrgUnit();
        eventOrgUnit.setId("event-org-unit-id");
        eventOrgUnit.setUid(EVENT_ORG_UNIT_UID);

        assignment = new Assignment();
        assignment.setUid(ASSIGNMENT_UID);
        assignment.setOrgUnit(assignmentOrgUnit);
        Team team = new Team();
        team.setUid("Tem00000001");
        assignment.setTeam(team);
        assignment.setForms(Set.of(REFERENCE_FORM_UID));
        user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn(USER_UID);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fieldUserCatalogUsesEventGrantFormsAndOrgUnit() {
        ReferenceEntry entry = new ReferenceEntry();
        entry.setUid("Ref00000001");
        entry.setDisplayName("Display Name");
        entry.setOrgUnit(eventOrgUnit);
        AssignmentCaptureEventGrant grant = activeGrant();
        when(assignmentService.findByIdOrUid(ASSIGNMENT_UID))
            .thenReturn(Optional.of(assignment));
        when(releasedWorkAuthority.readAssignments(
            user,
            Set.of(ASSIGNMENT_UID)
        )).thenReturn(ReleasedWorkReadScope.fieldUser(
            Map.of(ASSIGNMENT_UID, grant),
            Map.of()
        ));
        when(capabilityService.findReferenceTemplateUids(grant.formUids()))
            .thenReturn(Set.of(REFERENCE_FORM_UID));
        when(orgUnitRepository.findByUid(EVENT_ORG_UNIT_UID))
            .thenReturn(Optional.of(eventOrgUnit));
        when(repository.findAllByOrgUnitIdOrderByUidAsc(
            eq(eventOrgUnit.getId()),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(
            List.of(entry),
            PageRequest.of(2, 500),
            1001
        ));

        QueryRequest request = new QueryRequest().setPage(2).setSize(1000);
        var response = service.getForAssignment(ASSIGNMENT_UID, request);

        assertEquals(1, response.getItems().size());
        assertEquals(EVENT_ORG_UNIT_UID, response.getItems().get(0).getOrgUnitUid());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(
            Pageable.class
        );
        verify(repository).findAllByOrgUnitIdOrderByUidAsc(
            eq(eventOrgUnit.getId()),
            pageable.capture()
        );
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(500, pageable.getValue().getPageSize());
        assertEquals(
            Sort.Direction.ASC,
            pageable.getValue().getSort().getOrderFor("uid").getDirection()
        );
        verify(formAccessService, never()).canAddSubmissions(
            any(),
            any(),
            any()
        );
    }

    @Test
    void endedOrEmptyCaptureDisplayAssignmentCannotReadCatalog() {
        when(releasedWorkAuthority.readAssignments(
            user,
            Set.of(ASSIGNMENT_UID)
        )).thenReturn(ReleasedWorkReadScope.fieldUser(Map.of(), Map.of()));

        assertThrows(
            AccessDeniedException.class,
            () -> service.getForAssignment(ASSIGNMENT_UID, new QueryRequest())
        );
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
            any(),
            any()
        );
    }

    @Test
    void activeGrantWithoutReferenceCapableFormCannotReadCatalog() {
        AssignmentCaptureEventGrant grant = activeGrant();
        when(assignmentService.findByIdOrUid(ASSIGNMENT_UID))
            .thenReturn(Optional.of(assignment));
        when(releasedWorkAuthority.readAssignments(
            user,
            Set.of(ASSIGNMENT_UID)
        )).thenReturn(ReleasedWorkReadScope.fieldUser(
            Map.of(ASSIGNMENT_UID, grant),
            Map.of()
        ));
        when(capabilityService.findReferenceTemplateUids(grant.formUids()))
            .thenReturn(Set.of());

        assertThrows(
            AccessDeniedException.class,
            () -> service.getForAssignment(ASSIGNMENT_UID, new QueryRequest())
        );
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
            any(),
            any()
        );
    }

    @Test
    void absentActorReturnsEmptyBeforeReadingBaselineProjection() {
        when(releasedWorkAuthority.readAssignments(
            user,
            Set.of(ASSIGNMENT_UID)
        )).thenReturn(ReleasedWorkReadScope.actorAliasAbsentScope());

        var response = service.getForAssignment(
            ASSIGNMENT_UID,
            new QueryRequest()
        );

        assertEquals(0, response.getTotalElements());
        verify(assignmentService, never()).findByIdOrUid(any(String.class));
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
            any(),
            any()
        );
    }

    @Test
    void unavailableAuthorityFailsBeforeReadingBaselineProjection() {
        when(releasedWorkAuthority.readAssignments(
            user,
            Set.of(ASSIGNMENT_UID)
        )).thenThrow(new AssignmentCaptureAuthorityUnavailableException());

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> service.getForAssignment(
                ASSIGNMENT_UID,
                new QueryRequest()
            )
        );
        verify(assignmentService, never()).findByIdOrUid(any(String.class));
        verify(repository, never()).findAllByOrgUnitIdOrderByUidAsc(
            any(),
            any()
        );
    }

    @Test
    void customBaselineMethodDoesNotReadEventAuthority() {
        when(assignmentService.findAccessibleByIdOrUid(ASSIGNMENT_UID))
            .thenReturn(Optional.of(assignment));
        when(capabilityService.findReferenceTemplateUids(assignment.getForms()))
            .thenReturn(Set.of(REFERENCE_FORM_UID));
        when(formAccessService.canAddSubmissions(
            user,
            assignment,
            REFERENCE_FORM_UID
        )).thenReturn(true);
        when(repository.findAllByOrgUnitIdOrderByUidAsc(
            eq(assignment.getOrgUnit().getId()),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        service.getBaselineForAssignment(
            ASSIGNMENT_UID,
            new QueryRequest()
        );

        verify(releasedWorkAuthority, never()).readAssignments(any(), any());
    }

    @Test
    void administratorPreservesBaselineAccessWithoutEventRead() {
        when(user.isSuper()).thenReturn(true);
        when(assignmentService.findAccessibleByIdOrUid(ASSIGNMENT_UID))
            .thenReturn(Optional.of(assignment));
        when(capabilityService.findReferenceTemplateUids(assignment.getForms()))
            .thenReturn(Set.of(REFERENCE_FORM_UID));
        when(formAccessService.canAddSubmissions(
            user,
            assignment,
            REFERENCE_FORM_UID
        )).thenReturn(true);
        when(repository.findAllByOrgUnitIdOrderByUidAsc(
            eq(assignment.getOrgUnit().getId()),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        service.getForAssignment(ASSIGNMENT_UID, new QueryRequest());

        verify(releasedWorkAuthority, never()).readAssignments(
            any(),
            any()
        );
        verify(repository).findAllByOrgUnitIdOrderByUidAsc(
            eq(assignment.getOrgUnit().getId()),
            any(Pageable.class)
        );
    }

    private AssignmentCaptureEventGrant activeGrant() {
        return new AssignmentCaptureEventGrant(
            ASSIGNMENT_UID,
            AssignmentShadowIdentities.actorId(USER_UID),
            1,
            "Act00000001",
            AssignmentShadowIdentities.orgUnitId(EVENT_ORG_UNIT_UID),
            EVENT_ORG_UNIT_UID,
            List.of(REFERENCE_FORM_UID),
            AssignmentLifecycleState.ACTIVE
        );
    }
}
