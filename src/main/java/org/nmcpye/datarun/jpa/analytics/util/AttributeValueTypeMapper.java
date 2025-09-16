package org.nmcpye.datarun.jpa.analytics.util;

import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.analytics.domain.enums.DataType;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 25/08/2025
 */
public class AttributeValueTypeMapper {
    public static DataType map(ValueType valueType) {
        return switch (valueType) {
            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> DataType.NUMBER;
            case Date, DateTime, Time, Age -> DataType.TIMESTAMP;
//            case SelectMulti -> AttributeDataType.OPTIONS;
            case OrganisationUnit, Team, Activity, Entity, SelectOne, SelectMulti -> DataType.ENTITY_REF;
            default -> DataType.TEXT;
        };
    }
}
