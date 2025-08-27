package org.nmcpye.datarun.analytics.pivot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nmcpye.datarun.analytics.pivot.AllowedAggregationsResolver;
import org.nmcpye.datarun.analytics.pivot.PivotMetadataServiceImpl;
import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.analytics.pivot.dto.PivotMetadataResponse;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

class PivotMetadataServiceImplTest {

    ElementTemplateConfigRepository etcRepo = Mockito.mock(ElementTemplateConfigRepository.class);
    DataElementRepository deRepo = Mockito.mock(DataElementRepository.class);
    AllowedAggregationsResolver aggrResolver = new AllowedAggregationsResolver();
    PivotMetadataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PivotMetadataServiceImpl(etcRepo, deRepo, aggrResolver);
    }

    @Test
    void testGetMetadataForTemplate_numericAndSelect() {
        // prepare a numeric element template config
        ElementTemplateConfig etcNum = new ElementTemplateConfig();
        etcNum.setId(100L);
        etcNum.setTemplateId("tmpl-1");
        etcNum.setTemplateVersionId("v1");
        etcNum.setDataElementId("de-num-1");
        etcNum.setDisplayLabel(Map.of("en", "Weight"));
        etcNum.setIsMulti(false);
        etcNum.setIsReference(false);

        ElementTemplateConfig etcOpt = new ElementTemplateConfig();
        etcOpt.setId(101L);
        etcOpt.setTemplateId("tmpl-1");
        etcOpt.setTemplateVersionId("v1");
        etcOpt.setDataElementId("de-opt-1");
        etcOpt.setDisplayLabel(Map.of("en", "Hobbies"));
        etcOpt.setIsMulti(true); // multi-select

        when(etcRepo.findAllByTemplateIdAndTemplateVersionId("tmpl-1", "v1"))
            .thenReturn(List.of(etcNum, etcOpt));

        // data element definitions
        DataElement deNum = new DataElement();
        //
        deNum.setId("de-num-1");
        deNum.setType(ValueType.Number);

        DataElement deOpt = new DataElement();
        //
        deOpt.setId("de-opt-1");
        deOpt.setType(ValueType.SelectMulti);

        when(deRepo.findAllById(anyCollection()))
            .thenReturn(List.of(deNum, deOpt));

        PivotMetadataResponse resp = service
            .getMetadataForTemplate("tmpl-1", "v1");

        assertNotNull(resp);
        assertEquals(2, resp.getMeasures().size());

        PivotFieldDto numDto = resp.getMeasures()
            .stream().filter(d ->
                d.id().equals("etc:100")).findFirst().orElse(null);
        assertNotNull(numDto);
        assertEquals("value_num", numDto.dataType());
        assertTrue(numDto.aggregationModes().contains("SUM"));
        assertEquals("element_template_config", numDto.source());

        PivotFieldDto optDto = resp.getMeasures().stream().filter(d -> d.id().equals("etc:101")).findFirst().orElse(null);
        assertNotNull(optDto);
        assertEquals("option_id", optDto.dataType());
        assertTrue(optDto.aggregationModes().contains("COUNT"));
    }
}

