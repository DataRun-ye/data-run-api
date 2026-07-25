package org.nmcpye.datarun.datatemplateprocessor;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReferenceAssignmentFormGate {

    private final ReferenceTemplateCapabilityService capabilityService;

    public void filterUnsupportedForms(
        Collection<Assignment> assignments,
        Collection<AssignmentWithAccessDto> responses,
        int referenceVersion) {
        if (referenceVersion >= 1 || assignments == null || assignments.isEmpty()) {
            return;
        }

        Set<String> assignedFormUids = assignments.stream()
            .filter(Objects::nonNull)
            .flatMap(assignment -> Optional.ofNullable(assignment.getForms())
                .orElse(Set.of())
                .stream())
            .collect(HashSet::new, Set::add, Set::addAll);

        Set<String> unsupportedFormUids =
            capabilityService.findReferenceTemplateUids(assignedFormUids);
        if (unsupportedFormUids.isEmpty() || responses == null) {
            return;
        }

        responses.stream()
            .filter(Objects::nonNull)
            .map(AssignmentWithAccessDto::getAccessibleForms)
            .filter(Objects::nonNull)
            .forEach(forms -> forms.removeIf(
                form -> unsupportedFormUids.contains(form.getForm())));
    }
}
