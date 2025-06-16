package org.nmcpye.datarun.jpa.scopeinstance;

import java.util.List;

/**
 * @author Hamza Assada 07/06/2025 <7amza.it@gmail.com>
 */
public enum CoreElementType {
    ACTIVITY,
    TEAM,
    ORG_UNIT,
    ENTITY,
    DATE,
    NUMBER,
    STRING;

    public static final List<CoreElementType> coreElements = List.of(CoreElementType.ORG_UNIT,
        CoreElementType.TEAM,
        CoreElementType.ACTIVITY, CoreElementType.ENTITY);

    public boolean isCoreElement() {
        return coreElements.contains(this);
    }

    public boolean isOrgUnit() {
        return this == CoreElementType.ORG_UNIT;
    }

    public boolean isActivity() {
        return this == CoreElementType.ACTIVITY;
    }

    public boolean isTeam() {
        return this == CoreElementType.TEAM;
    }

    public boolean isEntity() {
        return this == CoreElementType.ENTITY;
    }
}
