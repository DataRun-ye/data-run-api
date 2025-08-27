package org.nmcpye.datarun.analytics.pivot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.analytics.pivot.dto.MeasureRequest;
import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.analytics.pivot.exception.InvalidMeasureException;
import org.nmcpye.datarun.analytics.pivot.util.AliasSanitizer;
import org.nmcpye.datarun.jooq.Tables;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
import org.nmcpye.datarun.jpa.dataelement.service.DataElementService;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Template-mode-first MeasureValidationService.
 * <p>
 * Notes:
 * - Prefers template-scoped filter using element_template_config_id if the metadata DTO corresponds to that template.
 * - Falls back to global element_id matching if needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureValidationServiceImpl implements MeasureValidationService {

    private final PivotMetadataService pivotMetadataService;
    private final PivotFieldJooqMapper fieldMapper;
    private final ElementTemplateConfigRepository etcRepository;
    private final DataElementService dataElementService;
    private final DSLContext dsl;

    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;

    @Override
    public ValidatedMeasure validate(MeasureRequest req, String templateId, String templateVersionId) throws InvalidMeasureException {
        if (req == null) throw new InvalidMeasureException("MeasureRequest is required");
        if (req.getElementIdOrUid() == null) throw new InvalidMeasureException("elementIdOrUid is required");

        // 1) Lookup metadata in template context
        Optional<PivotFieldDto> maybe = pivotMetadataService.resolveFieldByUidOrId(req.getElementIdOrUid(), templateId, templateVersionId);

        PivotFieldDto dto = null;
        String effectiveMode = "TEMPLATE";
        if (maybe.isPresent()) {
            dto = maybe.get();
        } else {
            // Fallback: maybe the client passed a data element id/uid referencing global data_element.
            // Try to resolve via DataElementRepository
            var deOpt = dataElementService.findByIdOrUid(req.getElementIdOrUid());
            if (deOpt.isPresent()) {
                // Build a simple PivotFieldDto from global DE info
                var de = deOpt.get();
                dto = PivotFieldDto.builder()
                    .id("de:" + de.getId())
                    .label(de.getName())
                    .category("FORM_MEASURE")
                    .dataType(mapValueTypeToDataType(de.getType()))
                    .aggregationModes(null) // metadata service not used here; we can compute allowed aggr outside
                    .templateModeOnly(false)
                    .source("data_element")
                    .extras(null)
                    .build();
                effectiveMode = "GLOBAL";
            } else {
                throw new InvalidMeasureException("Element not found in template or global catalog: " + req.getElementIdOrUid());
            }
        }

        // 2) Validate aggregation
        String requestedAgg = req.getAggregation();
        if (requestedAgg == null) {
            // default (Maybe throw?)
            requestedAgg = "COUNT";
        }
        requestedAgg = requestedAgg.toUpperCase(Locale.ROOT);
        // check allowed aggregation list if present
        if (dto.aggregationModes() != null && !dto.aggregationModes().isEmpty()) {
            if (!dto.aggregationModes().contains(requestedAgg)) {
                throw new InvalidMeasureException("Aggregation " + requestedAgg + " not allowed for field " + req.getElementIdOrUid());
            }
        }

        // 3) Build element predicate — prefer template id if dto indicates etc:...
        Condition elementPredicate = DSL.noCondition();
        Long etcId = null;
        String elementId = null;

        // If DTO id uses the "etc:" prefix or matches etc numeric id, prefer template filter
        String dtoId = dto.id();
        if (dtoId != null && dtoId.startsWith("etc:")) {
            String numeric = dtoId.substring("etc:".length());
            try {
                etcId = Long.parseLong(numeric);
                elementPredicate = PG.ELEMENT_CONFIG_ID.eq(etcId);
            } catch (NumberFormatException nfe) {
                // ignore
            }
        } else {
            // maybe dto came from data element fallback or uses dataElement id
            // pivot metadata in template mode might still have element_template_config_id unknown; we do best-effort by element_id
            // If dto.extras contains elementTemplateConfig id (not guaranteed), attempt to use it — otherwise fallback to element_id eq
            // Check extras for etc id (caller previously put categoryForRepeat etc); not reliable — fallback to element_id
            if (dto.id() != null && dto.id().startsWith("de:")) {
                elementId = dto.id().substring("de:".length());
            } else if (req.getElementIdOrUid().startsWith("de:")) {
                elementId = req.getElementIdOrUid().substring("de:".length());
            } else {
                // last-resort: treat incoming as element id (ULID)
                elementId = req.getElementIdOrUid();
            }
            elementPredicate = PG.ELEMENT_ID.eq(elementId);
        }

        // If the request also specified an optionId (for multi-select) include it in predicate
        String optionId = req.getOptionId();
        if (optionId != null) {
            elementPredicate = elementPredicate.and(PG.OPTION_ID.eq(optionId));
        }

        // 4) Determine targetField with correct typing using fieldMapper
        Field<?> baseField = fieldMapper.toJooqField(dto.dataType());
        Field<?> targetField;
        ValidatedMeasure.MeasureAggregation aggEnum;
        try {
            aggEnum = ValidatedMeasure.MeasureAggregation.valueOf(requestedAgg);
        } catch (IllegalArgumentException iae) {
            throw new InvalidMeasureException("Unsupported aggregation: " + requestedAgg);
        }

        switch (aggEnum) {
            case SUM, AVG -> {
                if (!"value_num".equals(dto.dataType())) {
                    throw new InvalidMeasureException("Aggregation " + requestedAgg + " requires numeric target (value_num)");
                }
                // typed cast — safe because mapper returned PG.VALUE_NUM (Field<BigDecimal>)
                targetField = PG.VALUE_NUM;
            }
            case MIN, MAX -> {
                // allow MIN/MAX on numeric or timestamp or text (lexicographic)
                if ("value_num".equals(dto.dataType())) {
                    targetField = PG.VALUE_NUM;
                } else if ("value_ts".equals(dto.dataType()) || "submission_completed_at".equals(dto.dataType())) {
                    targetField = PG.VALUE_TS;
                } else {
                    // fallback to text
                    targetField = PG.VALUE_TEXT;
                }
            }
            case SUM_TRUE -> {
                // build CASE WHEN value_bool THEN 1 ELSE 0 END as integer; jOOQ: DSL.when(...).otherwise(...)
                Field<Integer> caseInt = DSL.when(PG.VALUE_BOOL.eq(true), DSL.inline(1)).otherwise(0).cast(Integer.class);
                targetField = caseInt;
            }
            case COUNT -> {
                // For count, the "targetField" is usually the row (count(*)), but for COUNT_DISTINCT we need the field to distinct on.
                // We'll set targetField to option_id or value_text depending on dataType
                if ("option_id".equals(dto.dataType())) targetField = PG.OPTION_ID;
                else if ("value_num".equals(dto.dataType())) targetField = PG.VALUE_NUM;
                else if ("value_ts".equals(dto.dataType())) targetField = PG.VALUE_TS;
                else targetField = PG.VALUE_TEXT;
            }
            case COUNT_DISTINCT -> {
                if ("option_id".equals(dto.dataType())) targetField = PG.OPTION_ID;
                else if ("value_num".equals(dto.dataType())) targetField = PG.VALUE_NUM;
                else if ("value_ts".equals(dto.dataType())) targetField = PG.VALUE_TS;
                else targetField = PG.VALUE_TEXT;
            }
            default -> {
                targetField = PG.VALUE_TEXT;
            }
        }

        // 5) alias fallback
        String alias = req.getAlias();
        if (alias == null || alias.isBlank()) {
            alias = (dto.id() != null ? dto.id().replaceAll("[:]", "_") : "measure");
            alias = alias + "_" + requestedAgg.toLowerCase(Locale.ROOT);
        }

        // SANITIZE alias to produce safe SQL column name / jOOQ alias
        alias = AliasSanitizer.sanitize(alias);

        return new ValidatedMeasure(
            elementId,
            etcId,
            aggEnum,
            targetField,
            elementPredicate,
            alias,
            Boolean.TRUE.equals(req.getDistinct()),
            optionId,
            effectiveMode
        );
    }

    private static String mapValueTypeToDataType(org.nmcpye.datarun.datatemplateelement.enumeration.ValueType vt) {
        if (vt == null) return "value_text";
        return switch (vt) {
            case Number, Integer, IntegerPositive, IntegerNegative, IntegerZeroOrPositive, Percentage, UnitInterval ->
                "value_num";
            case Boolean -> "value_bool";
            case SelectOne, SelectMulti -> "option_id";
            case Date, DateTime -> "value_ts";
            default -> "value_text";
        };
    }
}
