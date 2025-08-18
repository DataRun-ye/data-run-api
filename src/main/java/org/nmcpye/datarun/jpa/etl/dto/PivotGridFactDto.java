package org.nmcpye.datarun.jpa.etl.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
@Data
public class PivotGridFactDto {
    private Long id;

    // Submission & Context ---
    private String submission;
    private Instant submissionDate;

    // ---LEVEL 1: Assignment Dimensions ---
    private String assignment;
    private String team;
    private String orgUnit;
    private String activity;
    // ---

    // Data Element & Value ---
    private String element;
    private String elementName; //json
    private String valueType;
    // ---

    // --- LEVEL 2: Repeat Dimensions ---
    private String repeatInstanceId;
    private String repeatPath;
    // CREATE INDEX idx_element_data_value_category ON element_data_value(category_kind, category_id);
    private String categoryKind; //'team'|'orgUnit'|'Activity'|'Entity'|'Option'|...
    private String category; // The specific subject ID
    //-----

    // Measures ---
    private String valueText;
    private BigDecimal valueNum;
    private Boolean valueBool;
    private String option;
    // ---

    // value timestamps
    private Instant createdDate;
    private Instant lastModifiedDate;
    private Instant deletedAt;
}
