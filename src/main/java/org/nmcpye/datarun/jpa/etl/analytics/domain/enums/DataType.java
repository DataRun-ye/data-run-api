package org.nmcpye.datarun.jpa.etl.analytics.domain.enums;

/**
 * @author Hamza Assada
 * @since 11/09/2025
 */
public enum DataType {
    TEXT,
    NUMBER,
    TIMESTAMP,
    BOOLEAN,
    OPTIONS, // multi select element values
    ENTITY_REF,
    REPEAT_GROUP,
    JSON
}
