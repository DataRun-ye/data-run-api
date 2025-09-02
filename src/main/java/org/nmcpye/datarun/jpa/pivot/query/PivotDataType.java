package org.nmcpye.datarun.jpa.pivot.query;

/**
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
public enum PivotDataType {
    /**
     * For all text-like types (Text, Letter, OrgUnit, etc.)
     */
    TEXT,
    /**
     * For all number-like types (Integer, Percentage, etc.)
     */
    NUMERIC,
    BOOLEAN, DATE, DATETIME
}
