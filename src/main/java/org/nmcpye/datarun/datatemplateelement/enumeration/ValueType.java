package org.nmcpye.datarun.datatemplateelement.enumeration;

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
    Percentage,
    Number,
    Integer,
    IntegerPositive,
    IntegerNegative,
    IntegerZeroOrPositive,
    UnitInterval,
    PhoneNumber,
    Email,
    URL,
    Image,
    Progress,

    Activity,
    OrganisationUnit,
    Team,
    Entity,
    Username,
    Reference,
    FileResource,
    Calculated,
    Coordinate,
    GeoJson;

    public List<ValueType> SectionTypes() {
        return List.of(Section, RepeatableSection);
    }

    public List<ValueType> numericTypes() {
        return List.of(Number,
            Integer,
            IntegerPositive,
            IntegerNegative,
            IntegerZeroOrPositive);
    }

    public List<ValueType> textualTypes() {
        return List.of(FullName, Text, LongText, Letter);
    }

    public boolean isNumeric() {
        return numericTypes().contains(this);
    }

    public boolean isTextual() {
        return textualTypes().contains(this);
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


    /**
     * Returns true if the two value types should be considered compatible:
     * - exactly the same, or
     * - both numeric, or
     * - both textual.
     */
    public boolean isCompatible(ValueType other) {
        if (this == other) {
            return true;
        }
        if (this.isNumeric() && other.isNumeric()) {
            return true;
        }
        if (this.isTextual() && other.isTextual()) {
            return true;
        }
        return false;
    }
}
