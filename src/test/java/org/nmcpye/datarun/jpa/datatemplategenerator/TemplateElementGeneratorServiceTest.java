package org.nmcpye.datarun.jpa.datatemplategenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.MetadataUpsertService;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.jpa.option.repository.OptionSetRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateElementGeneratorServiceTest {

    private TemplateVersionRepository versionRepository;
    private FlatTemplateProcessor flatProcessor;
    private MetadataUpsertService metadataUpsertService;
    private OptionSetRepository optionSetRepository;
    private TemplateElementGeneratorService service;

    @BeforeEach
    void setUp() {
        versionRepository = mock(TemplateVersionRepository.class);
        flatProcessor = mock(FlatTemplateProcessor.class);
        metadataUpsertService = mock(MetadataUpsertService.class);
        optionSetRepository = mock(OptionSetRepository.class);
        service = new TemplateElementGeneratorService(
            versionRepository,
            flatProcessor,
            new TemplateElementBuilderImpl(),
            metadataUpsertService,
            optionSetRepository);
    }

    @Test
    void projectsRepeatAndFieldMetadataFromPublishedVersion() {
        TemplateVersion version = new TemplateVersion();
        version.setUid("version0001");
        version.setTemplateUid("formUid0001");
        version.setVersionNumber(1);

        FormSectionConf repeat = new FormSectionConf();
        repeat.setName("households");
        repeat.setPath("households");
        repeat.setRepeatable(true);

        FormDataElementConf field = new FormDataElementConf();
        field.setId("fieldUid001");
        field.setName("householdName");
        field.setPath("households.fieldUid001");
        field.setParent("households");
        field.setType(ValueType.Text);

        when(versionRepository.findByTemplateUidAndUid(
            "formUid0001",
            "version0001"))
            .thenReturn(Optional.of(version));
        when(flatProcessor.process(version)).thenReturn(
            new FlatTemplateProcessor.TemplateFlatSnapshot(
                Map.of("households", repeat),
                List.of(field)));
        when(optionSetRepository.findAllByUidIn(any()))
            .thenReturn(List.of());

        List<TemplateElement> generated = service.generate(
            "formUid0001",
            "version0001");

        assertEquals(2, generated.size());
        ArgumentCaptor<List<CanonicalElement>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(metadataUpsertService).upsertCanonicalElements(captor.capture());
        verify(metadataUpsertService).upsertTemplateElements(generated);

        List<CanonicalElement> canonical = captor.getValue();
        assertEquals(2, canonical.size());
        CanonicalElement repeatElement = canonical.stream()
            .filter(CanonicalElement::isRepeatCE)
            .findFirst()
            .orElseThrow();
        CanonicalElement fieldElement = canonical.stream()
            .filter(element -> element.getSemanticType() != SemanticType.Repeat)
            .findFirst()
            .orElseThrow();
        assertNotNull(repeatElement.getId());
        assertEquals("households", repeatElement.getCanonicalPath());
        assertEquals("households.householdName",
            fieldElement.getCanonicalPath());
        assertEquals(repeatElement.getId(), fieldElement.getParentRepeatId());
        assertTrue(fieldElement.getJsonDataPaths()
            .contains("households.householdName"));
    }
}
