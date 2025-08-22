package org.nmcpye.datarun.jpa.etl.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Canonical SubmissionValueRow DTO used by the normalizer and DAO.
 * Field names and getter names must match those used by SubmissionValuesJdbcDao.
 *
 * @author Hamza Assada 10/08/2025 (7amza.it@gmail.com)
 */
@Data
@Builder
@EqualsAndHashCode(of = {"submissionId", "repeatInstanceId", "elementId", "optionId"},
    cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class ElementDataValue {
    // db bigserial
    private Long id;

    // ------------------------
    // Submission & Context
    // ------------------------
    // db: "submission_id"
    private String submissionId;
    // ------------------------


    // ------------------------
    // LEVEL 1: Assignment Dimensions
    // ------------------------
    // db: "assignment_id"
    private String assignmentId;
    // db: "team_id"
    private String teamId;
    // db: "org_unit_id"
    private String orgUnitId;
    // db: "activity_id"
    private String activityId;
    // ------------------------


    // ------------------------
    // Data Element & Value
    // ------------------------
    // db: "element_id"
    private String elementId;
    // db: element_label (jsonb)
    private String elementLabel;
    // ------------------------

    // ------------------------
    // Form Template parameters
    // would be deprecated if global
    // cross domain grouping/filtering
    // worked (i.e element,orgUnit,team, activity,
    // assignment, category, single select option...etc)
    // ------------------------
    // db: repeat_instance_id
    private String repeatInstanceId;

    // ------------------------


    // ------------------------
    // For multi-select, this id is not null,
    // each selected option is stored in a row
    // ------------------------
    // db: "option_id"
    private String optionId;
    // ------------------------

    // ------------------------
    // Measures
    // ------------------------
    // db: "value_text"
    private String valueText;
    // db: "value_num"
    private BigDecimal valueNum;
    // db: "value_bool"
    private Boolean valueBool;

    private Instant valueTs;
    // ------------------------

    // ------------------------
    // value timestamps
    // ------------------------
    // db: "deleted_at"
    private Instant deletedAt;
    // db: "created_date"
    private Instant createdDate;
    // db: "last_modified_date"
    private Instant lastModifiedDate;
    // ------------------------
}

