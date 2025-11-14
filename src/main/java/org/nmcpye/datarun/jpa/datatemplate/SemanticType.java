package org.nmcpye.datarun.jpa.datatemplate;

import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

public enum SemanticType {
    OrgUnit,
    Team,
    Activity,
    Option,
    Name,
    Age,
    PhoneNumber,
    Email,
    URL,
    Image,
    Progress,
    Entity,
    Username,
    Coordinate,
    Repeat,
    GeoJson;

    public static SemanticType fromValueType(ValueType valueType) {
        if(valueType.isOptionsType()) return Option;

        for (SemanticType type : SemanticType.values()) {
            if (type.name().equalsIgnoreCase(valueType.name())) {
                return type;
            }
        }

        return null;
    }
}
