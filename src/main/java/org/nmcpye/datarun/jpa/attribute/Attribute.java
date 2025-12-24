//package org.nmcpye.datarun.attribute;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.google.common.base.MoreObjects;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//import lombok.Setter;
//import org.nmcpye.datarun.common.PrimaryKeyObject;
//import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
//import org.nmcpye.datarun.optionset.OptionSet;
//
//import java.util.List;
//import java.util.Objects;
//
//import static java.util.stream.Collectors.toList;
//
//@Entity
//@Table(name = "attribute")
//@Getter
//@Setter
//public class Attribute
//    extends AttributeBase {
//
//    @Column(name = "code", unique = true)
//    private String code;
//
//    @NotNull
//    @Column(name = "name", nullable = false, unique = true)
//    private String name;
//
//    @Column(name = "shortname", unique = true)
//    private String shortName;
//
//    @Column(name = "description")
//    private String description;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "valuetype")
//    private ValueType dataType;
//
//    @Column(name = "mandatory")
//    private boolean mandatory;
//
//    @Column(name = "isunique")
//    private boolean unique;
//
//    @Column(name = "sortorder")
//    private Integer sortOrder;
//
//    @ManyToOne
//    @JsonIgnoreProperties(value = {"options"}, allowSetters = true)
//    @JoinColumn(updatable = false)
//    private OptionSet optionSet;
//
//    public Attribute() {
//
//    }
//
//    public Attribute(String uid) {
//        this.uid = uid;
//    }
//
//    public Attribute(String name, ValueType dataType) {
//        this.name = name;
//        this.dataType = dataType;
//    }
//
//    @Override
//    public int hashCode() {
//        return 31 * super.hashCode() + Objects.hash(dataType, objectTypes,
//            mandatory, unique, optionSet);
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        return this == obj || obj instanceof Attribute && super.equals(obj) && objectEquals((Attribute) obj);
//    }
//
//    private boolean objectEquals(Attribute other) {
//        return Objects.equals(this.dataType, other.dataType)
//            && Objects.equals(this.objectTypes, other.objectTypes)
//            && Objects.equals(this.mandatory, other.mandatory)
//            && Objects.equals(this.unique, other.unique)
//            && Objects.equals(this.optionSet, other.optionSet);
//    }
//
//    @JsonIgnore
//    public boolean isAttribute(ObjectType type) {
//        return objectTypes.contains(type);
//    }
//
//    public void setAttribute(ObjectType type, boolean isAttribute) {
//        if (isAttribute) {
//            objectTypes.add(type);
//        } else {
//            objectTypes.remove(type);
//        }
//    }
//
//    @JsonProperty
//    @Column(name = "data_element_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getDataElementAttribute() {
//        return isAttribute(ObjectType.DATA_ELEMENT);
//    }
//
//    public void setDataElementAttribute(boolean dataElementAttribute) {
//        setAttribute(ObjectType.DATA_ELEMENT, dataElementAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "data_element_group_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getDataElementGroupAttribute() {
//        return isAttribute(ObjectType.DATA_ELEMENT_GROUP);
//    }
//
//    public void setDataElementGroupAttribute(Boolean dataElementGroupAttribute) {
//        setAttribute(ObjectType.DATA_ELEMENT_GROUP, dataElementGroupAttribute);
//    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    @Property(value = PropertyType.BOOLEAN, required = Property.Value.FALSE)
////    @Access(AccessType.PROPERTY)
////    public boolean getIndicatorAttribute() {
////        return isAttribute(ObjectType.INDICATOR);
////    }
////
////    public void setIndicatorAttribute(boolean indicatorAttribute) {
////        setAttribute(ObjectType.INDICATOR, indicatorAttribute);
////    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getIndicatorGroupAttribute() {
////        return isAttribute(ObjectType.INDICATOR_GROUP);
////    }
////
////    public void setIndicatorGroupAttribute(Boolean indicatorGroupAttribute) {
////        setAttribute(ObjectType.INDICATOR_GROUP, indicatorGroupAttribute);
////    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getDataSetAttribute() {
////        return isAttribute(ObjectType.DATA_SET);
////    }
////
////    public void setDataSetAttribute(Boolean dataSetAttribute) {
////        setAttribute(ObjectType.DATA_SET, dataSetAttribute);
////    }
//
//    @JsonProperty
//    @Column(name = "organisation_unit_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getOrganisationUnitAttribute() {
//        return isAttribute(ObjectType.ORG_UNIT);
//    }
//
//    public void setOrganisationUnitAttribute(boolean organisationUnitAttribute) {
//        setAttribute(ObjectType.ORG_UNIT, organisationUnitAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "organisation_unit_group_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getOrganisationUnitGroupAttribute() {
//        return isAttribute(ObjectType.ORGANISATION_UNIT_GROUP);
//    }
//
//    public void setOrganisationUnitGroupAttribute(Boolean organisationUnitGroupAttribute) {
//        setAttribute(ObjectType.ORGANISATION_UNIT_GROUP, organisationUnitGroupAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "organisation_unit_group_set_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getOrganisationUnitGroupSetAttribute() {
//        return isAttribute(ObjectType.ORGANISATION_UNIT_GROUP_SET);
//    }
//
//    public void setOrganisationUnitGroupSetAttribute(Boolean organisationUnitGroupSetAttribute) {
//        setAttribute(ObjectType.ORGANISATION_UNIT_GROUP_SET, organisationUnitGroupSetAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "user_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getUserAttribute() {
//        return isAttribute(ObjectType.USER);
//    }
//
//    public void setUserAttribute(boolean userAttribute) {
//        setAttribute(ObjectType.USER, userAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "user_group_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getUserGroupAttribute() {
//        return isAttribute(ObjectType.Team);
//    }
//
//    public void setUserGroupAttribute(Boolean userGroupAttribute) {
//        setAttribute(ObjectType.Team, userGroupAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "template_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getTemplateAttribute() {
//        return isAttribute(ObjectType.TEMPLATE);
//    }
//
//    public void setTemplateAttribute(boolean templateAttribute) {
//        setAttribute(ObjectType.TEMPLATE, templateAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "data_stage_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getDataStageAttribute() {
//        return isAttribute(ObjectType.DATA_STAGE);
//    }
//
//    public void setDataStageAttribute(boolean dataStageAttribute) {
//        setAttribute(ObjectType.DATA_STAGE, dataStageAttribute);
//    }
//
//    @JsonProperty
//    @Column(name = "entity_definition_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getEntityDefinitionAttribute() {
//        return isAttribute(ObjectType.ENTITY_DEFINITION);
//    }
//
//    public void setEntityDefinitionAttribute(boolean entityDefinitionAttribute) {
//        setAttribute(ObjectType.ENTITY_DEFINITION, entityDefinitionAttribute);
//    }
//
////    @JsonProperty
////    @Access(AccessType.PROPERTY)
////    @Column(name = "tracked_attribute_attribute")
////    public boolean getTrackedEntityAttributeAttribute() {
////        return isAttribute(ObjectType.TRACKED_ENTITY_ATTRIBUTE);
////    }
////
////    public void setTrackedEntityAttributeAttribute(boolean trackedEntityAttributeAttribute) {
////        setAttribute(ObjectType.TRACKED_ENTITY_ATTRIBUTE, trackedEntityAttributeAttribute);
////    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getCategoryOptionAttribute() {
////        return isAttribute(ObjectType.CATEGORY_OPTION);
////    }
////
////    public void setCategoryOptionAttribute(boolean categoryOptionAttribute) {
////        setAttribute(ObjectType.CATEGORY_OPTION, categoryOptionAttribute);
////    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getCategoryOptionGroupAttribute() {
////        return isAttribute(ObjectType.CATEGORY_OPTION_GROUP);
////    }
////
////    public void setCategoryOptionGroupAttribute(boolean categoryOptionGroupAttribute) {
////        setAttribute(ObjectType.CATEGORY_OPTION_GROUP, categoryOptionGroupAttribute);
////    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    @Column(name = "document_attribute")
////    @Access(AccessType.PROPERTY)
////    public boolean getDocumentAttribute() {
////        return isAttribute(ObjectType.DOCUMENT);
////    }
////
////    public void setDocumentAttribute(boolean documentAttribute) {
////        setAttribute(ObjectType.DOCUMENT, documentAttribute);
////    }
//
////    @JsonProperty
////    @Column(name = "option_attribute")
////    @Access(AccessType.PROPERTY)
////    public boolean getOptionAttribute() {
////        return isAttribute(ObjectType.OPTION);
////    }
////
////    public void setOptionAttribute(boolean optionAttribute) {
////        setAttribute(ObjectType.OPTION, optionAttribute);
////    }
//
//    @JsonProperty
//    @Column(name = "option_set_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getOptionSetAttribute() {
//        return isAttribute(ObjectType.OPTION_SET);
//    }
//
//    public void setOptionSetAttribute(boolean optionSetAttribute) {
//        setAttribute(ObjectType.OPTION_SET, optionSetAttribute);
//    }
//
////    @JsonProperty
////    @Column(name = "legend_set_attribute")
////    @Access(AccessType.PROPERTY)
////    public boolean getLegendSetAttribute() {
////        return isAttribute(ObjectType.LEGEND_SET);
////    }
////
////    public void setLegendSetAttribute(boolean legendSetAttribute) {
////        setAttribute(ObjectType.LEGEND_SET, legendSetAttribute);
////    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    @Column(name = "constant_attribute")
////    @Access(AccessType.PROPERTY)
////    public boolean getConstantAttribute() {
////        return isAttribute(ObjectType.CONSTANT);
////    }
////
////    public void setConstantAttribute(boolean constantAttribute) {
////        setAttribute(ObjectType.CONSTANT, constantAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    @Column(name = "Program_indicator_attribute")
////    @Access(AccessType.PROPERTY)
////    public boolean getProgramIndicatorAttribute() {
////        return isAttribute(ObjectType.PROGRAM_INDICATOR);
////    }
////
////    public void setProgramIndicatorAttribute(boolean programIndicatorAttribute) {
////        setAttribute(ObjectType.PROGRAM_INDICATOR, programIndicatorAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    @Column(name = "sql_view_attribute")
////    @Access(AccessType.PROPERTY)
////    public boolean getSqlViewAttribute() {
////        return isAttribute(ObjectType.SQL_VIEW);
////    }
////
////    public void setSqlViewAttribute(boolean sqlViewAttribute) {
////        setAttribute(ObjectType.SQL_VIEW, sqlViewAttribute);
////    }
//
//    //    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getCategoryOptionComboAttribute() {
////        return isAttribute(ObjectType.CATEGORY_OPTION_COMBO);
////    }
////
////    public void setCategoryOptionComboAttribute(boolean categoryOptionComboAttribute) {
////        setAttribute(ObjectType.CATEGORY_OPTION_COMBO, categoryOptionComboAttribute);
////    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getSectionAttribute() {
////        return isAttribute(ObjectType.SECTION);
////    }
////
////    public void setSectionAttribute(boolean sectionAttribute) {
////        setAttribute(ObjectType.SECTION, sectionAttribute);
////    }
//
//    @JsonProperty
//    public OptionSet getOptionSet() {
//        return optionSet;
//    }
//
//    public void setOptionSet(OptionSet optionSet) {
//        this.optionSet = optionSet;
//    }
//
//    //    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getCategoryOptionGroupSetAttribute() {
////        return isAttribute(ObjectType.CATEGORY_OPTION_GROUP_SET);
////    }
////
////    public void setCategoryOptionGroupSetAttribute(boolean categoryOptionGroupSetAttribute) {
////        setAttribute(ObjectType.CATEGORY_OPTION_GROUP_SET, categoryOptionGroupSetAttribute);
////    }
//
//    @JsonProperty
//    @Column(name = "data_element_group_set_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getDataElementGroupSetAttribute() {
//        return isAttribute(ObjectType.DATA_ELEMENT_GROUP_SET);
//    }
//
//    public void setDataElementGroupSetAttribute(boolean dataElementGroupSetAttribute) {
//        setAttribute(ObjectType.DATA_ELEMENT_GROUP_SET, dataElementGroupSetAttribute);
//    }
//
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getValidationRuleAttribute() {
////        return isAttribute(ObjectType.VALIDATION_RULE);
////    }
////
////    public void setValidationRuleAttribute(boolean validationRuleAttribute) {
////        setAttribute(ObjectType.VALIDATION_RULE, validationRuleAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getValidationRuleGroupAttribute() {
////        return isAttribute(ObjectType.VALIDATION_RULE_GROUP);
////    }
////
////    public void setValidationRuleGroupAttribute(boolean validationRuleGroupAttribute) {
////        setAttribute(ObjectType.VALIDATION_RULE_GROUP, validationRuleGroupAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getCategoryAttribute() {
////        return isAttribute(ObjectType.CATEGORY);
////    }
////
////    public void setCategoryAttribute(boolean categoryAttribute) {
////        setAttribute(ObjectType.CATEGORY, categoryAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getVisualizationAttribute() {
////        return isAttribute(ObjectType.VISUALIZATION);
////    }
////
////    public void setVisualizationAttribute(boolean visualizationAttribute) {
////        setAttribute(ObjectType.VISUALIZATION, visualizationAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getMapAttribute() {
////        return isAttribute(ObjectType.MAP);
////    }
////
////    public void setMapAttribute(boolean mapAttribute) {
////        setAttribute(ObjectType.MAP, mapAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getEventReportAttribute() {
////        return isAttribute(ObjectType.EVENT_REPORT);
////    }
////
////    public void setEventReportAttribute(boolean eventReportAttribute) {
////        setAttribute(ObjectType.EVENT_REPORT, eventReportAttribute);
////    }
////
////    @JsonProperty
////    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
////    public boolean getEventChartAttribute() {
////        return isAttribute(ObjectType.EVENT_CHART);
////    }
////
////    public void setEventChartAttribute(boolean eventChartAttribute) {
////        setAttribute(ObjectType.EVENT_CHART, eventChartAttribute);
////    }
//
////    @JsonProperty
////    @Column(name = "relationship_type_attribute")
////    @Access(AccessType.PROPERTY)
////    public boolean getRelationshipTypeAttribute() {
////        return isAttribute(ObjectType.RELATIONSHIP_TYPE);
////    }
//
////    public void setRelationshipTypeAttribute(boolean relationshipTypeAttribute) {
////        setAttribute(ObjectType.RELATIONSHIP_TYPE, relationshipTypeAttribute);
////    }
//
//    @JsonProperty
//    public Integer getSortOrder() {
//        return sortOrder;
//    }
//
//    public void setSortOrder(Integer sortOrder) {
//        this.sortOrder = sortOrder;
//    }
//
//    public List<Class<? extends PrimaryKeyObject<?>>> getSupportedClasses() {
//        return objectTypes.stream().map(ObjectType::getType).collect(toList());
//    }
//
//    @Override
//    public String toString() {
//        return MoreObjects.toStringHelper(this)
//            .add("sortOrder", sortOrder)
//            .add("dataType", dataType)
//            .add("objectTypes", objectTypes)
//            .add("mandatory", mandatory)
//            .toString();
//    }
//}
