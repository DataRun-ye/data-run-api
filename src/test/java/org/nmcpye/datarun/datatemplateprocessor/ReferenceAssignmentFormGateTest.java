package org.nmcpye.datarun.datatemplateprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentFormDto;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReferenceAssignmentFormGateTest {

    private ReferenceTemplateCapabilityService capabilityService;
    private ReferenceAssignmentFormGate gate;

    @BeforeEach
    void setUp() {
        capabilityService = mock(ReferenceTemplateCapabilityService.class);
        gate = new ReferenceAssignmentFormGate(capabilityService);
    }

    @Test
    void oldClientKeepsOrdinaryFormAndExcludesReferenceForm() {
        Assignment assignment = new Assignment();
        assignment.setForms(Set.of("ordinary01", "reference01"));

        AssignmentWithAccessDto response = new AssignmentWithAccessDto();
        response.setAccessibleForms(new HashSet<>(Set.of(
            form("ordinary01"),
            form("reference01"))));

        when(capabilityService.findReferenceTemplateUids(
            Set.of("ordinary01", "reference01")))
            .thenReturn(Set.of("reference01"));

        gate.filterUnsupportedForms(
            List.of(assignment),
            List.of(response),
            0);

        assertEquals(
            Set.of("ordinary01"),
            response.getAccessibleForms().stream()
                .map(AssignmentFormDto::getForm)
                .collect(java.util.stream.Collectors.toSet()));
        verify(capabilityService).findReferenceTemplateUids(
            Set.of("ordinary01", "reference01"));
    }

    @Test
    void referenceVersionOneLeavesAccessibleFormsUnchanged() {
        Assignment assignment = new Assignment();
        assignment.setForms(Set.of("reference01"));

        AssignmentWithAccessDto response = new AssignmentWithAccessDto();
        response.setAccessibleForms(new HashSet<>(Set.of(
            form("reference01"))));

        gate.filterUnsupportedForms(
            List.of(assignment),
            List.of(response),
            1);

        assertEquals(1, response.getAccessibleForms().size());
        verifyNoInteractions(capabilityService);
    }

    private AssignmentFormDto form(String uid) {
        return AssignmentFormDto.builder().form(uid).build();
    }
}
