//package org.nmcpye.datarun.jpa.datatemplate.elementmapping;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.datatemplateelement.AbstractElement;
//import org.nmcpye.datarun.datatemplateelement.AggregationType;
//import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
//import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
//import org.springframework.stereotype.Service;
//
///**
// * Small helper to convert FormDataElementConf +
// * DataElement metadata into TemplateField DTO.
// *
// * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public final class TemplateFieldMapper {
//    private final ObjectMapper objectMapper;
//
//
//    private static AggregationType getDefaultAggregationType(ValueType valueType) {
//        return switch (valueType) {
//            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
//                 IntegerNegative, IntegerZeroOrPositive -> AggregationType.SUM;
//            case Boolean, TrueOnly -> AggregationType.SUM_TRUE;
//            default -> AggregationType.COUNT;
//        };
//    }
//
//    public TemplateElement from(
//        String templateUid,
//        String templateVersionUid,
//        int templateVersionNo,
//        AbstractElement conf,
//        DataElementMeta meta,
//        String repeatPath,
//        String categoryForRepeatElementUid,
//        Boolean isCategory) {
//        TemplateElement.ElementTemplateConfigBuilder builder = TemplateElement.builder()
//            .templateUid(templateUid)
//            .templateVersionUid(templateVersionUid)
//            .versionNo(templateVersionNo)
//            .dataElementUid(meta.elementUid())
//            .idPath(conf.getPath())
//            .sortOrder(conf.getOrder())
//            .name(conf.getName())
//            .ancestorRepeatPath(repeatPath)
////            .isCategory(Boolean.TRUE.equals(isCategory))
////            .categoryForRepeat(categoryForRepeatElementUid)
//            .isReference(meta.isReference())
////            .ref(meta.referenceTable())
//            .optionSetUid(conf instanceof FormDataElementConf f ? f.getOptionSet() : null)
//            .hasRepeatAncestor(conf instanceof FormSectionConf s ? Boolean.TRUE.equals(s.getRepeatable()) : Boolean.FALSE)
//            .elementKind(conf instanceof FormDataElementConf ? TemplateElement.ElementKind.FIELD : TemplateElement.ElementKind.REPEAT);
//
//        if (conf instanceof FormDataElementConf field) {
//            builder.elementKind(TemplateElement.ElementKind.FIELD);
//            builder.valueType(meta.valueType());
//            builder.namePath(field.getPath().replaceFirst(field.getId(), field.getName()));
//            builder.isMulti(Boolean.TRUE.equals(field.isMultiSelect()));
//            builder.isMeasure(Boolean.TRUE.equals(field.getIsMeasure()));
//            builder.isDimension(Boolean.TRUE.equals(field.getIsDimension()));
//            builder.aggregationType(/*field.getAggregationType() == null ? */getDefaultAggregationType(field.getType())/* : field.getAggregationType()*/);
//        } else {
//            // section defaults
//            builder.elementKind(TemplateElement.ElementKind.REPEAT);
//            builder.isMulti(Boolean.FALSE);
//            builder.isMeasure(Boolean.FALSE);
//            builder.aggregationType(AggregationType.DEFAULT);
//        }
//
//        // store the conf object as JSON in definition_json (use ObjectMapper to produce structure)
//        try {
//            builder.definitionJson(objectMapper.convertValue(conf, Object.class));
//        } catch (Exception ex) {
//            log.error("error mapping element definition json: (template:{}:{}), (path:{})", templateUid, templateVersionUid, conf.getPath());
//            builder.definitionJson(null);
//        }
//
//        return builder.build();
//    }
//}
