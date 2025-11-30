//package org.nmcpye.datarun.analytics.domaintabletoolkit;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.analytics.ElementMetadataService;
//import org.nmcpye.datarun.analytics.domaintabletoolkit.model.CeMeta;
//import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ProjectAnalyticsMetadata;
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * The core of the toolkit: takes a {@link ProjectAnalyticsMetadata} and gets the SQL DDL.
// * <p>
// * construct the full, executable SQL DDL string for the materialized view and its indexes.
// *
// * @author Hamza Assada
// * @since 25/08/2025
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class WideViewDdlBuilderService {
//    private final ElementMetadataService metadataService;
//    private String VIEW_PREFIX = "analytics_facts_wide_";
//
//    // CREATE UNIQUE INDEX my_unique_idx ON my_unique_materialized_view (section_id, subsection_repeat_id, option_id) NULLS NOT DISTINCT;
//
//    private String generateTargetDdl(String projectUid) {
//        ProjectAnalyticsMetadata metadata = metadataService.getPivotableElements(projectUid);
//        final var elements = metadata.elements();
//        StringBuilder columnsBuilder = new StringBuilder();
//        Set<String> projectElementIds = metadata.elements().stream()
//            .map(CeMeta::columnAlias).collect(Collectors.toSet());
//
//        StringBuilder ddl = new StringBuilder();
////        ddl.append(String.format("CREATE MATERIALIZED VIEW %s AS\nSELECT\n", viewName));
//        ddl.append("    -- Grain and Hierarchy Identifiers\n");
//        ddl.append("    sub.id AS submission_id,\n");
//        ddl.append("    ri.id AS repeat_instance_id,\n");
//        //  -- Core Denormalized Dimensions
//        ddl.append("    asgn.org_unit_id AS,\n");
//        ddl.append("    asgn.activity_id AS,\n");
//        ddl.append("    asgn.team_id,\n");
//        ddl.append("    sub.finished_entry_time AS submission_completed_at,\n");
//
//        // checkout Docs: 1UeHrPBnis-2gvbppPKV8CffcDYuWMROb
//        // -- Category Dimensions (live resolution)
//        //    ri.category_id,
//        ddl.append("    ri.category_id,\n");
//        ddl.append("    COALESCE(ri.category_kind, 'N/A') AS category_kind,\n");
//        ddl.append("    COALESCE(t.name, ou.name, de.name, child_opt.name, ri.category_name, '(Uncategorized)') AS category_name,\n");
//        ddl.append("    sub.finished_entry_time AS submission_completed_at,\n");
//
//        //  ------------------------------------------------------------------
//        //    -- DYNAMICALLY GENERATED PIVOTED COLUMNS
//        //    ------------------------------------------------------------------
//        //  append dynamics
//        //
//
//        // -------------
//        //  -- Example for a MEASURE element ('Child Age')
//        //    iterate through default aggregations and calculate each aggregation type value for the element
//        //    SUM(CASE WHEN ev.element_id = '01H...Age' THEN ev.value_num ELSE 0 END) AS child_age_sum,
//        //    COUNT(CASE WHEN ev.element_id = '01H...Age' THEN ev.value_num END) AS child_age_count,
//
//        // -------
//        //   -- Example for a DIMENSION element ('Water Source' - a SelectOne)
//        //    -- We aggregate into an array for consistency, it will just have one element.
//        //    ARRAY_AGG(ev.option_id) FILTER (WHERE ev.element_id = '01H...WaterSource') AS water_source_option,
//        //
//        //    -- Example for a MULTI-SELECT element ('Reported Symptoms')
//        //    -- ARRAY_AGG correctly collects all option_ids for the group into a single array column.
//        //    ARRAY_AGG(ev.option_id) FILTER (WHERE ev.element_id = '01H...Symptoms') AS reported_symptoms_options
//        // FROM
//        //    data_submission sub
//        //    JOIN assignment asgn ON sub.assignment_id = asgn.id
//        //    LEFT JOIN repeat_instance ri ON sub.id = ri.submission_id
//        //    LEFT JOIN element_data_value ev ON sub.id = ev.submission_id AND COALESCE(ri.id, '') = COALESCE(ev.repeat_instance_id, '')
//        //    -- LEFT JOINs for live category names (team t, org_unit ou, etc.)
//        //WHERE
//        //    sub.deleted_at IS NULL
//        //GROUP BY
//        //    sub.id,
//        //    asgn.team_id,
//        //    asgn.org_unit_id,
//        //    asgn.activity_id,
//        //    ri.id;
//        for (final var element : elements) {
//            String columnName = element.columnAlias();
//            String valueColumn = mapValueTypeToColumn(element); // "Number" -> "ev.value_num"
//
//            columnsBuilder.append(String.format(
//                ",\n    MAX(CASE WHEN ev.element_id = '%s' THEN %s END) AS %s",
//                element.elementId(),
//                valueColumn,
//                columnName
//            ));
//        }
//
//        String VIEW_NAME = VIEW_PREFIX + metadata.activityAlias();
//
//        // Using a template for the static part of the query is best practice
//        return String.format(
//            """
//                CREATE MATERIALIZED VIEW %s AS
//                SELECT
//                    sub.id AS submission_id,
//                    sub.assignment_id,
//                    sub.team_id,
//                    sub.org_unit_id,
//                    sub.activity_id,
//                    ri.id AS repeat_instance_id
//                    %s
//                FROM
//                    data_submission sub
//                    LEFT JOIN repeat_instance ri ON sub.id = ri.submission_id
//                    LEFT JOIN element_data_value ev ON sub.id = ev.submission_id AND COALESCE(ri.id, '') = COALESCE(ev.repeat_instance_id, '')
//                WHERE
//                    sub.deleted_at IS NULL
//                GROUP BY
//                    sub.id,
//                    ri.id;
//                """, VIEW_NAME, columnsBuilder.toString()
//        );
//    }
//
//
//    private String mapValueTypeToColumn(CeMeta element) {
//        final var semanticType = element.semanticType();
//        if (semanticType != null) {
//            if (semanticType.isRef()) {
//                return switch (element.semanticType()) {
//                    case OrgUnit, Team, Activity, Option -> "ev.value_ref_uid";
//                    case MultiSelectOption -> "ev.value_json";
//                    default -> "ev.value_text";
//                };
//            }
//
//        }
//
//        return switch (element.dataType()) {
//            case INTEGER, DECIMAL -> "ev.value_number";
//            default -> "ev.value_text";
//        };
//    }
//}
