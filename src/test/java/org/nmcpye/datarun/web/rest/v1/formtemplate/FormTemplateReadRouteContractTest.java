package org.nmcpye.datarun.web.rest.v1.formtemplate;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateService;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class FormTemplateReadRouteContractTest {

    @Test
    void releasedMobileTemplateReadRoutesRemainRegistered() {
        assertArrayEquals(
            new String[]{"/api/v1/formTemplates"},
            FormTemplateResource.class.getAnnotation(RequestMapping.class).value());
        assertArrayEquals(
            new String[]{"/api/v1/formTemplateVersions"},
            TemplateVersionResource.class.getAnnotation(RequestMapping.class).value());
    }

    @Test
    void templateReadMappingsRegisterWithoutAmbiguity() {
        FormTemplateResource templates = new FormTemplateResource(
            mock(DataTemplateService.class),
            mock(DataTemplateRepository.class));
        TemplateVersionResource versions = new TemplateVersionResource(
            mock(TemplateVersionService.class),
            mock(TemplateVersionRepository.class));

        assertDoesNotThrow(
            () -> MockMvcBuilders.standaloneSetup(templates, versions).build());
    }
}
