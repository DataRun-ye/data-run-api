package org.nmcpye.datarun.jpa.datatemplate.elementmapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.springframework.stereotype.Service;

/**
 * Small helper to convert FormDataElementConf +
 * DataElement metadata into TemplateField DTO.
 *
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public final class TemplateFieldMapper {
    private final ObjectMapper objectMapper;


    private static AggregationType getDefaultAggregationType(ValueType valueType) {
        return switch (valueType) {
            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> AggregationType.SUM;
            default -> AggregationType.NONE;
        };
    }

    public ElementTemplateConfig from(
            String templateId,
            String versionId,
            int versionNo,
            AbstractElement conf,
            DataElementMeta meta,
            String repeatPath,
            String categoryForRepeatElementId,
            Boolean isCategory
    ) {
        ElementTemplateConfig.ElementTemplateConfigBuilder builder = ElementTemplateConfig.builder()
                .templateId(templateId)
                .templateVersionId(versionId)
                .versionNo(versionNo)
                .dataElementId(meta.elementId())
                .idPath(conf.getPath())
                .templateOrder(conf.getOrder())
                .name(conf.getName())
                .repeatPath(repeatPath)
                .isCategory(Boolean.TRUE.equals(isCategory))
                .categoryForRepeat(categoryForRepeatElementId)
                .isReference(meta.isReference())
                .referenceTable(meta.referenceTable())
                .optionSetId(conf instanceof FormDataElementConf f ? f.getOptionSet() : null)
                .isRepeatable(conf instanceof FormSectionConf s ? Boolean.TRUE.equals(s.getRepeatable()) : Boolean.FALSE)
                .elementKind(conf instanceof FormDataElementConf ? ElementTemplateConfig.ElementKind.FIELD : ElementTemplateConfig.ElementKind.SECTION);

        if (conf instanceof FormDataElementConf field) {
            builder.elementKind(ElementTemplateConfig.ElementKind.FIELD);
            builder.valueType(meta.valueType());
            builder.namePath(field.getPath().replaceFirst(field.getId(), field.getName()));
            builder.isMulti(Boolean.TRUE.equals(field.isMultiSelect()));
            builder.isMeasure(Boolean.TRUE.equals(field.getIsMeasure()));
            builder.aggregationType(/*field.getAggregationType() == null ? */getDefaultAggregationType(field.getType())/* : field.getAggregationType()*/);
        } else {
            // section defaults
            builder.elementKind(ElementTemplateConfig.ElementKind.SECTION);
            builder.isMulti(Boolean.FALSE);
            builder.isMeasure(Boolean.FALSE);
            builder.aggregationType(AggregationType.DEFAULT);
        }

        // store the conf object as JSON in definition_json (use ObjectMapper to produce structure)
        try {
            builder.definitionJson(objectMapper.convertValue(conf, Object.class));
        } catch (Exception ex) {
            log.error("error mapping element definition json: (template:{}:{}), (path:{})", templateId, versionId, conf.getPath());
            builder.definitionJson(null);
        }

        return builder.build();
//        Objects.requireNonNull(conf);
//        Objects.requireNonNull(meta);
//        var tf = ElementTemplateConfig.builder();
//        tf.templateId(templateId);
//        tf.templateVersionId(versionId);
//        tf.versionNo(versionNo);
//        tf.dataElementId(meta.elementId());
//        tf.idPath(conf.getPath());
//        tf.name(conf.getName());
//        tf.repeatPath(repeatPath);
//        tf.categoryForRepeat(categoryForRepeatElementId);
//        if (conf instanceof FormDataElementConf field) {
//            tf.elementKind(ElementTemplateConfig.ElementKind.FIELD);
//            tf.dataType(meta.dataType());
//            tf.namePath(field.getPath().replaceFirst(field.getId(), field.getName()));
//            // get it from passed category
//            // conf may override meta
//            tf.isReference(meta.isReference());
//            tf.referenceTable(meta.referenceTable());
//            tf.optionSetId(field.getOptionSet()); // conf may override meta
//            tf.isMulti(Boolean.TRUE.equals(field.isMultiSelect())); // adapt to your properties structure
//            tf.aggregationType(field.getAggregationType().isDefault() ?
//                getDefaultAggregationType(field.getType()) :
//                field.getAggregationType());
//            // Create a canonical metadata layer describing:
//            //available measures (aggregate-able fields: count, sum, avg, min, max),
//            //available dimensions (attributes to group by),
//            tf.isMeasure(field.getIsMeasure());
//        } else if (conf instanceof FormSectionConf sectionConf) {
//            tf.elementKind(ElementTemplateConfig.ElementKind.SECTION);
//            // conf may override meta
//            tf.isRepeatable(sectionConf.getRepeatable());
//        }
//
//        // serialize displayLabel and full conf as JSON strings for storage; caller may set via ObjectMapper
//        // caller should set displayLabelJson / definitionJson before inserting
//        return tf.build();
    }
}
