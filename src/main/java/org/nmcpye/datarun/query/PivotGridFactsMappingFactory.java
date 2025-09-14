//package org.nmcpye.datarun.query;
//
//import lombok.RequiredArgsConstructor;
//import org.jooq.Condition;
//import org.jooq.Field;
//import org.nmcpye.datarun.analytics.dto.Aggregation;
//import org.nmcpye.datarun.analytics.dto.MeasureRequest;
//import org.nmcpye.datarun.analytics.dto.QueryableElement;
//import org.nmcpye.datarun.analytics.dto.QueryableElementMapping;
//import org.nmcpye.datarun.analytics.metadata.MetadataResolver;
//import org.springframework.stereotype.Service;
//
//import static org.nmcpye.datarun.jooq.Tables.PIVOT_GRID_FACTS;
//
///**
// * PivotGridFactsMappingFactory - default implementation for PIVOT_GRID_FACTS.
// * This contains the existing mapping logic extracted from MeasureValidationServiceImpl.
// * NOTE: This file intentionally refers to codegen class PIVOT_GRID_FACTS; adjust
// *
// * @author Hamza Assada
// * @since 14/09/2025
// */
//@Service
//@RequiredArgsConstructor
//public class PivotGridFactsMappingFactory implements QueryableElementMappingFactory {
//    private final QueryTarget target;
//    private final MetadataResolver metadataResolver;
//
//    @Override
//    public QueryableElementMapping build(MeasureRequest mreq, MetadataResolver resolver) {
//        // Example mapping logic skeleton. Replace with the exact logic you currently have.
//
//        // 1) resolve client field id to deUid / etcUid
//        var resolved = resolver.resolveFieldById(mreq.getFieldId()).orElseThrow();
//        String deUid = resolved.deUid();
//        String etcUid = resolved.etcUid();
//
//        // 2) pick the correct jOOQ Field<?> from PIVOT_GRID_FACTS for this measure
//        Field<?> targetField;
//        switch (resolved.fieldType()) {
//            case NUMERIC:
//                targetField = PIVOT_GRID_FACTS.VALUE_NUM;
//                break;
//            case TEXT:
//                targetField = PIVOT_GRID_FACTS.VALUE_TEXT;
//                break;
//            case TIMESTAMP:
//                targetField = PIVOT_GRID_FACTS.VALUE_TS;
//                break;
//            case BOOLEAN:
//                targetField = PIVOT_GRID_FACTS.VALUE_BOOL;
//                break;
//            case OPTION:
//                targetField = PIVOT_GRID_FACTS.OPTION_UID;
//                break;
//            default:
//                throw new IllegalArgumentException("Unsupported field type: " + resolved.fieldType());
//        }
//
//        // 3) build element predicate (scoping)
//        Condition elementPredicate = null;
//        if (etcUid != null) {
//            elementPredicate = PIVOT_GRID_FACTS.ETC_UID.eq(etcUid);
//        } else if (deUid != null) {
//            elementPredicate = PIVOT_GRID_FACTS.DE_UID.eq(deUid);
//        }
//
//        // 4) handle optionId scoping
//        String optionUid = mreq.getOptionId();
//        if (optionUid != null && !optionUid.isBlank()) {
//            // ensure predicate includes option scope
//            Condition optionCond = PIVOT_GRID_FACTS.OPTION_UID.eq(optionUid);
//            elementPredicate = elementPredicate == null ? optionCond : elementPredicate.and(optionCond);
//        }
//
//        // 5) derive aggregation enum and alias
//        var aggregation = Aggregation.valueOf(mreq.getAggregation());
//        boolean distinct = Boolean.TRUE.equals(mreq.getDistinct());
//        String alias = (mreq.getAlias() == null || mreq.getAlias().isBlank())
//            ? generateStableAlias(mreq, resolved)
//            : mreq.getAlias();
//
//        return QueryableElementMapping.builder()
//            .deUid(deUid)
//            .etcUid(etcUid)
//            .aggregation(aggregation)
//            .targetField(targetField)
//            .elementPredicate(elementPredicate)
//            .alias(alias)
//            .distinct(distinct)
//            .optionUid(optionUid)
//            .build();
//    }
//
//    private String generateStableAlias(MeasureRequest req, QueryableElement resolved) {
//        // Implement deterministic alias construction to preserve previous behavior
//        // Example: <aggregation>__<fieldId or deUid or etcUid>
//        String base = req.getAggregation().toLowerCase() + "__" + (resolved.etcUid() != null ? resolved.etcUid() : resolved.deUid());
//        return base.replace(':', '_');
//    }
//}
