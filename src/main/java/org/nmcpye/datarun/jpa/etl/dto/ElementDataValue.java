package org.nmcpye.datarun.jpa.etl.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nmcpye.datarun.jpa.dataelement.DataElement;

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
@EqualsAndHashCode(of = {"submissionUid", "repeatInstanceId",
    "elementUid", "optionUid"},
    cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class ElementDataValue {

    /**
     * Primary DB id (bigserial).
     * db: {@code id, PK}
     */
    private Long id;

    private String semanticPath;

    /**
     * Reference to the element configuration that produced this value.
     * db: {@code element_template_config_uid, not null}
     */
    private String elementTemplateConfigUid;

    // ------------------------
    // Submission & Context
    // ------------------------

    /**
     * Submission unique identifier.
     * db: {@code submission.uid, not null}
     */
    private String submissionUid;

    // ------------------------
    // Assignment Dimensions (level 1)
    // ------------------------

    /**
     * Assignment identifier (context of this submission).
     * db: {@code assignment.uid, not null}
     */
    private String assignmentUid;

    /**
     * Team identifier related to this submission.
     * db: {@code team.uid, not null}
     */
    private String teamUid;

    /**
     * Organizational unit identifier related to this submission.
     * db: {@code org_unit_id, not null}
     */
    private String orgUnitUid;

    /**
     * Activity identifier related to this submission.
     * db: {@code activity_id, not null}
     */
    private String activityUid;

    // ------------------------
    // Data Element & Value
    // ------------------------

    /**
     * Global data element id (canonical DataElement.id).
     * db: {@code element_id, not null}
     */
    private String elementUid;

    /**
     * element value type i.e Text, Number, DateTime, Team, OrgUnit, ...etc
     * {@link DataElement#getValueType()}
     */
    private String valueType;

    // ------------------------
    // Template/Form parameters
    // ------------------------

    /**
     * Repeat instance id for repeated groups (global unique string 26).
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
     * it's rather stored in {@link #valueRefUid} like other reference entities)
     * <p>
     * db: {@code option.id, nullable}
     */
    private String optionUid;

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
    private String valueRefUid;

    /**
     * Value stored as text (for string values or normalized text representation).
     * <p>
     * db: {@code value_as_text}
     */
    private String valueAsText;
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
