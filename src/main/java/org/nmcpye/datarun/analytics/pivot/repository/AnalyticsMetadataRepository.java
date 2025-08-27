//package org.nmcpye.datarun.analytics.pivot.repository;
//
//import org.jooq.DSLContext;
//import org.jooq.JoinType;
//import org.nmcpye.datarun.jooq.tables.records.DataElementRecord;
//import org.nmcpye.datarun.jooq.tables.records.ElementTemplateConfigRecord;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//import static org.nmcpye.datarun.jooq.tables.DataElement.DATA_ELEMENT;
//import static org.nmcpye.datarun.jooq.tables.DataTemplate.DATA_TEMPLATE;
//import static org.nmcpye.datarun.jooq.tables.ElementTemplateConfig.ELEMENT_TEMPLATE_CONFIG;
//import static org.nmcpye.datarun.jooq.tables.PivotGridFacts.PIVOT_GRID_FACTS;
//
///**
// * @author Hamza Assada
// * @since 26/08/2025
// */
//
//@Repository
//public class AnalyticsMetadataRepository {
//
//    private final DSLContext dsl;
//
//    public AnalyticsMetadataRepository(DSLContext dsl) {
//        this.dsl = dsl;
//    }
//
//    /**
//     * Finds all DataElements that are present in the facts table.
//     * This ensures we only show measures for which data actually exists.
//     */
//    public List<DataElementRecord> findUsedDataElements() {
//        return dsl.selectDistinct(DATA_ELEMENT.fields())
//            .from(DATA_ELEMENT)
//            .join(PIVOT_GRID_FACTS).on(PIVOT_GRID_FACTS.ELEMENT_ID.eq(DATA_ELEMENT.ID))
//            .fetchInto(DataElementRecord.class);
//    }
//
//    /**
//     * Finds all ElementTemplateConfigs for a given form template UID.
//     */
//    public List<ElementTemplateConfigRecord> findElementConfigsByTemplate(String formTemplateUid) {
//        var query = dsl.selectQuery();
//        query.addFrom(ELEMENT_TEMPLATE_CONFIG);
//        query.addJoin(DATA_TEMPLATE, JoinType.JOIN, ELEMENT_TEMPLATE_CONFIG.TEMPLATE_ID.eq(DATA_TEMPLATE.ID));
//        query.addConditions(DATA_TEMPLATE.UID.eq(formTemplateUid));
//        return query.fetchInto(ElementTemplateConfigRecord.class);
//    }
//}
