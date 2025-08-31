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
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jooq.Tables;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.utils.UidValidator;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureValidationServiceImpl implements MeasureValidationService {

    private final PivotMetadataService pivotMetadataService;
    private final PivotFieldJooqMapper fieldMapper;
    private final DSLContext dsl;
    private final DataElementRepository dataElementRepository;

    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;

    @Override
    public ValidatedMeasure validate(MeasureRequest req, String templateUid, String templateVersionUid) throws InvalidMeasureException {
        if (req == null) throw new InvalidMeasureException("MeasureRequest is required");
        if (req.getElementIdOrUid() == null) throw new InvalidMeasureException("elementIdOrUid is required");

        String elementSpecifier = req.getElementIdOrUid();

        // Resolve metadata (prefer template lookup)
        Optional<PivotFieldDto> maybe = pivotMetadataService.resolveFieldByUidOrId(elementSpecifier, templateUid, templateVersionUid);
        PivotFieldDto dto = maybe.orElse(null);

        String effectiveMode = "TEMPLATE";
        if (dto == null) {
            // Fallback: maybe the client sent a global data element, try data element repo by UID
            if (elementSpecifier.startsWith("de:")) {
                String deUid = elementSpecifier.substring("de:".length());
                Optional<DataElement> deOpt = dataElementRepository.findByUid(deUid);
                if (deOpt.isPresent()) {
                    DataElement de = deOpt.get();
                    dto = PivotFieldDto.builder()
                        .id("de:" + de.getUid())
                        .label(de.getName())
                        .category("FORM_MEASURE")
                        .dataType(mapValueTypeToDataType(de.getValueType()))
                        .factColumn("de_uid")
                        .aggregationModes(null)
                        .templateModeOnly(false)
                        .source("data_element")
                        .extras(null)
                        .build();
                    effectiveMode = "GLOBAL";
                } else {
                    throw new InvalidMeasureException("Element not found: " + elementSpecifier);
                }
            } else {
                throw new InvalidMeasureException("Element not found in template or global: " + elementSpecifier);
            }
        }

        // Validate aggregation
        String requestedAgg = req.getAggregation();
        if (requestedAgg == null) requestedAgg = "COUNT";
        requestedAgg = requestedAgg.toUpperCase(Locale.ROOT);

        if (dto.aggregationModes() != null && !dto.aggregationModes().isEmpty()) {
            if (!dto.aggregationModes().contains(requestedAgg)) {
                throw new InvalidMeasureException("Aggregation " + requestedAgg + " not allowed for field " + elementSpecifier);
            }
        }

        // Build element predicate using MV UID columns
        Condition elementPredicate = DSL.noCondition();
        String etcUid = null;
        String deUid = null;

        if (dto.id() != null && dto.id().startsWith("etc:")) {
            etcUid = dto.id().substring("etc:".length());
            UidValidator.requireValid(etcUid, "etc_uid");
            elementPredicate = PG.ETC_UID.eq(etcUid);
        } else if (dto.id() != null && dto.id().startsWith("de:")) {
            deUid = dto.id().substring("de:".length());
            UidValidator.requireValid(deUid, "de_uid");
            elementPredicate = PG.DE_UID.eq(deUid);
        } else {
            // If the DTO didn't follow prefixes but dto.factColumn tells us how to scope, use it
            if ("etc_uid".equals(dto.factColumn())) {
                // attempt to treat given spec as etc uid (best-effort)
                etcUid = elementSpecifier.startsWith("etc:") ? elementSpecifier.substring("etc:".length()) : elementSpecifier;
                UidValidator.requireValid(etcUid, "etc_uid");
                elementPredicate = PG.ETC_UID.eq(etcUid);
            } else if ("de_uid".equals(dto.factColumn())) {
                deUid = elementSpecifier.startsWith("de:") ? elementSpecifier.substring("de:".length()) : elementSpecifier;
                UidValidator.requireValid(deUid, "de_uid");
                elementPredicate = PG.DE_UID.eq(deUid);
            } else {
                // Fallback to scoping by element_template_config_uid if ambiguous
                etcUid = elementSpecifier;
                if (UidValidator.isValid(etcUid)) {
                    elementPredicate = PG.ETC_UID.eq(etcUid);
                } else {
                    throw new InvalidMeasureException("Unable to resolve element predicate for: " + elementSpecifier);
                }
            }
        }

        // Option-scoping (multi-select)
        String optionUid = req.getOptionId();
        if (optionUid != null) {
            elementPredicate = elementPredicate.and(PG.OPTION_UID.eq(optionUid));
        }

        // Determine targetField
        Field<?> targetField = fieldMapper.toJooqFieldForPivotField(dto);

        // Validate aggregation-specific constraints and produce final targetField (typed)
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
                targetField = PG.VALUE_NUM;
            }
            case MIN, MAX -> {
                if ("value_num".equals(dto.dataType())) {
                    targetField = PG.VALUE_NUM;
                } else if ("value_ts".equals(dto.dataType()) || "submission_completed_at".equals(dto.dataType())) {
                    targetField = PG.VALUE_TS;
                } else {
                    targetField = PG.VALUE_TEXT;
                }
            }
            case SUM_TRUE -> {
                // SUM(CASE WHEN value_bool THEN 1 ELSE 0 END)
                targetField = DSL.when(PG.VALUE_BOOL.eq(true), DSL.inline(1)).otherwise(0).cast(Integer.class);
            }
            case COUNT, COUNT_DISTINCT -> {
                // targetField remains as mapped (option_uid/value_ref_uid/value_text/value_num/...),
                // but if COUNT without distinct we might count rows instead of field
                // leave targetField as-is: PivotQueryBuilder will choose count() vs countDistinct
            }
            default -> { /* leave as-is */ }
        }

        // Alias fallback
        String alias = req.getAlias();
        if (alias == null || alias.isBlank()) {
            alias = (dto.id() != null ? dto.id().replaceAll("[:]", "_") : "measure");
            alias = alias + "_" + requestedAgg.toLowerCase(Locale.ROOT);
        }

        return new ValidatedMeasure(
            deUid,
            etcUid,
            aggEnum,
            targetField,
            elementPredicate,
            alias,
            Boolean.TRUE.equals(req.getDistinct()),
            optionUid,
            effectiveMode
        );
    }

    private static String mapValueTypeToDataType(ValueType vt) {
        if (vt == null) return "value_text";
        return switch (vt) {
            case Number, Integer, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive, Percentage, UnitInterval -> "value_num";
            case Boolean, TrueOnly -> "value_bool";
            case SelectMulti -> "option_uid";
            case SelectOne, Activity, OrganisationUnit, Team -> "value_ref_uid";
            case Date, DateTime, Time -> "value_ts";
            default -> "value_text";
        };
    }
}
