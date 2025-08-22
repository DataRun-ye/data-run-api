package org.nmcpye.datarun.jpa.datatemplate.elementmapping;

import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplate;

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

    public static ElementTemplate from(
        String templateId,
        String versionId,
        int versionNo,
        FormDataElementConf conf,
        DataElementMeta meta,
        String repeatPath,
        String categoryForRepeatElementId) {

        Objects.requireNonNull(conf);
        Objects.requireNonNull(meta);

        ElementTemplate tf = new ElementTemplate();
        tf.setTemplateId(templateId);
        tf.setTemplateVersionId(versionId);
        tf.setVersionNo(versionNo);
        tf.setDataElementId(meta.elementId());
        tf.setPath(conf.getPath());
        tf.setName(conf.getName());
        tf.setValueType(meta.valueType());
        tf.setIsReference(meta.isReference());
        tf.setReferenceTable(meta.referenceTable());
        tf.setOptionSetId(conf.getOptionSet()); // conf may override meta
        tf.setIsRepeatable(repeatPath != null);
        tf.setRepeatPath(repeatPath);
        tf.setIsMulti(Boolean.TRUE.equals(conf.isMultiSelect())); // adapt to your properties structure
        // Create a canonical metadata layer describing:
        //available measures (aggregate-able fields: count, sum, avg, min, max),
        //available dimensions (attributes to group by),
        tf.setIsMeasure(conf.isMultiSelect());
        tf.setAggregationType(conf.getAggregationType().isDefault() ?
            getDefaultAggregationType(conf.getType()) :
            conf.getAggregationType());
        tf.setCategoryForRepeat(categoryForRepeatElementId);
        // serialize displayLabel and full conf as JSON strings for storage; caller may set via ObjectMapper
        // caller should set displayLabelJson / definitionJson before inserting
        return tf;
    }
}
