package org.nmcpye.datarun.jpa.reference;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.ReferenceDefinitionV1Dto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceSubmissionResolver {

    private static final Pattern UID =
        Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{10}$");

    private final AssignmentRepository assignmentRepository;
    private final ReferenceEntryRepository referenceEntryRepository;
    private final ReferenceValueExtractor valueExtractor;
    private final ReferenceDisplayNamePolicy displayNamePolicy;

    public void resolve(
        DataSubmission submission,
        DataTemplateInstanceDto template,
        Collection<ReferenceDefinitionV1Dto> suppliedDefinitions) {
        List<ReferenceValueOccurrence> occurrences = valueExtractor.extract(
            submission.getUid(),
            template,
            submission.getFormData());
        Map<String, String> definitions = definitionsByUid(
            suppliedDefinitions);

        validateValues(submission.getUid(), occurrences);
        Set<String> usedUids = occurrences.stream()
            .map(ReferenceValueOccurrence::uid)
            .collect(Collectors.toSet());
        definitions.keySet().stream()
            .filter(uid -> !usedUids.contains(uid))
            .findFirst()
            .ifPresent(uid -> {
                throw new DomainValidationException(
                    ErrorCode.E4121,
                    submission.getUid(),
                    uid);
            });
        if (usedUids.isEmpty()) {
            return;
        }

        Assignment assignment = assignmentRepository
            .findByUid(submission.getAssignment())
            .orElseThrow(() -> new DomainValidationException(
                "Assignment is required"));
        if (assignment.getOrgUnit() == null || assignment.getActivity() == null) {
            throw new DomainValidationException(
                "Reference assignment scope is incomplete");
        }

        Map<String, ReferenceEntry> existingByUid =
            referenceEntryRepository.findAllByUidIn(usedUids).stream()
                .collect(Collectors.toMap(
                    ReferenceEntry::getUid,
                    entry -> entry));
        List<ReferenceEntry> entriesToCreate = new ArrayList<>();
        for (String uid : usedUids) {
            ReferenceEntry existing = existingByUid.get(uid);
            if (existing != null) {
                if (!assignment.getOrgUnit().getId()
                    .equals(existing.getOrgUnit().getId())) {
                    throw new DomainValidationException(ErrorCode.E4119, uid);
                }
                continue;
            }

            String normalizedName =
                displayNamePolicy.normalizeNewName(definitions.get(uid));
            if (normalizedName == null) {
                throw new DomainValidationException(ErrorCode.E4120, uid);
            }

            ReferenceEntry created = new ReferenceEntry();
            created.setUid(uid);
            created.setDisplayName(normalizedName);
            created.setOrgUnit(assignment.getOrgUnit());
            created.setFirstRegisteredActivity(assignment.getActivity());
            entriesToCreate.add(created);
        }
        if (!entriesToCreate.isEmpty()) {
            referenceEntryRepository.persistAllAndFlush(entriesToCreate);
        }
    }

    private Map<String, String> definitionsByUid(
        Collection<ReferenceDefinitionV1Dto> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return Map.of();
        }

        Map<String, String> byUid = new LinkedHashMap<>();
        for (ReferenceDefinitionV1Dto definition : definitions) {
            if (definition == null || definition.getUid() == null) {
                throw new DomainValidationException(
                    ErrorCode.E4120,
                    definition == null ? null : definition.getUid());
            }
            if (byUid.containsKey(definition.getUid())
                && !Objects.equals(
                byUid.get(definition.getUid()),
                definition.getName())) {
                throw new DomainValidationException(
                    ErrorCode.E4120,
                    definition.getUid());
            }
            byUid.putIfAbsent(definition.getUid(), definition.getName());
        }
        return byUid;
    }

    private void validateValues(
        String submissionUid,
        List<ReferenceValueOccurrence> occurrences) {
        Map<String, Set<String>> valuesByPath = new HashMap<>();
        for (ReferenceValueOccurrence occurrence : occurrences) {
            if (!UID.matcher(occurrence.uid()).matches()) {
                throw new DomainValidationException(
                    ErrorCode.E4117,
                    submissionUid,
                    occurrence.elementPath());
            }
            Set<String> values = valuesByPath.computeIfAbsent(
                occurrence.elementPath(),
                ignored -> new HashSet<>());
            if (!values.add(occurrence.uid())) {
                throw new DomainValidationException(
                    ErrorCode.E4118,
                    submissionUid,
                    occurrence.uid(),
                    occurrence.elementPath());
            }
        }
    }
}
