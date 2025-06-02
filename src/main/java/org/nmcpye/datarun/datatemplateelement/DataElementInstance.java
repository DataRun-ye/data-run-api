//package org.nmcpye.datarun.datatemplateelement;
//
//import jakarta.persistence.Column;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.nmcpye.datarun.common.enumeration.ValueTypeRendering;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.dataelement.DataElement;
//import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateTemplateVersion;
//
///**
// * an {@link DataElementInstance} links an {@link DataTemplateTemplateVersion}
// * to an {@link DataElement}
// * and configure it with additional configuration properties
// *
// * @author Hamza Assada (29-05-2025), <7amza.it@gmail.com>
// */
//@Getter
//@Setter
//@NoArgsConstructor
//public class DataElementInstance extends TypeElement {
//    private String id;
//    @NotNull
//    @Column(name = "type")
//    private ValueType type;
//    @Column(name = "calculation")
//    private Boolean readOnly = Boolean.FALSE;
//    private Boolean mandatory = Boolean.FALSE;
//    private Boolean mainField = Boolean.FALSE;
//    private Object defaultValue;
//    private String optionSet;
//    private String choiceFilter;
//    private ElementValidationRule validationRule;
//    private String calculation;
//    private AggregationType aggregationType = AggregationType.DEFAULT;
//    private ReferenceType resourceType;
//    /**
//     * resourceMetadataSchema for ReferenceField type
//     */
//    private String resourceMetadataSchema;
//    private ValueTypeRendering valueTypeRendering = ValueTypeRendering.DEFAULT;
//
//    @Override
//    public DataElementInstance path(String path) {
//        this.setPath(path);
//        return this;
//    }
//
//    public DataElementInstance type(ValueType type) {
//        this.setType(type);
//        return this;
//    }
//}
