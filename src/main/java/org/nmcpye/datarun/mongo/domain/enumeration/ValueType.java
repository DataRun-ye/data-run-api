package org.nmcpye.datarun.mongo.domain.enumeration;

import java.util.List;

/**
 * The ValueType enumeration.
 */
public enum ValueType {
    @Deprecated(since = "Remove for Value Type rendering")
    ScannedCode,
    @Deprecated(since = "Remove for boolean repeatable in section")
    RepeatableSection,
    Section,
    @Deprecated(since = "Remove for boolean isMulti with select one")
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
    Task,
    Team,
    Progress,

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

    public boolean isSection() {
        return this == Section;
    }

    public boolean isRepeat() {
        return this == RepeatableSection;
    }

    public boolean isReference() {
        return this == Reference;
    }

    public boolean isOptionsType() {
        return this == SelectOne || this == SelectMulti;
    }
}
