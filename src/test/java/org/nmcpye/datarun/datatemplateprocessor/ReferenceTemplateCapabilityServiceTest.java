package org.nmcpye.datarun.datatemplateprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.FieldTemplateElementDto;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReferenceTemplateCapabilityServiceTest {

    private TemplateVersionRepository repository;
    private ReferenceTemplateCapabilityService service;

    @BeforeEach
    void setUp() {
        repository = mock(TemplateVersionRepository.class);
        service = new ReferenceTemplateCapabilityService(repository);
    }

    @Test
    void returnsOnlyLatestTemplatesContainingReferenceFields() {
        TemplateVersion ordinary = version("ordinary01", ValueType.Text);
        TemplateVersion reference = version("reference01", ValueType.Reference);

        when(repository.findLatestByTemplateUidIn(Set.of("ordinary01", "reference01")))
                .thenReturn(List.of(ordinary, reference));

        assertEquals(
                Set.of("reference01"),
                service.findReferenceTemplateUids(Set.of("ordinary01", "reference01")));
        verify(repository).findLatestByTemplateUidIn(Set.of("ordinary01", "reference01"));
    }

    @Test
    void emptyInputDoesNotQueryTheDatabase() {
        assertFalse(service.containsReference(Set.of()));
        verifyNoInteractions(repository);
    }

    @Test
    void historicalReferenceVersionKeepsTheTemplateIdentifiedForScopeLocking() {
        TemplateVersion oldReference = version("reference01", ValueType.Reference);
        TemplateVersion latestOrdinary = version("reference01", ValueType.Text);
        when(repository.findByTemplateUidIn(Set.of("reference01")))
                .thenReturn(List.of(oldReference, latestOrdinary));

        assertEquals(
                Set.of("reference01"),
                service.findReferenceTemplateUidsAcrossVersions(Set.of("reference01")));
    }

    private TemplateVersion version(String templateUid, ValueType type) {
        FieldTemplateElementDto field = new FieldTemplateElementDto();
        field.setType(type);

        TemplateVersion version = new TemplateVersion();
        version.setTemplateUid(templateUid);
        version.setFields(List.of(field));
        return version;
    }
}
