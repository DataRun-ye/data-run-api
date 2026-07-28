package org.nmcpye.datarun.datatemplateprocessor;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentFormDto;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReferenceAssignmentFormGate {

    private final ReferenceTemplateCapabilityService capabilityService;

    public void filterUnsupportedForms(
        Collection<AssignmentWithAccessDto> responses,
        int referenceVersion) {
        if (referenceVersion >= 1 || responses == null || responses.isEmpty()) {
            return;
        }

        Set<String> assignedFormUids = responses.stream()
            .filter(Objects::nonNull)
            .map(AssignmentWithAccessDto::getAccessibleForms)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .map(AssignmentFormDto::getForm)
            .filter(Objects::nonNull)
            .collect(HashSet::new, Set::add, Set::addAll);

        Set<String> unsupportedFormUids =
            capabilityService.findReferenceTemplateUids(assignedFormUids);
        if (unsupportedFormUids.isEmpty()) {
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
