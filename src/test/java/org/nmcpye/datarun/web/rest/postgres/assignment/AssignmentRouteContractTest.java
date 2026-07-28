package org.nmcpye.datarun.web.rest.postgres.assignment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureAuthorityUnavailableException;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.orgunit.service.OrgUnitService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.authorization.ResourceApiAuthorization;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.postgres.orgunit.OrgUnitResource;
import org.nmcpye.datarun.web.rest.postgres.orgunit.OrgUnitV1Resource;
import org.nmcpye.datarun.web.rest.v1.referenceentry.ReferenceEntryCustomResource;
import org.nmcpye.datarun.web.rest.v1.referenceentry.ReferenceEntryResource;
import org.nmcpye.datarun.web.rest.v1.referenceentry.ReferenceEntryV1Service;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssignmentRouteContractTest {

    private CurrentUserDetails user;

    @BeforeEach
    void authenticateTeamlessUser() {
        user = mock(CurrentUserDetails.class);
        when(user.getUsername()).thenReturn("field-user");
        when(user.getUserTeamsUIDs()).thenReturn(Set.of());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignmentCustomAndV1RootReadsResolveToDifferentOwners()
        throws Exception {
        AssignmentService service = mock(AssignmentService.class);
        when(service.findAllByUser(any(), isNull())).thenReturn(Page.empty());
        when(service.findAllReleasedWork(any(), isNull()))
            .thenReturn(Page.empty());
        when(service.getAllUserAccessibleDto(any(), isNull(), anyInt()))
            .thenReturn(Page.empty());
        when(service.getAllReleasedWorkDto(any(), isNull(), anyInt()))
            .thenReturn(Page.empty());
        AssignmentResource custom = new AssignmentResource(
            service,
            mock(AssignmentRepository.class)
        );
        AssignmentV1Resource v1 = new AssignmentV1Resource(
            service,
            mock(AssignmentRepository.class)
        );
        ResourceApiAuthorization legacyGate =
            mock(ResourceApiAuthorization.class);
        when(legacyGate.canRead(user)).thenReturn(true);
        when(legacyGate.canRead(isNull())).thenReturn(true);
        injectLegacyGate(custom, legacyGate);
        injectLegacyGate(v1, legacyGate);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(custom, v1)
            .setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver()
            )
            .build();

        mvc.perform(get("/api/custom/assignments"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/assignments"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/custom/assignments/query"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/assignments/query"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/custom/assignments/forms"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/assignments/forms"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/assignments/forms"))
            .andExpect(status().isOk());

        verify(service, times(3)).findAllByUser(any(), isNull());
        verify(service).findAllReleasedWork(any(), isNull());
        verify(service, times(2)).getAllUserAccessibleDto(
            any(),
            isNull(),
            anyInt()
        );
        verify(service).getAllReleasedWorkDto(any(), isNull(), anyInt());
    }

    @Test
    void teamlessV1AssignmentReadsBypassDenyingLegacyGate()
        throws Exception {
        AssignmentService service = mock(AssignmentService.class);
        when(service.findAllReleasedWork(any(), isNull()))
            .thenReturn(Page.empty());
        when(service.getAllReleasedWorkDto(any(), isNull(), anyInt()))
            .thenReturn(Page.empty());
        AssignmentV1Resource v1 = new AssignmentV1Resource(
            service,
            mock(AssignmentRepository.class)
        );
        ResourceApiAuthorization legacyGate =
            mock(ResourceApiAuthorization.class);
        when(legacyGate.canRead(any())).thenReturn(false);
        injectLegacyGate(v1, legacyGate);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(v1).build();

        mvc.perform(get("/api/v1/assignments"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/assignments/forms"))
            .andExpect(status().isOk());

        verify(service).findAllReleasedWork(any(), isNull());
        verify(service).getAllReleasedWorkDto(any(), isNull(), anyInt());
    }

    @Test
    void orgUnitCustomAndV1RootReadsResolveToDifferentOwners()
        throws Exception {
        OrgUnitService service = mock(OrgUnitService.class);
        when(service.findAllByUser(any(), isNull())).thenReturn(Page.empty());
        when(service.findAllReleasedWork(any(), isNull()))
            .thenReturn(Page.empty());
        OrgUnitResource custom = new OrgUnitResource(
            service,
            mock(OrgUnitRepository.class)
        );
        OrgUnitV1Resource v1 = new OrgUnitV1Resource(
            service,
            mock(OrgUnitRepository.class)
        );
        ResourceApiAuthorization legacyGate =
            mock(ResourceApiAuthorization.class);
        when(legacyGate.canRead(user)).thenReturn(true);
        injectLegacyGate(custom, legacyGate);
        injectLegacyGate(v1, legacyGate);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(custom, v1).build();

        mvc.perform(get("/api/custom/orgUnits"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/orgUnits"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/custom/orgUnits/query"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/orgUnits/query"))
            .andExpect(status().isOk());

        verify(service, times(3)).findAllByUser(any(), isNull());
        verify(service).findAllReleasedWork(any(), isNull());
    }

    @Test
    void teamlessV1OrgUnitReadBypassesDenyingLegacyGate()
        throws Exception {
        OrgUnitService service = mock(OrgUnitService.class);
        when(service.findAllReleasedWork(any(), isNull()))
            .thenReturn(Page.empty());
        OrgUnitV1Resource v1 = new OrgUnitV1Resource(
            service,
            mock(OrgUnitRepository.class)
        );
        ResourceApiAuthorization legacyGate =
            mock(ResourceApiAuthorization.class);
        when(legacyGate.canRead(any())).thenReturn(false);
        injectLegacyGate(v1, legacyGate);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(v1).build();

        mvc.perform(get("/api/v1/orgUnits"))
            .andExpect(status().isOk());

        verify(service).findAllReleasedWork(any(), isNull());
    }

    @Test
    void referenceCustomAndV1ReadsResolveToDifferentServiceMethods()
        throws Exception {
        ReferenceEntryV1Service service = mock(ReferenceEntryV1Service.class);
        when(service.getBaselineForAssignment(any(), any())).thenReturn(
            new PagedResponse<>(Page.empty(), "referenceEntries", null)
        );
        when(service.getForAssignment(any(), any())).thenReturn(
            new PagedResponse<>(Page.empty(), "referenceEntries", null)
        );
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new ReferenceEntryCustomResource(service),
            new ReferenceEntryResource(service)
        ).build();

        mvc.perform(get(
            "/api/custom/assignments/Asg00000001/referenceEntries"
        )).andExpect(status().isOk());
        mvc.perform(get(
            "/api/v1/assignments/Asg00000001/referenceEntries"
        )).andExpect(status().isOk());

        verify(service).getBaselineForAssignment(
            org.mockito.ArgumentMatchers.eq("Asg00000001"),
            any()
        );
        verify(service).getForAssignment(
            org.mockito.ArgumentMatchers.eq("Asg00000001"),
            any()
        );
    }

    @Test
    void unavailableAuthorityReturns503AcrossAllFourV1Reads()
        throws Exception {
        AssignmentService assignmentService = mock(AssignmentService.class);
        when(assignmentService.findAllReleasedWork(any(), isNull()))
            .thenThrow(new AssignmentCaptureAuthorityUnavailableException());
        when(assignmentService.getAllReleasedWorkDto(
            any(),
            isNull(),
            anyInt()
        )).thenThrow(new AssignmentCaptureAuthorityUnavailableException());
        AssignmentV1Resource assignments = new AssignmentV1Resource(
            assignmentService,
            mock(AssignmentRepository.class)
        );

        OrgUnitService orgUnitService = mock(OrgUnitService.class);
        when(orgUnitService.findAllReleasedWork(any(), isNull()))
            .thenThrow(new AssignmentCaptureAuthorityUnavailableException());
        OrgUnitV1Resource orgUnits = new OrgUnitV1Resource(
            orgUnitService,
            mock(OrgUnitRepository.class)
        );

        ReferenceEntryV1Service referenceService =
            mock(ReferenceEntryV1Service.class);
        when(referenceService.getForAssignment(any(), any()))
            .thenThrow(new AssignmentCaptureAuthorityUnavailableException());
        ReferenceEntryResource references =
            new ReferenceEntryResource(referenceService);

        ResourceApiAuthorization legacyGate =
            mock(ResourceApiAuthorization.class);
        injectLegacyGate(assignments, legacyGate);
        injectLegacyGate(orgUnits, legacyGate);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
            assignments,
            orgUnits,
            references
        ).build();

        mvc.perform(get("/api/v1/assignments"))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/assignments/forms"))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/orgUnits"))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(get(
            "/api/v1/assignments/Asg00000001/referenceEntries"
        )).andExpect(status().isServiceUnavailable());
    }

    private void injectLegacyGate(
        Object resource,
        ResourceApiAuthorization gate
    ) {
        ReflectionTestUtils.setField(
            resource,
            "resourceApiAuthorization",
            gate
        );
    }
}
