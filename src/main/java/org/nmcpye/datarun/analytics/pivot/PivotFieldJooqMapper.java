package org.nmcpye.datarun.analytics.pivot;

import org.jooq.Field;
import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.jooq.Tables;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Small mapping utility that returns a typed jOOQ Field for a PivotFieldDto.
 * The PivotMetadataService returns DTOs without jOOQ types; the query builder / measure validation
 * should call this to obtain the properly typed Field<T>.
 * <p>
 * This file has strong typing to avoid ambiguous comparators in jOOQ later.
 */
@Component
public class PivotFieldJooqMapper {

    private static final Logger log = LoggerFactory.getLogger(PivotFieldJooqMapper.class);
    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;

    /**
     * Map a dataType name to the jOOQ Field with proper Java type.
     * <p>
     * dataType examples:
     * - "value_num"  -> Field<BigDecimal>
     * - "value_bool" -> Field<Boolean>
     * - "option_id" / "value_text" / "value_ref" -> Field<String>
     * - "value_ts"   -> Field<LocalDateTime>
     */
    @SuppressWarnings("unchecked")
    public Field<?> toJooqField(String dataType) {
        if (dataType == null) {
            // default to text
            return PG.VALUE_TEXT;
        }
        return switch (dataType) {
            case "value_num" -> PG.VALUE_NUM; // Field<BigDecimal>
            case "value_bool" -> PG.VALUE_BOOL; // Field<Boolean>
            case "option_id" -> PG.OPTION_ID; // Field<String>
            case "value_text" -> PG.VALUE_TEXT; // Field<String>
            case "value_ref" -> PG.VALUE_REF; // assume String (entity ULID)
            case "value_ts" -> PG.VALUE_TS; // Field<LocalDateTime>
            case "submission_completed_at" -> PG.SUBMISSION_COMPLETED_AT; // LocalDateTime
            case "team_id" -> PG.TEAM_ID; // String
            case "org_unit_id" -> PG.ORG_UNIT_ID; // String
            case "activity_id" -> PG.ACTIVITY_ID; // String
            case "element_id" -> PG.ELEMENT_ID; // String
            default -> {
                log.debug("Unknown dataType '{}', falling back to VALUE_TEXT", dataType);
                yield PG.VALUE_TEXT;
            }
        };
    }

    /**
     * Helper to pick the column to use when translating a requested field (dataType may be null).
     */
    public Field<?> toJooqFieldForPivotField(PivotFieldDto dto) {
        return toJooqField(dto.dataType());
    }
}
