package org.nmcpye.datarun.jpa.scopeinstance;

import java.util.List;

/**
 * @author Hamza Assada 07/06/2025 <7amza.it@gmail.com>
 */
public enum DimensionalType {
    ACTIVITY,
    TEAM,
    ORG_UNIT,
    ENTITY,
    DATE,
    NUMBER,
    STRING;

    public static final List<DimensionalType> coreElements = List.of(DimensionalType.ORG_UNIT,
        DimensionalType.TEAM,
        DimensionalType.ACTIVITY, DimensionalType.ENTITY);

    public boolean isCoreElement() {
        return coreElements.contains(this);
    }

    public boolean isOrgUnit() {
        return this == DimensionalType.ORG_UNIT;
    }

    public boolean isActivity() {
        return this == DimensionalType.ACTIVITY;
    }

    public boolean isTeam() {
        return this == DimensionalType.TEAM;
    }

    public boolean isEntity() {
        return this == DimensionalType.ENTITY;
    }
}
