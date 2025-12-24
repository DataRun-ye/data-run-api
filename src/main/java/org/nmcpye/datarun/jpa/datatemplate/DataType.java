package org.nmcpye.datarun.jpa.datatemplate;

import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

public enum DataType {
    TEXT,
    BOOLEAN,
    INTEGER,
    DECIMAL,
    TIMESTAMP,
    ARRAY;

    public static DataType fromValueType(ValueType vt) {
        if (vt == null) return TEXT;
        return switch (vt) {
            case Integer, IntegerPositive, IntegerNegative,
                 IntegerZeroOrPositive -> INTEGER;
            case Number, Percentage,
                 UnitInterval -> DECIMAL;
            case Boolean, TrueOnly, YesNo -> BOOLEAN;
            case SelectMulti -> ARRAY;
            case Date, DateTime, Time -> TIMESTAMP;
            default -> TEXT;
        };
    }
}
