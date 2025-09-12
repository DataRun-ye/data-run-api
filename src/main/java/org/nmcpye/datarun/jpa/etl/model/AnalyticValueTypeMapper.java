package org.nmcpye.datarun.jpa.etl.model;

import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 25/08/2025
 */
public class AnalyticValueTypeMapper {
    public static AnalyticValueType map(ValueType valueType) {
        return switch (valueType) {
            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> AnalyticValueType.NUMBER;
            case Date, DateTime, Time, Age -> AnalyticValueType.DATE;
            case SelectOne, SelectMulti -> AnalyticValueType.OPTION;
            case OrganisationUnit, Team, Activity, Entity -> AnalyticValueType.REFERENCE;
            default -> AnalyticValueType.TEXT;

        };
    }
}
