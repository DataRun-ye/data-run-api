package org.nmcpye.datarun.analytics.pivot;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.jooq.Tables;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.nmcpye.datarun.jooq.tables.PivotGridFacts.PIVOT_GRID_FACTS;

/**
 * Maps pivot field dataType or factColumn names to typed jOOQ Fields from the generated
 * {@code PIVOT_GRID_FACTS} table. This mapper is UID-native and understands the new
 * *_uid columns exposed by the pivot materialized view.
 * <p>
 * The mapper returns the generated typed Field when possible (preferred).
 * When an unknown column name is provided, it falls back to {@code DSL.field(name, Object.class)}.
 */
@Component
public class PivotFieldJooqMapper {

    private static final Logger log = LoggerFactory.getLogger(PivotFieldJooqMapper.class);
    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;

    /**
     * Map a dataType/factColumn name to the appropriate jOOQ Field.
     * <p>
     * dataType examples handled:
     * - "value_num"             -> PIVOT_GRID_FACTS.VALUE_NUM (BigDecimal)
     * - "value_text"            -> PIVOT_GRID_FACTS.VALUE_TEXT (String)
     * - "value_bool"            -> PIVOT_GRID_FACTS.VALUE_BOOL (Boolean)
     * - "value_ts"              -> PIVOT_GRID_FACTS.VALUE_TS (LocalDateTime)
     * - "value_ref_uid"         -> PIVOT_GRID_FACTS.VALUE_REF_UID (String - UID)
     * - "option_uid"            -> PIVOT_GRID_FACTS.OPTION_UID (String - UID for multi-select rows)
     * - "option_value_uid"      -> PIVOT_GRID_FACTS.OPTION_VALUE_UID (String)
     * - "de_uid"                -> PIVOT_GRID_FACTS.DE_UID (String)
     * - "etc_uid" -> PIVOT_GRID_FACTS.ETC_UID (String)
     * - "team_uid","org_unit_uid","activity_uid","assignment_uid","submission_uid"
     * - "child_category_uid","parent_category_uid"
     * - "submission_completed_at"
     * <p>
     * Unknown names: fallback to DSL.field(name, Object.class).
     *
     * @param dataTypeOrFactColumn a pivot dataType or fact column name
     * @return jOOQ Field<?> typed where possible
     */
    @SuppressWarnings("unchecked")
    public Field<?> toJooqField(String dataTypeOrFactColumn) {
        if (dataTypeOrFactColumn == null) {
            return PIVOT_GRID_FACTS.VALUE_TEXT;
        }

        String key = dataTypeOrFactColumn.trim();

        return switch (key) {
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

            // De / element & template uids
            case "de_uid" -> PG.DE_UID;
            case "de_name" -> PG.DE_NAME;
            case "de_value_type" -> PG.DE_VALUE_TYPE;
            case "de_option_set_uid" -> PG.DE_OPTION_SET_UID;

            case "etc_uid" -> PG.ETC_UID;
            case "template_repeat_path" -> PG.TEMPLATE_REPEAT_PATH;
            case "template_id_path" -> PG.TEMPLATE_ID_PATH;
            case "template_name_path" -> PG.TEMPLATE_NAME_PATH;
            case "display_label" -> PG.DISPLAY_LABEL;

            // Context / Dimensions (UIDs)
            case "team_uid" -> PG.TEAM_UID;
            case "org_unit_uid" -> PG.ORG_UNIT_UID;
            case "activity_uid" -> PG.ACTIVITY_UID;
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
                log.debug("Unknown pivot field '{}', falling back to untyped DSL.field(...)", key);
                yield DSL.field(DSL.name(key), Object.class);
            }
        };
    }

    /**
     * Convenient helper when caller has a PivotFieldDto (metadata) and wants the proper jOOQ field.
     * The DTO may expose a dedicated factColumn (preferred) or its dataType; prefer factColumn if present.
     *
     * @param dto pivot metadata describing the field
     * @return the jOOQ Field<?> for use in queries
     */
    public Field<?> toJooqFieldForPivotField(org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto dto) {
        if (dto == null) return PG.VALUE_TEXT;
        // prefer explicit factColumn (if provided by metadata), otherwise dataType
        String fact = dto.factColumn() != null && !dto.factColumn().isBlank() ? dto.factColumn() : dto.dataType();
        return toJooqField(fact);
    }
}
