package org.nmcpye.datarun.datatemplateprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceAssignmentScopeGuardTest {

    private ReferenceTemplateCapabilityService capabilityService;
    private ReferenceAssignmentScopeGuard guard;
    private Assignment assignment;
    private Activity activity;
    private OrgUnit orgUnit;

    @BeforeEach
    void setUp() {
        capabilityService = mock(ReferenceTemplateCapabilityService.class);
        guard = new ReferenceAssignmentScopeGuard(capabilityService);

        activity = activity("activity-id");
        orgUnit = orgUnit("org-unit-id");
        assignment = new Assignment();
        assignment.setUid("assignment1");
        assignment.setForms(Set.of("reference01"));
        assignment.setActivity(activity);
        assignment.setOrgUnit(orgUnit);
    }

    @Test
    void referenceAssignmentKeepsItsOriginalActivityAndOrgUnit() {
        when(capabilityService.findReferenceTemplateUidsAcrossVersions(assignment.getForms()))
                .thenReturn(Set.of("reference01"));

        assertDoesNotThrow(() -> guard.validateScopeUpdate(
                assignment,
                activity("activity-id"),
                orgUnit("org-unit-id"),
                assignment.getForms()));
    }

    @Test
    void referenceAssignmentRejectsScopeChanges() {
        when(capabilityService.findReferenceTemplateUidsAcrossVersions(assignment.getForms()))
                .thenReturn(Set.of("reference01"));

        assertThrows(
                IllegalQueryException.class,
                () -> guard.validateScopeUpdate(
                        assignment,
                        activity("different-activity"),
                        orgUnit,
                        assignment.getForms()));
    }

    @Test
    void referenceAssignmentRejectsRemovingItsReferenceForm() {
        when(capabilityService.findReferenceTemplateUidsAcrossVersions(assignment.getForms()))
                .thenReturn(Set.of("reference01"));

        assertThrows(
                IllegalQueryException.class,
                () -> guard.validateScopeUpdate(
                        assignment,
                        activity,
                        orgUnit,
                        Set.of()));
    }

    @Test
    void ordinaryAssignmentKeepsExistingUpdateBehavior() {
        when(capabilityService.findReferenceTemplateUidsAcrossVersions(assignment.getForms()))
                .thenReturn(Set.of());

        assertDoesNotThrow(() -> guard.validateScopeUpdate(
                assignment,
                activity("different-activity"),
                orgUnit("different-org-unit"),
                Set.of()));
    }

    private Activity activity(String id) {
        Activity value = new Activity();
        value.setId(id);
        return value;
    }

    private OrgUnit orgUnit(String id) {
        OrgUnit value = new OrgUnit();
        value.setId(id);
        return value;
    }
}
