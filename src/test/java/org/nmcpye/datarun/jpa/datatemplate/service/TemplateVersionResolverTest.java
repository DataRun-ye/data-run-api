package org.nmcpye.datarun.jpa.datatemplate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateVersionResolverTest {

    private DataTemplateInstanceService templateService;
    private TemplateVersionResolver resolver;
    private DataTemplateInstanceDto template;

    @BeforeEach
    void setUp() {
        templateService = mock(DataTemplateInstanceService.class);
        resolver = new TemplateVersionResolver(templateService);
        template = mock(DataTemplateInstanceDto.class);
        when(template.getAllElementPathMap()).thenReturn(Map.of());
    }

    @Test
    void resolvesExactVersionUid() {
        when(templateService.findByTemplateAndVersionUid(
            "formUid0001",
            "version0001"))
            .thenReturn(Optional.of(template));

        TemplateVersionContext result = resolver.resolveByUid(
            "formUid0001",
            "version0001");

        assertSame(template, result.getTemplate());
        verify(templateService).findByTemplateAndVersionUid(
            "formUid0001",
            "version0001");
    }

    @Test
    void resolvesExactVersionNumber() {
        when(templateService.findByTemplateAndVersionNo("formUid0001", 3))
            .thenReturn(Optional.of(template));

        TemplateVersionContext result = resolver.resolveByNumber(
            "formUid0001",
            3);

        assertSame(template, result.getTemplate());
        verify(templateService).findByTemplateAndVersionNo("formUid0001", 3);
    }

    @Test
    void aMissingVersionIsARefusalThePhoneCanShow() {
        when(templateService.findByTemplateAndVersionUid("formUid0001", "gone0000001"))
            .thenReturn(Optional.empty());
        when(templateService.findByTemplateAndVersionNo("formUid0001", 9))
            .thenReturn(Optional.empty());

        IllegalQueryException byUid = assertThrows(IllegalQueryException.class,
            () -> resolver.resolveByUid("formUid0001", "gone0000001"));
        IllegalQueryException byNumber = assertThrows(IllegalQueryException.class,
            () -> resolver.resolveByNumber("formUid0001", 9));

        assertEquals("Form version gone0000001 of form formUid0001 is not on the server",
            byUid.getMessage());
        assertEquals("Form version 9 of form formUid0001 is not on the server",
            byNumber.getMessage());
    }
}
