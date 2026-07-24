package org.nmcpye.datarun.datatemplateprocessor;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReferenceAssignmentScopeGuard {

    private final ReferenceTemplateCapabilityService referenceTemplateCapabilityService;

    public void validateScopeUpdate(
            Assignment existing,
            Activity incomingActivity,
            OrgUnit incomingOrgUnit,
            Collection<String> incomingForms) {
        if (existing == null) {
            return;
        }

        Set<String> lockedReferenceForms = referenceTemplateCapabilityService
                .findReferenceTemplateUidsAcrossVersions(existing.getForms());
        if (lockedReferenceForms.isEmpty()) {
            return;
        }

        boolean removesReferenceForm = incomingForms == null
                || !incomingForms.containsAll(lockedReferenceForms);
        if (removesReferenceForm
                || !sameActivity(existing.getActivity(), incomingActivity)
                || !sameOrgUnit(existing.getOrgUnit(), incomingOrgUnit)) {
            throw new IllegalQueryException(
                    new ErrorMessage(ErrorCode.E1123, existing.getUid()));
        }
    }

    private boolean sameActivity(Activity left, Activity right) {
        return left == null && right == null
                || left != null && right != null && Objects.equals(left.getId(), right.getId());
    }

    private boolean sameOrgUnit(OrgUnit left, OrgUnit right) {
        return left == null && right == null
                || left != null && right != null && Objects.equals(left.getId(), right.getId());
    }
}
