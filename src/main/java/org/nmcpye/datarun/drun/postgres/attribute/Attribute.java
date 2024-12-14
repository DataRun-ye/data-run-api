///*
// * Copyright (c) 2004-2022, University of Oslo
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright notice, this
// * list of conditions and the following disclaimer.
// *
// * Redistributions in binary form must reproduce the above copyright notice,
// * this list of conditions and the following disclaimer in the documentation
// * and/or other materials provided with the distribution.
// * Neither the name of the HISP project nor the names of its contributors may
// * be used to endorse or promote products derived from this software without
// * specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package org.nmcpye.datarun.drun.postgres.attribute;
//
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
//
//import java.time.Instant;
//
///**
// * @author Morten Olav Hansen <mortenoh@gmail.com>
// */
//@Entity
//@Table(name = "attribute")
//public class Attribute
//    extends AttributeBase {
//
//    @Id
//    @GeneratedValue
//    @Column(name = "attributeid")
//    private Long id;
//
//    @NotNull
//    @Size(max = 11)
//    @Column(name = "uid", length = 11, nullable = false, unique = true)
//    private String uid;
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
//    private ValueType valueType;
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
//    public Attribute() {
//
//    }
//
//    public Attribute(String uid) {
//        this.uid = uid;
//    }
//
//    public Attribute(String name, ValueType valueType) {
//        this.name = name;
//        this.valueType = valueType;
//    }
//
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getUid() {
//        return uid;
//    }
//
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    public String getCode() {
//        return code;
//    }
//
//    public void setCode(String code) {
//        this.code = code;
//    }
//
//    public Instant getCreatedDate() {
//        return createdDate;
//    }
//
//    public void setCreatedDate(Instant createdDate) {
//        this.createdDate = createdDate;
//    }
//
//    public Instant getLastModifiedDate() {
//        return lastModifiedDate;
//    }
//
//    public void setLastModifiedDate(Instant lastModifiedDate) {
//        this.lastModifiedDate = lastModifiedDate;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }
//
//    @Override
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    @Override
//    public String getShortName() {
//        return shortName;
//    }
//
//    @Override
//    public void setShortName(String shortName) {
//        this.shortName = shortName;
//    }
//
//    @Override
//    public String getDescription() {
//        return description;
//    }
//
//    @Override
//    public void setDescription(String description) {
//        this.description = description;
//    }
//
//    @Override
//    public User getCreatedBy() {
//        return createdBy;
//    }
//
//    @Override
//    public void setCreatedBy(User createdBy) {
//        this.createdBy = createdBy;
//    }
//
//    @Override
//    public User getUpdatedBy() {
//        return updatedBy;
//    }
//
//    @Override
//    public void setUpdatedBy(User updatedBy) {
//        this.updatedBy = updatedBy;
//    }
//
//    @Override
//    public int hashCode() {
//        return 31 * super.hashCode() + Objects.hash(valueType, objectTypes,
//            mandatory, unique, optionSet);
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        return this == obj || obj instanceof Attribute && super.equals(obj) && objectEquals((Attribute) obj);
//    }
//
//    private boolean objectEquals(Attribute other) {
//        return Objects.equals(this.valueType, other.valueType)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    public ValueType getValueType() {
//        return valueType;
//    }
//
//    public void setValueType(ValueType valueType) {
//        this.valueType = valueType;
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    public boolean isMandatory() {
//        return mandatory;
//    }
//
//    public void setMandatory(boolean mandatory) {
//        this.mandatory = mandatory;
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Property(value = PropertyType.BOOLEAN, required = Property.Value.TRUE)
//    public boolean isUnique() {
//        return unique;
//    }
//
//    public void setUnique(boolean unique) {
//        this.unique = unique;
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Property(value = PropertyType.BOOLEAN, required = Property.Value.FALSE)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Property(value = PropertyType.BOOLEAN, required = Property.Value.FALSE)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "user_attribute")
//    @Property(value = PropertyType.BOOLEAN, required = Property.Value.FALSE)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "program_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getProgramAttribute() {
//        return isAttribute(ObjectType.PROGRAM);
//    }
//
//    public void setProgramAttribute(boolean programAttribute) {
//        setAttribute(ObjectType.PROGRAM, programAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "program_stage_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getProgramStageAttribute() {
//        return isAttribute(ObjectType.PROGRAM_STAGE);
//    }
//
//    public void setProgramStageAttribute(boolean programStageAttribute) {
//        setAttribute(ObjectType.PROGRAM_STAGE, programStageAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "tracked_entity_type_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getTrackedEntityTypeAttribute() {
//        return isAttribute(ObjectType.TRACKED_ENTITY_TYPE);
//    }
//
//    public void setTrackedEntityTypeAttribute(boolean trackedEntityTypeAttribute) {
//        setAttribute(ObjectType.TRACKED_ENTITY_TYPE, trackedEntityTypeAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Access(AccessType.PROPERTY)
//    @Column(name = "tracked_entity_attribute_attribute")
//    public boolean getTrackedEntityAttributeAttribute() {
//        return isAttribute(ObjectType.TRACKED_ENTITY_ATTRIBUTE);
//    }
//
//    public void setTrackedEntityAttributeAttribute(boolean trackedEntityAttributeAttribute) {
//        setAttribute(ObjectType.TRACKED_ENTITY_ATTRIBUTE, trackedEntityAttributeAttribute);
//    }
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
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "document_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getDocumentAttribute() {
//        return isAttribute(ObjectType.DOCUMENT);
//    }
//
//    public void setDocumentAttribute(boolean documentAttribute) {
//        setAttribute(ObjectType.DOCUMENT, documentAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "option_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getOptionAttribute() {
//        return isAttribute(ObjectType.OPTION);
//    }
//
//    public void setOptionAttribute(boolean optionAttribute) {
//        setAttribute(ObjectType.OPTION, optionAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "legend_set_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getLegendSetAttribute() {
//        return isAttribute(ObjectType.LEGEND_SET);
//    }
//
//    public void setLegendSetAttribute(boolean legendSetAttribute) {
//        setAttribute(ObjectType.LEGEND_SET, legendSetAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "constant_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getConstantAttribute() {
//        return isAttribute(ObjectType.CONSTANT);
//    }
//
//    public void setConstantAttribute(boolean constantAttribute) {
//        setAttribute(ObjectType.CONSTANT, constantAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "Program_indicator_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getProgramIndicatorAttribute() {
//        return isAttribute(ObjectType.PROGRAM_INDICATOR);
//    }
//
//    public void setProgramIndicatorAttribute(boolean programIndicatorAttribute) {
//        setAttribute(ObjectType.PROGRAM_INDICATOR, programIndicatorAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "sql_view_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getSqlViewAttribute() {
//        return isAttribute(ObjectType.SQL_VIEW);
//    }
//
//    public void setSqlViewAttribute(boolean sqlViewAttribute) {
//        setAttribute(ObjectType.SQL_VIEW, sqlViewAttribute);
//    }
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    @Column(name = "relationship_type_attribute")
//    @Access(AccessType.PROPERTY)
//    public boolean getRelationshipTypeAttribute() {
//        return isAttribute(ObjectType.RELATIONSHIP_TYPE);
//    }
//
//    public void setRelationshipTypeAttribute(boolean relationshipTypeAttribute) {
//        setAttribute(ObjectType.RELATIONSHIP_TYPE, relationshipTypeAttribute);
//    }
//
//    @JsonProperty
//    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
//    public Integer getSortOrder() {
//        return sortOrder;
//    }
//
//    public void setSortOrder(Integer sortOrder) {
//        this.sortOrder = sortOrder;
//    }
//
//    public List<Class<? extends IdentifiableObject>> getSupportedClasses() {
//        return objectTypes.stream().map(ObjectType::getType).collect(toList());
//    }
//
//    @Override
//    public String toString() {
//        return MoreObjects.toStringHelper(this)
//            .add("sortOrder", sortOrder)
//            .add("valueType", valueType)
//            .add("objectTypes", objectTypes)
//            .add("mandatory", mandatory)
//            .toString();
//    }
//}
