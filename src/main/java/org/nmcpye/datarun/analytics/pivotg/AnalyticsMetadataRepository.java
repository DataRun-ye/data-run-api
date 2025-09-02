package org.nmcpye.datarun.analytics.pivotg;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JoinType;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.jooq.tables.DataTemplate;
import org.nmcpye.datarun.jooq.tables.ElementTemplateConfig;
import org.nmcpye.datarun.jooq.tables.records.DataElementRecord;
import org.nmcpye.datarun.jooq.tables.records.ElementTemplateConfigRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.nmcpye.datarun.jooq.tables.DataElement.DATA_ELEMENT;
import static org.nmcpye.datarun.jooq.tables.DataTemplate.DATA_TEMPLATE;
import static org.nmcpye.datarun.jooq.tables.ElementTemplateConfig.ELEMENT_TEMPLATE_CONFIG;
import static org.nmcpye.datarun.jooq.tables.PivotGridFacts.PIVOT_GRID_FACTS;

/**
 * @author Hamza Assada
 * @since 26/08/2025
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsMetadataRepository {

    private final DSLContext dsl;

    /**
     * Finds all DataElements that are present in the facts table.
     * This ensures we only show measures for which data actually exists.
     */
    public List<DataElementRecord> findUsedDataElements() {
        return dsl.selectDistinct(DATA_ELEMENT.fields())
            .from(DATA_ELEMENT)
            .join(PIVOT_GRID_FACTS)
            .on(PIVOT_GRID_FACTS.DE_UID.eq(DATA_ELEMENT.UID))
            .fetchInto(DataElementRecord.class).stream().toList();
    }

    /**
     * Finds all ElementTemplateConfigs for a given form template UID, and templateVersionUid or latest
     *
     * @param formTemplateUid    form template UID
     * @param templateVersionUid form template version UID, if null, fetch latest
     * @return list of JOOQ ETC records matching the criteria
     */
    public List<ElementTemplateConfigRecord> findElementConfigsByTemplate(
        @NotNull @Size(max = 11) String formTemplateUid,
        @Size(max = 11) String templateVersionUid) {

        final ElementTemplateConfig etc = ELEMENT_TEMPLATE_CONFIG;
        final DataTemplate dt = DATA_TEMPLATE;

        SelectQuery<Record> query = dsl.selectQuery();
        query.addFrom(etc);
        query.addJoin(dt, JoinType.JOIN,
            etc.TEMPLATE_UID.eq(dt.UID));

        // ensure we only look at configs for the requested form template
        query.addConditions(dt.UID.eq(formTemplateUid));

        if (templateVersionUid != null) {
            // fetch for the exact template version uid supplied
            query.addConditions(etc.TEMPLATE_VERSION_UID.eq(templateVersionUid));
        } else {
            // templateVersionUid not supplied -> pick the latest TEMPLATE_VERSION_NO for this form template
            var maxVersionSubquery =
                dsl.select(DSL.max(etc.TEMPLATE_VERSION_NO)).from(etc)
                    .where(etc.TEMPLATE_UID.eq(formTemplateUid));

            query.addConditions(etc.TEMPLATE_VERSION_NO.eq(maxVersionSubquery));
        }

        return query.fetchInto(ElementTemplateConfigRecord.class);
    }
}
