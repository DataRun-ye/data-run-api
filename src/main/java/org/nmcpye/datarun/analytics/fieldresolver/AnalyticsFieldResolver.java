package org.nmcpye.datarun.analytics.fieldresolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
import org.springframework.stereotype.Component;

import static org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS;

/**
 * The single source of truth for mapping a public, standardized field identifier
 * from the API contract to a physical, strongly-typed jOOQ database field.
 * This service completely decouples the query construction logic from the
 * underlying database schema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsFieldResolver {

    private static final PivotGridFacts PG = PIVOT_GRID_FACTS;

    /**
     * Resolves a standardized field ID into a jOOQ Field for use in dimensions,
     * grouping, and filtering.
     *
     * @param standardizedId The public-facing ID from the API contract (e.g., "core:team_uid").
     * @return The corresponding jOOQ Field from the pivot_grid_facts view.
     * @throws InvalidRequestException if the field ID cannot be resolved.
     */
    public Field<?> resolveDimensionField(String standardizedId) {
        MappedQueryableElement field = MappedQueryableElement.from(standardizedId);
        // The namespace determines the context for resolution.
        return switch (field.namespace()) {
            case "core" -> resolveCoreField(field.value());
            // For 'etc' and 'de', the goal is not to get a value field,
            // but the ID field used for predicates. The validation service handles this.
            // This resolver focuses on dimension/grouping fields.
            case "etc" -> PG.ETC_UID;
            case "de" -> PG.DE_UID;
            default -> {
                // For forward compatibility, try a direct lookup on the value
                log.warn("Unknown namespace '{}' in field ID '{}'. Falling back to direct lookup.", field.namespace(), standardizedId);
                yield resolveCoreField(field.value());
            }
        };
    }

    /**
     * Maps a core field name to its jOOQ representation.
     * This reuses the logic from your original PivotFieldJooqMapper.
     */
    private Field<?> resolveCoreField(String fieldName) {
        // This switch is now an internal implementation detail of the resolver.
        // It's the exact same logic as previous PivotFieldJooqMapper.
        return switch (fieldName) {
            // Measures / values
            case "value_num" -> PG.VALUE_NUM;
            case "value_text" -> PG.VALUE_TEXT;
            case "value_bool" -> PG.VALUE_BOOL;
            case "value_ts" -> PG.VALUE_TS;
            case "value_ref_uid" -> PG.VALUE_REF_UID;

            // Option handling
            case "option_uid" -> PG.OPTION_UID;
            case "option_value_uid" -> PG.OPTION_VALUE_UID;
            case "option_name" -> PG.OPTION_NAME;
            case "option_code" -> {
                // will migrate to uid; keep mapping for now if present
                yield PG.OPTION_CODE;
            }

            // De / element
            case "de_uid" -> PG.DE_UID;
            case "de_name" -> PG.DE_NAME;
            case "de_value_type" -> PG.DE_VALUE_TYPE;
            case "de_option_set_uid" -> PG.DE_OPTION_SET_UID;

            // etc template uids
            case "etc_uid" -> PG.ETC_UID;
            case "template_repeat_path" -> PG.TEMPLATE_REPEAT_PATH;
            case "template_id_path" -> PG.TEMPLATE_ID_PATH;
            case "template_name_path" -> PG.TEMPLATE_NAME_PATH;
            case "display_label" -> PG.DISPLAY_LABEL;

            // Context / Dimensions
            case "team_uid" -> PG.TEAM_UID;
            case "team_code" -> PG.TEAM_CODE;
            case "org_unit_uid" -> PG.ORG_UNIT_UID;
            case "org_unit_name" -> PG.ORG_UNIT_NAME;
            case "org_unit_cod" -> PG.ORG_UNIT_CODE;
            case "activity_uid" -> PG.ACTIVITY_UID;
            case "activity_name" -> PG.ACTIVITY_NAME;
            case "assignment_uid" -> PG.ASSIGNMENT_UID;
            case "submission_uid" -> PG.SUBMISSION_UID;

            // Repeat / category uids
            case "child_category_uid" -> PG.CHILD_CATEGORY_UID;
            case "parent_category_uid" -> PG.PARENT_CATEGORY_UID;
            case "child_category_kind" -> PG.CHILD_CATEGORY_KIND;
            case "parent_category_kind" -> PG.PARENT_CATEGORY_KIND;

            // Repeat instance ids (we keep ULIDs here)
            case "repeat_instance_id" -> PG.REPEAT_INSTANCE_ID;
            case "parent_repeat_instance_id" -> PG.PARENT_REPEAT_INSTANCE_ID;
            case "repeat_path" -> PG.REPEAT_PATH;
            case "repeat_section_label" -> PG.REPEAT_SECTION_LABEL;

            // submission time (timestamp)
            case "submission_completed_at" -> PG.SUBMISSION_COMPLETED_AT;

            // fallback to other known columns present in MV
            case "value_id" -> PG.VALUE_ID;
            case "deleted_at" -> PG.DELETED_AT;

            // Default fallback: create a typed field with Object.class
            default -> {
                log.debug("Unknown pivot field '{}', falling back to untyped DSL.field(...)", fieldName);
                yield DSL.field(DSL.name(fieldName), Object.class);
            }
        };
    }
}
