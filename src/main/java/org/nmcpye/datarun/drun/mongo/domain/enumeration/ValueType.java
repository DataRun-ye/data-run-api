package org.nmcpye.datarun.drun.mongo.domain.enumeration;

import java.util.List;

/**
 * The ValueType enumeration.
 */
public enum ValueType {
    ScannedCode,
    Section,
    RepeatableSection,
    SelectMulti,
    SelectOne,
    Age,
    FullName,
    Text,
    LongText,
    Letter,
    Boolean,
    TrueOnly,
    YesNo,
    Date,
    DateTime,
    Time,
    Number,
    Percentage,
    Integer,
    IntegerPositive,
    IntegerNegative,
    IntegerZeroOrPositive,
    UnitInterval,
    PhoneNumber,
    Email,
    URL,
    Username,
    Image,
    OrganisationUnit,
    Reference,
    FileResource,
    Calculated,
    Coordinate,
    GeoJson;

    public List<ValueType> SectionTypes() {
        return List.of(Section, RepeatableSection);
    }

    public boolean isSectionType() {
        return SectionTypes().contains(this);
    }
}
