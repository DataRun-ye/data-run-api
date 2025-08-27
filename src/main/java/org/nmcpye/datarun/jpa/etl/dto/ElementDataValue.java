package org.nmcpye.datarun.jpa.etl.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Canonical DTO representing a single element value produced during normalization and persisted by the DAO.
 *
 * <p>
 * Notes:
 * <ul>
 *   <li>Field names and getter names must match those used by {@code SubmissionValuesJdbcDao}.</li>
 *   <li>Equals/hashCode is based on {@code submissionId, repeatInstanceId, elementId, optionId}.</li>
 * </ul>
 *
 * @author Hamza Assada
 * @since 2025-10-08
 */
@Data
@Builder
@EqualsAndHashCode(of = {"submissionId", "repeatInstanceId", "elementId", "optionId"},
    cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class ElementDataValue {

    /**
     * Primary DB id (bigserial).
     * db: {@code id, PK}
     */
    private Long id;

    /**
     * Reference to the element configuration that produced this value.
     * db: {@code element_config_id, not null}
     */
    private Long elementConfigId;

    // ------------------------
    // Submission & Context
    // ------------------------

    /**
     * Submission unique identifier.
     * db: {@code submission_id, not null}
     */
    private String submissionId;

    // ------------------------
    // Assignment Dimensions (level 1)
    // ------------------------

    /**
     * Assignment identifier (context of this submission).
     * db: {@code assignment_id, not null}
     */
    private String assignmentId;

    /**
     * Team identifier related to this submission.
     * db: {@code team_id, not null}
     */
    private String teamId;

    /**
     * Organizational unit identifier related to this submission.
     * db: {@code org_unit_id, not null}
     */
    private String orgUnitId;

    /**
     * Activity identifier related to this submission.
     * db: {@code activity_id, not null}
     */
    private String activityId;

    // ------------------------
    // Data Element & Value
    // ------------------------

    /**
     * Global data element id (canonical DataElement.id).
     * db: {@code element_id, not null}
     */
    private String elementId;

    /**
     * Human-readable/JSON label for the element.
     * db: {@code element_label, (JSONB)}
     */
    private String elementLabel;

    // ------------------------
    // Template/Form parameters
    // ------------------------

    /**
     * Repeat instance id for repeated groups (indexed path or token).
     * Use when the element belongs to a repeated section.
     * db: {@code repeat_instance_id, nullable}
     */
    private String repeatInstanceId;

    // ------------------------
    // Option / Multi-select support
    // ------------------------

    /**
     * {@code Option.id} for multi-select elements (part of uniquely identifying each option selected in a multi select values)
     * When non-null, each selected {@code Option} is a separate row.
     * (Important Note: single select "{@code Option.id}" is not stored in this property,
     * it's rather stored in {@link #valueRef} like other reference entities)
     * <p>
     * db: {@code option_id, nullable}
     */
    private String optionId;

    // ------------------------
    // Measures / Stored value variants
    // ------------------------

    /**
     * Value stored as text (for string values or normalized text representation).
     * <p>
     * db: {@code value_text}
     */
    private String valueText;

    /**
     * Value stored as numeric (for numeric measures).
     * <p>
     * db: {@code value_num}
     */
    private BigDecimal valueNum;

    /**
     * Value stored as boolean.
     * db: {@code value_bool}
     * <p>
     * for every stored boolean value, an aggregatable value (e.g. 1/0) is also stored
     * in {@link #valueNum} during normalization (for fast aggregations
     * of boolean (e.g.{@code Agg.COUNT_TRUE, Agg.PERCENT_TRUE}))
     */
    private Boolean valueBool;

    /**
     * Value stored as timestamp for date/time type values.
     * <p>
     * for every stored timestamp value, an aggregatable value (epochSecond) is also stored
     * in {@link #valueNum} during normalization (for fast lookup with same type,
     * or timestamp calculations
     * db: {@code value_ts}
     */
    private Instant valueTs;

    /**
     * Reference value for reference-type elements (IDs of other domain entities).
     * Examples: a {@code DataTemplateVersion} can have elements that neither categories nor multi-select but of
     * one of type {@code OrgUnit.id, Team.id, Activity.id, DomainEntity.id, Option.id (for select-one elements)}.
     * <p>
     * elements of this type usually become a COUNT, COUNT_DISTINCT measure in an aggregatable result,
     * or the specific entity if per specific {@code DataTemplate} result
     * db: {@code value_ref, nullable}
     */
    private String valueRef;

    // ------------------------
    // Auditing & lifecycle
    // ------------------------

    /**
     * Soft-delete timestamp. When non-null, this row is considered deleted.
     * db: {@code deleted_at}
     */
    private Instant deletedAt;

    /**
     * Normalization creation time (when the value was normalized/inserted).
     * db: {@code created_date}
     */
    private Instant createdDate;

    /**
     * Normalization last modified time (when the normalized value was last updated).
     * db: {@code last_modified_date}
     */
    private Instant lastModifiedDate;
}
