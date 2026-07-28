package org.nmcpye.datarun.web.rest.v1.formtemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.datatemplateprocessor.FormTemplateProcessor;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.security.authorization.ResourceApiAuthorization;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormTemplateAuthoringResourceTest {

    private FormTemplateProcessor processor;
    private DataTemplateInstanceService service;
    private FormTemplateAuthoringResource resource;

    @BeforeEach
    void setUp() {
        processor = mock(FormTemplateProcessor.class);
        service = mock(DataTemplateInstanceService.class);
        resource = new FormTemplateAuthoringResource(
            processor,
            service,
            mock(ResourceApiAuthorization.class));
    }

    @Test
    void publishesProcessedDefinitionAsFirstVersion() {
        PublicationFixture fixture = fixture("newForm0001", false);

        ResponseEntity<EntitySaveSummaryVM> response =
            resource.saveOne(fixture.request());

        EntitySaveSummaryVM summary = response.getBody();
        assertNotNull(summary);
        assertEquals(List.of("newForm0001"), summary.getCreated());
        assertEquals(List.of(), summary.getUpdated());
        verify(service).publishVersion(fixture.processed());
    }

    @Test
    void publishesProcessedDefinitionAsNextVersion() {
        PublicationFixture fixture = fixture("oldForm0001", true);

        ResponseEntity<EntitySaveSummaryVM> response =
            resource.saveOne(fixture.request());

        EntitySaveSummaryVM summary = response.getBody();
        assertNotNull(summary);
        assertEquals(List.of(), summary.getCreated());
        assertEquals(List.of("oldForm0001"), summary.getUpdated());
        verify(service).publishVersion(fixture.processed());
    }

    private PublicationFixture fixture(String uid, boolean existing) {
        DataTemplateInstanceDto request = mock(DataTemplateInstanceDto.class);
        DataTemplateInstanceDto processed =
            mock(DataTemplateInstanceDto.class);
        DataTemplateInstanceDto saved = mock(DataTemplateInstanceDto.class);
        when(processor.validate(request)).thenReturn(request);
        when(processor.processMetadata(request)).thenReturn(processed);
        when(processed.getUid()).thenReturn(uid);
        when(saved.getUid()).thenReturn(uid);
        when(service.existsByUid(uid)).thenReturn(existing);
        when(service.publishVersion(processed)).thenReturn(saved);
        return new PublicationFixture(request, processed);
    }

    private record PublicationFixture(
        DataTemplateInstanceDto request,
        DataTemplateInstanceDto processed) {
    }
}
