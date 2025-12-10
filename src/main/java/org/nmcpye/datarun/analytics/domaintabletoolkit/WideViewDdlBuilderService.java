package org.nmcpye.datarun.analytics.domaintabletoolkit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.analytics.ElementMetadataService;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ActivityAnalyticsMetadata;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.CeMeta;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The core of the toolkit: takes a {@link ActivityAnalyticsMetadata} and gets the SQL DDL.
 * <p>
 * construct the full, executable SQL DDL string for the materialized view and its indexes.
 *
 * @author Hamza Assada
 * @since 25/08/2025
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WideViewDdlBuilderService {
    private final ElementMetadataService metadataService;
    private String VIEW_PREFIX = "analytics_facts_wide_";

    // CREATE UNIQUE INDEX my_unique_idx ON my_unique_materialized_view (section_id, subsection_repeat_id, option_id) NULLS NOT DISTINCT;

    private String generateTargetDdl(String activityUid) {
        ActivityAnalyticsMetadata metadata = metadataService.getPivotableElements(activityUid);
        final var elements = metadata.elements();
        StringBuilder columnsBuilder = new StringBuilder();
        Set<String> columns = metadata.elements().stream()
            .map(CeMeta::columnAlias).collect(Collectors.toSet());

        StringBuilder ddl = new StringBuilder();
        ddl.append(String.format("CREATE MATERIALIZED VIEW %s AS\nSELECT\n", metadata.activityAlias()));
        ddl.append("   --  submission_uid,\n");
        ddl.append("    --  repeat_instance_id,\n");
        ddl.append("   -- org_unit_uid AS,\n");
        ddl.append("   -- org_unit_code AS,\n");
        ddl.append("   -- org_unit_name AS,\n");
        ddl.append("   --  activity_uid AS,\n");
        ddl.append("   --  activity_code AS,\n");
        ddl.append("  --   activity_name AS,\n");
        ddl.append("  --   anchor_* AS,\n");
        ddl.append("  --   team_uid,\n");
        ddl.append("  --   team_code,\n");
        ddl.append("    ...submission_creation_time,\n");
        // keep going for all grouping dims in enriched tables

        //  ------------------------------------------------------------------
        //    -- DYNAMICALLY GENERATED PIVOTED COLUMNS, we will use data type to infer the column,
        //    ------------------------------------------------------------------
        for (final var element : elements) {
            String columnName = element.columnAlias();
            String valueColumn = mapValueTypeToColumn(element); // "Number" -> "ev.value_num"

            columnsBuilder.append(String.format(
                ",\n    MAX(CASE WHEN ev.element_id = '%s' THEN %s END) AS %s",
                element.elementId(),
                valueColumn,
                columnName
            ));
        }

        String VIEW_NAME = VIEW_PREFIX + metadata.activityAlias();

        return String.format(
            """
                CREATE MATERIALIZED VIEW %s AS
                SELECT
                    submission_uid,
                    sub.assignment_uid,
                    sub.team_uid,
                    sub.org_unit_uid,
                    sub.activity_uid,
                    -- rest ...
                    %s
                FROM
                    --
                """, VIEW_NAME, columnsBuilder.toString()
        );
    }


    private String mapValueTypeToColumn(CeMeta element) {
        final var semanticType = element.semanticType();
        if (semanticType != null) {
            if (semanticType.isRef()) {
                return switch (element.semanticType()) {
                    case OrgUnit, Team, Activity, Option -> "ev.value_ref_uid";
                    case MultiSelectOption -> "ev.value_json";
                    default -> "ev.value_text";
                };
            }

        }

        return switch (element.dataType()) {
            case INTEGER, DECIMAL -> "ev.value_number";
            default -> "ev.value_text";
        };
    }

    private void refreshView() {
        // This command requires a unique index to exist on the view
//        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + VIEW_NAME + ";");
    }

}
