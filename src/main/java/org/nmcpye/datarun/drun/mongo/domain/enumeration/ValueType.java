package org.nmcpye.datarun.drun.mongo.domain.enumeration;

import java.util.List;

/**
 * The ValueType enumeration.
 */
public enum ValueType {
    ScannedCode, Section, RepeatableSection, Text,
    LongText, Letter, PhoneNumber,
    Email, Boolean, TrueOnly,
    Date, DateTime, Time,
    Number, UnitInterval,
    Percentage, Integer,
    IntegerPositive, IntegerNegative, IntegerZeroOrPositive,
    Username, Coordinate, OrganisationUnit,
    Reference, Age, FullName, URL,
    FileResource, Image, SelectMulti,
    SelectOne, YesNo, GeoJson, Team, Activity, Warehouse;

    public List<ValueType> SectionTypes() {
        return List.of(Section, RepeatableSection);
    }

    public boolean isSectionType() {
        return SectionTypes().contains(this);
    }
}
