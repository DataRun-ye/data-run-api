//package org.nmcpye.datarun.analytics.pivot.service;
//
//import org.jooq.Field;
//import org.jooq.impl.DSL;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.nmcpye.datarun.analytics.pivot.MeasureValidationServiceImpl;
//import org.nmcpye.datarun.analytics.pivot.PivotFieldJooqMapper;
//import org.nmcpye.datarun.analytics.pivot.PivotMetadataService;
//import org.nmcpye.datarun.analytics.pivot.ValidatedMeasure;
//import org.nmcpye.datarun.analytics.pivot.dto.MeasureRequest;
//import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
//import org.nmcpye.datarun.jooq.Tables;
//import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
//import org.nmcpye.datarun.jpa.dataelement.service.DataElementService;
//import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
//
//import java.util.Map;
//import java.util.Optional;
//import java.util.Set;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//
//class MeasureValidationServiceImplTest {
//
//    PivotMetadataService pivotMetadataService = Mockito.mock(PivotMetadataService.class);
//    PivotFieldJooqMapper fieldMapper = new PivotFieldJooqMapper();
//    ElementTemplateConfigRepository etcRepo = Mockito.mock(ElementTemplateConfigRepository.class);
//    DataElementRepository deRepo = Mockito.mock(DataElementRepository.class);
//    org.jooq.DSLContext dsl = DSL.using(org.jooq.SQLDialect.POSTGRES);
//
//    MeasureValidationServiceImpl svc;
//
//    @BeforeEach
//    void setUp() {
//        svc = new MeasureValidationServiceImpl(pivotMetadataService, fieldMapper, etcRepo,
//            deRepo, dsl);
//    }
//
//    @Test
//    void testValidateNumericSum_templateEtc() {
//        PivotFieldDto dto = PivotFieldDto.builder()
//            .id("etc:500")
//            .label("Weight")
//            .dataType("value_num")
//            .aggregationModes(Set.of("SUM", "AVG", "COUNT"))
//            .source("element_template_config")
//            .templateModeOnly(true)
//            .category("FORM_MEASURE")
//            .extras(Map.of())
//            .build();
//
//        when(pivotMetadataService.resolveFieldByUidOrId(eq("etc:500"), anyString(), anyString()))
//            .thenReturn(Optional.of(dto));
//
//        MeasureRequest req = MeasureRequest.builder()
//            .elementIdOrUid("etc:500")
//            .aggregation("SUM")
//            .alias("sum_weight")
//            .build();
//
//        ValidatedMeasure vm = svc.validate(req, "tmpl-1", "v1");
//        assertNotNull(vm);
//        assertEquals(ValidatedMeasure.MeasureAggregation.SUM, vm.aggregation());
//        assertEquals("TEMPLATE", vm.effectiveMode());
//        assertEquals("sum_weight", vm.alias());
//        // targetField is PG.VALUE_NUM
//        assertEquals(Tables.PIVOT_GRID_FACTS.VALUE_NUM.getName(), ((Field<?>) vm.targetField()).getName());
//        // elementPredicate should be equality against element_template_config_uid
//        assertTrue(vm.elementPredicate().toString().contains("element_template_config_id"));
//    }
//
//    @Test
//    void testValidateOptionCount_withOption() {
//        PivotFieldDto dto = PivotFieldDto.builder()
//            .id("etc:600")
//            .label("Hobby")
//            .dataType("option_id")
//            .aggregationModes(Set.of("COUNT", "COUNT_DISTINCT"))
//            .source("element_template_config")
//            .templateModeOnly(true)
//            .category("FORM_MEASURE")
//            .extras(Map.of())
//            .build();
//
//        when(pivotMetadataService.resolveFieldByUidOrId(eq("etc:600"), anyString(), anyString()))
//            .thenReturn(Optional.of(dto));
//
//        MeasureRequest req = MeasureRequest.builder()
//            .elementIdOrUid("etc:600")
//            .aggregation("COUNT")
//            .optionId("opt-123")
//            .alias("count_hobby_opt123")
//            .build();
//
//        ValidatedMeasure vm = svc.validate(req, "tmpl-1", "v1");
//        assertNotNull(vm);
//        assertEquals(ValidatedMeasure.MeasureAggregation.COUNT, vm.aggregation());
//        assertEquals("count_hobby_opt123", vm.alias());
//        assertEquals("opt-123", vm.optionId());
//        // predicate should include option_id
//        assertTrue(vm.elementPredicate().toString().contains("option_id"));
//    }
//}
