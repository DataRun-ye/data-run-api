package org.nmcpye.datarun.analytics.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.analytics.QueryJooqMapper;
import org.nmcpye.datarun.analytics.dto.*;
import org.nmcpye.datarun.analytics.exception.InvalidMeasureException;
import org.nmcpye.datarun.analytics.fieldresolver.MappedQueryableElement;
import org.nmcpye.datarun.jooq.Tables;
import org.nmcpye.datarun.jooq.tables.PivotGridFacts;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureValidationServiceImpl implements MeasureValidationService {
    private final MetadataService metadataService;

    private static final PivotGridFacts PG = Tables.PIVOT_GRID_FACTS;

    @Override
    public QueryableElementMapping validate(MeasureRequest req, String templateUid,
                                            String templateVersionUid) throws InvalidMeasureException {
        if (req == null) throw new InvalidMeasureException("MeasureRequest is required");
        if (req.getFieldId() == null) throw new InvalidMeasureException("fieldId is required");

        Map<String, QueryableElement> fieldMap = new ConcurrentHashMap<>(metadataService
                .getMetadataForTemplate(templateUid, templateVersionUid)
                .getAvailableFields().stream()
                .collect(Collectors.toMap(QueryableElement::id, f -> f)));


        // Resolve metadata (prefer template lookup)
        // STEP 1: Resolve the complete, authoritative metadata for the field.
        QueryableElement fieldDto = Optional.ofNullable(fieldMap.get(req.getFieldId()))
                .orElseThrow(() -> new InvalidMeasureException("Field not found: " + req.getFieldId()));

        // STEP 2: Validate the requested aggregation against the field's allowed modes.
        Aggregation aggEnum = validateAggregation(req, fieldDto);

        // STEP 3: Build the element predicate. This is now simple and declarative,
        // using the authoritative `sourceColumn` from our metadata contract.
        MappedQueryableElement parsedId = MappedQueryableElement.from(fieldDto.id());
        Condition condition = DSL.field(DSL.name(fieldDto.sourceColumn()), String.class).eq(parsedId.value());

        // Handle option-scoping for multi-selects
        if (req.getOptionId() != null) {
            condition = condition.and(PG.OPTION_UID.eq(req.getOptionId()));
        }

        //Determine the correct target value field (e.g., value_num) for the aggregate function.
        Field<?> targetField = determineAggregationColumn(aggEnum, fieldDto);

        String alias = getAliasOrFallback(req, fieldDto, aggEnum);

        return QueryableElementMapping.builder()
                .deUid(fieldDto.deUid())
                .etcUid("etc".equals(parsedId.namespace()) ? parsedId.value() : null)
                .aggregation(aggEnum)
                .targetField(targetField)
                .elementPredicate(condition)
                .alias(alias)
                .distinct(Boolean.TRUE.equals(req.getDistinct()))
                .optionUid(req.getOptionId())
                .build();
    }

    private Aggregation validateAggregation(MeasureRequest req, QueryableElement dto) {
        String requestedAgg = Optional.ofNullable(req.getAggregation()).orElse("COUNT").toUpperCase(Locale.ROOT);
        Aggregation aggEnum;
        try {
            aggEnum = Aggregation.valueOf(requestedAgg);
        } catch (IllegalArgumentException e) {
            throw new InvalidMeasureException("Unsupported aggregation: " + requestedAgg);
        }

        if (dto.aggregationModes() == null || !dto.aggregationModes().contains(aggEnum)) {
            throw new InvalidMeasureException(String.format(
                    "Aggregation %s not allowed for field %s. Allowed: %s",
                    requestedAgg, dto.id(), dto.aggregationModes()
            ));
        }
        return aggEnum;
    }

    /**
     * Determine the correct target value field (e.g., value_num) for the aggregate function
     *
     * @param agg aggregation type
     * @param dto queryable metadata element
     * @return target db field
     */
    private Field<?> determineAggregationColumn(Aggregation agg, QueryableElement dto) {
        return switch (agg) {
            case SUM, AVG -> {
                if (!dto.dataType().equals(DataType.NUMERIC))
                    throw new InvalidMeasureException("SUM/AVG requires a NUMERIC field.");
                yield PG.VALUE_NUM;
            }
            case SUM_TRUE -> {
                if (!dto.dataType().equals(DataType.BOOLEAN))
                    throw new InvalidMeasureException("SUM_TRUE requires a BOOLEAN field.");
                yield DSL.when(PG.VALUE_BOOL.isTrue(), 1).otherwise(0);
            }
            // For MIN/MAX, the target depends on the dataType
            case MIN, MAX -> switch (dto.dataType()) {
                case NUMERIC -> PG.VALUE_NUM;
                case TIMESTAMP -> PG.VALUE_TS;
                default -> PG.VALUE_TEXT; // Default for text, refs, etc.
            };
            // For COUNT, we count the specific value column to correctly handle NULLs.
            case COUNT, COUNT_DISTINCT -> QueryJooqMapper.toJooqField(dto.sourceColumn());
        };
    }

    private String getAliasOrFallback(MeasureRequest req, QueryableElement dto, Aggregation agg) {
        if (req.getAlias() != null && !req.getAlias().isBlank()) {
            return req.getAlias();
        }

        // Fallback to a generated alias
        return String.format("%s_%s",
                MappedQueryableElement.from(dto.id()).value(),
                agg.name().toLowerCase(Locale.ROOT));
    }
}
