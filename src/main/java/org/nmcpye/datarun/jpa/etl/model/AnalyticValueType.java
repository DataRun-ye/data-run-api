package org.nmcpye.datarun.jpa.etl.model;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 25/08/2025
 */
public enum AnalyticValueType {
    NUMBER,
    TEXT,
    BOOLEAN,
    TIMESTAMP,
    CATEGORY,
    REFERENCE, // e.g Team, OrgUnit, Activity, Entity, Option, etc.
    OPTION,
}
