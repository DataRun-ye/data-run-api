package org.nmcpye.datarun.jpa.datatemplate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateDto;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.jpa.datatemplate.mapper.DataTemplateMapper;
import org.nmcpye.datarun.jpa.datatemplate.mapper.FormJpaTemplateVersionMapper;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.jpa.datatemplategenerator.TemplateElementGeneratorService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataTemplateInstanceServiceImplTest {

    private DataTemplateService masterService;
    private DataTemplateRepository masterRepository;
    private FormJpaTemplateVersionMapper versionMapper;
    private TemplateVersionRepository versionRepository;
    private DataTemplateMapper templateMapper;
    private TemplateElementGeneratorService elementGenerator;
    private DataTemplateInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        masterService = mock(DataTemplateService.class);
        masterRepository = mock(DataTemplateRepository.class);
        versionMapper = mock(FormJpaTemplateVersionMapper.class);
        versionRepository = mock(TemplateVersionRepository.class);
        templateMapper = mock(DataTemplateMapper.class);
        elementGenerator = mock(TemplateElementGeneratorService.class);
        service = new DataTemplateInstanceServiceImpl(
            masterService,
            masterRepository,
            versionMapper,
            versionRepository,
            templateMapper,
            elementGenerator);
    }

    @Test
    void publishesFirstVersionAndGeneratesProjectionOnce() {
        PublicationFixture fixture = fixture("newForm0001");
        when(masterRepository.findByUidForWrite("newForm0001"))
            .thenReturn(Optional.empty());
        when(masterRepository.persist(fixture.incoming()))
            .thenReturn(fixture.incoming());

        DataTemplateInstanceDto result =
            service.publishVersion(fixture.request());

        assertSame(fixture.saved(), result);
        assertEquals(1, fixture.incoming().getVersionNumber());
        assertNotNull(fixture.incoming().getVersionUid());
        verify(elementGenerator, times(1)).generate(
            "newForm0001",
            fixture.saved().getVersionUid());
    }

    @Test
    void publishesNextVersionAndGeneratesProjectionOnce() {
        PublicationFixture fixture = fixture("oldForm0001");
        DataTemplate existing = fixture.incoming();
        existing.setVersionNumber(3);
        existing.setVersionUid("oldVersion1");
        when(masterRepository.findByUidForWrite("oldForm0001"))
            .thenReturn(Optional.of(existing));

        DataTemplateInstanceDto result =
            service.publishVersion(fixture.request());

        assertSame(fixture.saved(), result);
        assertEquals(4, existing.getVersionNumber());
        assertNotNull(existing.getVersionUid());
        verify(elementGenerator, times(1)).generate(
            "oldForm0001",
            fixture.saved().getVersionUid());
    }

    private PublicationFixture fixture(String uid) {
        DataTemplateInstanceDto request = mock(DataTemplateInstanceDto.class);
        DataTemplate incoming = new DataTemplate();
        incoming.setUid(uid);
        incoming.setName("Form");
        TemplateVersion version = new TemplateVersion();
        DataTemplateDto masterDto = mock(DataTemplateDto.class);
        FormTemplateVersionDto versionDto = mock(FormTemplateVersionDto.class);
        DataTemplateInstanceDto saved = mock(DataTemplateInstanceDto.class);
        when(saved.getUid()).thenReturn(uid);
        when(saved.getVersionUid()).thenReturn("newVersion1");

        when(templateMapper.fromInstanceDto(request)).thenReturn(incoming);
        when(versionMapper.fromInstanceDto(request)).thenReturn(version);
        when(versionRepository.persist(version)).thenReturn(version);
        when(masterRepository.merge(incoming)).thenReturn(incoming);
        when(templateMapper.toDto(incoming)).thenReturn(masterDto);
        when(versionMapper.toDto(version)).thenReturn(versionDto);
        when(templateMapper.toInstanceDto(masterDto, versionDto))
            .thenReturn(saved);

        return new PublicationFixture(request, incoming, saved);
    }

    private record PublicationFixture(
        DataTemplateInstanceDto request,
        DataTemplate incoming,
        DataTemplateInstanceDto saved) {
    }
}
