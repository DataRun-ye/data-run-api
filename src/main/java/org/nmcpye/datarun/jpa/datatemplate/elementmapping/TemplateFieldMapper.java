package org.nmcpye.datarun.jpa.datatemplate.elementmapping;

import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;

import java.util.Objects;

/**
 * Small helper to convert FormDataElementConf +
 * DataElement metadata into TemplateField DTO.
 *
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public final class TemplateFieldMapper {

    private TemplateFieldMapper() {
    }

    private static AggregationType getDefaultAggregationType(ValueType valueType) {
        return switch (valueType) {
            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> AggregationType.SUM;
            default -> AggregationType.NONE;
        };
    }

    public static ElementTemplateConfig from(
        String templateId,
        String versionId,
        int versionNo,
        AbstractElement conf,
        DataElementMeta meta,
        String repeatPath,
        String categoryForRepeatElementId) {

        Objects.requireNonNull(conf);
        Objects.requireNonNull(meta);
        var tf = ElementTemplateConfig.builder();
        tf.templateId(templateId);
        tf.templateVersionId(versionId);
        tf.versionNo(versionNo);
        tf.dataElementId(meta.elementId());
        tf.idPath(conf.getPath());
        tf.name(conf.getName());
        tf.repeatPath(repeatPath);
        tf.categoryForRepeat(categoryForRepeatElementId);
        if (conf instanceof FormDataElementConf field) {
            tf.elementKind(ElementTemplateConfig.ElementKind.FIELD);
            tf.valueType(meta.valueType());
            tf.namePath(field.getPath().replaceFirst(field.getId(), field.getName()));
            // get it from passed category
            // conf may override meta
            tf.isReference(meta.isReference());
            tf.referenceTable(meta.referenceTable());
            tf.optionSetId(field.getOptionSet()); // conf may override meta
            tf.isMulti(Boolean.TRUE.equals(field.isMultiSelect())); // adapt to your properties structure
            tf.aggregationType(field.getAggregationType().isDefault() ?
                getDefaultAggregationType(field.getType()) :
                field.getAggregationType());
            // Create a canonical metadata layer describing:
            //available measures (aggregate-able fields: count, sum, avg, min, max),
            //available dimensions (attributes to group by),
            tf.isMeasure(field.getIsMeasure());
        } else if (conf instanceof FormSectionConf sectionConf) {
            tf.elementKind(ElementTemplateConfig.ElementKind.SECTION);
            // conf may override meta
            tf.isRepeatable(sectionConf.getRepeatable());
        }

        // serialize displayLabel and full conf as JSON strings for storage; caller may set via ObjectMapper
        // caller should set displayLabelJson / definitionJson before inserting
        return tf.build();
    }
}
