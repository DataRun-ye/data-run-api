package org.nmcpye.datarun.jpa.reference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.ReferenceDefinitionV1Dto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceSubmissionResolverTest {

    private AssignmentRepository assignmentRepository;
    private ReferenceEntryRepository referenceEntryRepository;
    private ReferenceValueExtractor extractor;
    private ReferenceSubmissionResolver resolver;
    private DataSubmission submission;
    private DataTemplateInstanceDto template;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(AssignmentRepository.class);
        referenceEntryRepository = mock(ReferenceEntryRepository.class);
        extractor = mock(ReferenceValueExtractor.class);
        resolver = new ReferenceSubmissionResolver(
            assignmentRepository,
            referenceEntryRepository,
            extractor,
            new ReferenceDisplayNamePolicy());

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setId("org-unit-id");
        Activity activity = new Activity();
        activity.setId("activity-id");
        assignment = new Assignment();
        assignment.setUid("assignment1");
        assignment.setOrgUnit(orgUnit);
        assignment.setActivity(activity);

        submission = new DataSubmission();
        submission.setUid("submission1");
        submission.setAssignment("assignment1");
        template = mock(DataTemplateInstanceDto.class);

        when(assignmentRepository.findByUid("assignment1"))
            .thenReturn(Optional.of(assignment));
    }

    @Test
    void knownReferencePreservesCanonicalNameAndLineage() {
        extracted("fieldId", "a1234567890");
        ReferenceEntry known = entry(
            "a1234567890",
            assignment.getOrgUnit(),
            "Canonical Existing Name");
        when(referenceEntryRepository.findAllByUidIn(
            Set.of("a1234567890")))
            .thenReturn(List.of(known));

        resolver.resolve(
            submission,
            template,
            List.of(definition(
                "a1234567890",
                "Different Imported Name")));

        verify(referenceEntryRepository, never()).persistAllAndFlush(any());
        assertEquals("Canonical Existing Name", known.getDisplayName());
    }

    @Test
    void createsUnknownReferenceWithDerivedScopeAndLineage() {
        extracted("fieldId", "b1234567890");
        when(referenceEntryRepository.findAllByUidIn(
            Set.of("b1234567890")))
            .thenReturn(List.of());

        resolver.resolve(
            submission,
            template,
            List.of(definition(
                "b1234567890",
                "  Ahmed   Ali Saleh Hassan  ")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReferenceEntry>> created =
            ArgumentCaptor.forClass(List.class);
        verify(referenceEntryRepository).persistAllAndFlush(created.capture());
        ReferenceEntry createdEntry = created.getValue().get(0);
        assertEquals("b1234567890", createdEntry.getUid());
        assertEquals(
            "Ahmed Ali Saleh Hassan",
            createdEntry.getDisplayName());
        assertSame(
            assignment.getOrgUnit(),
            createdEntry.getOrgUnit());
        assertSame(
            assignment.getActivity(),
            createdEntry.getFirstRegisteredActivity());
    }

    @Test
    void persistsTwoHundredFiftyUnknownReferencesInOneBatch() {
        List<ReferenceValueOccurrence> occurrences = IntStream.range(0, 250)
            .mapToObj(index -> new ReferenceValueOccurrence(
                "repeat.reference",
                referenceUid(index)))
            .toList();
        List<ReferenceDefinitionV1Dto> definitions = IntStream.range(0, 250)
            .mapToObj(index -> definition(
                referenceUid(index),
                "Ahmed Ali Saleh Hassan"))
            .toList();
        when(extractor.extract(
            submission.getUid(),
            template,
            submission.getFormData()))
            .thenReturn(occurrences);
        when(referenceEntryRepository.findAllByUidIn(any()))
            .thenReturn(List.of());

        resolver.resolve(submission, template, definitions);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReferenceEntry>> created =
            ArgumentCaptor.forClass(List.class);
        verify(referenceEntryRepository).findAllByUidIn(any());
        verify(referenceEntryRepository).persistAllAndFlush(created.capture());
        assertEquals(250, created.getValue().size());
    }

    @Test
    void acceptsSameUidAtDifferentReferencePaths() {
        when(extractor.extract(
            submission.getUid(),
            template,
            submission.getFormData()))
            .thenReturn(List.of(
                new ReferenceValueOccurrence(
                    "firstReference",
                    "a1234567890"),
                new ReferenceValueOccurrence(
                    "secondReference",
                    "a1234567890")));
        when(referenceEntryRepository.findAllByUidIn(
            Set.of("a1234567890")))
            .thenReturn(List.of(entry(
                "a1234567890",
                assignment.getOrgUnit(),
                "Canonical Existing Name")));

        resolver.resolve(submission, template, List.of());

        verify(referenceEntryRepository, never()).persistAllAndFlush(any());
    }

    @Test
    void rejectsDuplicateWithinOneReferencePath() {
        when(extractor.extract(
            submission.getUid(),
            template,
            submission.getFormData()))
            .thenReturn(List.of(
                new ReferenceValueOccurrence(
                    "repeat.reference",
                    "a1234567890"),
                new ReferenceValueOccurrence(
                    "repeat.reference",
                    "a1234567890")));

        assertThrows(
            DomainValidationException.class,
            () -> resolver.resolve(submission, template, List.of()));
        verify(referenceEntryRepository, never()).findAllByUidIn(any());
    }

    @Test
    void rejectsUnknownMissingDefinitionAndUnusedDefinition() {
        extracted("fieldId", "a1234567890");
        when(referenceEntryRepository.findAllByUidIn(
            Set.of("a1234567890")))
            .thenReturn(List.of());

        assertThrows(
            DomainValidationException.class,
            () -> resolver.resolve(submission, template, List.of()));
        assertThrows(
            DomainValidationException.class,
            () -> resolver.resolve(
                submission,
                template,
                List.of(definition(
                    "b1234567890",
                    "Ahmed Ali Saleh Hassan"))));
    }

    @Test
    void rejectsKnownReferenceFromAnotherOrgUnit() {
        extracted("fieldId", "a1234567890");
        OrgUnit otherOrgUnit = new OrgUnit();
        otherOrgUnit.setId("other-org-unit");
        when(referenceEntryRepository.findAllByUidIn(
            Set.of("a1234567890")))
            .thenReturn(List.of(entry(
                "a1234567890",
                otherOrgUnit,
                "Canonical Existing Name")));

        assertThrows(
            DomainValidationException.class,
            () -> resolver.resolve(submission, template, List.of()));
    }

    @Test
    void rejectsMalformedUidBeforeReadingCatalog() {
        extracted("fieldId", "invalid");

        assertThrows(
            DomainValidationException.class,
            () -> resolver.resolve(submission, template, List.of()));
        verify(referenceEntryRepository, never()).findAllByUidIn(any());
    }

    private void extracted(String path, String uid) {
        when(extractor.extract(
            submission.getUid(),
            template,
            submission.getFormData()))
            .thenReturn(List.of(new ReferenceValueOccurrence(path, uid)));
    }

    private ReferenceEntry entry(
        String uid,
        OrgUnit orgUnit,
        String name) {
        ReferenceEntry entry = new ReferenceEntry();
        entry.setUid(uid);
        entry.setOrgUnit(orgUnit);
        entry.setDisplayName(name);
        return entry;
    }

    private ReferenceDefinitionV1Dto definition(
        String uid,
        String name) {
        ReferenceDefinitionV1Dto definition =
            new ReferenceDefinitionV1Dto();
        definition.setUid(uid);
        definition.setName(name);
        return definition;
    }

    private String referenceUid(int index) {
        return "r" + String.format("%010d", index);
    }
}
