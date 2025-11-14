package org.nmcpye.datarun.jpa.etl.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents one row for a single repeat-instance extracted from a submission.
 *
 * <p>Purpose
 * <ul>
 *   <li>Each instance corresponds to a repeated (repeatable) section occurrence in a submitted form.</li>
 *   <li>These rows are primary targets for analytics/materialized tables and aggregate queries.</li>
 * </ul>
 *
 * <p>Context levels
 * <ol>
 *   <li><b>Level-1 (submission-level) context</b> — submission-level tags (team_id, org_unit_id, activity_id)
 *       are propagated into each repeat row via {@link #submissionUid} and resolved server-side when loading
 *       submissions (assignment → context resolution).</li>
 *   <li><b>Level-2 (repeat-level) category</b> — optional subject tags inside the repeat (e.g. a selected
 *       team, entity, option). When a repeat defines a category element, that element's canonical id becomes
 *       {@link #categoryUid} and {@link #categoryKind} for all sibling rows of the repeat. This enables subject-level
 *       aggregations in analytics (context → subject).</li>
 * </ol>
 *
 * <p>Persistence / DB notes
 * <ul>
 *   <li>Stored as a row in the repeat instance table (example columns shown in field docs). Some fields hold JSONB
 *       content (e.g. localized labels).</li>
 *   <li>This DTO is intended for ETL and analytics pipelines — lightweight and serializable.</li>
 * </ul>
 *
 * <p>Soft deletes: {@link #deletedAt} marks soft-deletion timestamps used by the ETL pipeline to exclude or
 * archive rows.</p>
 *
 * @author Hamza Assada
 * @since 2025-06-02
 */
@Data
@Builder
public class RepeatInstance {
    /**
     * ULID id. Stored as String.
     * <p>
     * the repeat Instance PK
     */
    protected String id;

    protected String teUid;

    private String canonicalPath;
    private String canonicalElementUid;

    // ------------------------
    // LEVEL 2: Repeat Dimensions
    // ------------------------

    /**
     * FK to the submission this repeat instance belongs to.
     * <p>
     * db: {@code submission_id, not null}
     */
    private String submissionUid;

    /**
     * The specific subject canonical uid for the category element.
     * <p>
     * db: {@code category_id, nullable (repeat sections may not define a category)}
     */
    private String categoryUid;

    /**
     * system table name indicating the category subject kind e.g
     * 'team'|'org_unit'|'activity'|'entity'|'option'|... when the repeat defines a category element.
     * - Used as a dimension for aggregations (together with categoryId).
     * Example: "org_unit"
     */
    private String categoryKind;

    /**
     * The specific subject.
     * Non-localized human-readable display name for the category subject
     * (convenience; not the localized label).
     */
    private String categoryName;

    /**
     * Localized label map, typically language-code → label (e.g., {"en":"Clinic A","ar":"..."}).
     * <p>
     * - Kept as String in this DTO; parsing/typing happens elsewhere in the pipeline.
     * <p>
     * db: {@code JSONB}.
     */
    private String categoryLabel;

    /**
     * (template mode analysis): Points to the parent repeat instance id when repeats are nested.
     * Enables reconstructing nested repeat hierarchies in analytics (template mode).
     * <p>
     * db: {@code parent_repeat_instance_id, nullable}
     */
    private String parentRepeatInstanceId;
    // ------------------------

    /**
     * DB: (jsonb)
     * <ul>
     *   <li>Localized label for the repeat section (jsonb map — language-code → label).</li>
     *   <li>Useful for UI or analytics display (template mode); stored as String in this DTO</li>
     * </ul>
     */
    private String repeatSectionLabel;

    /**
     * dot-delimited path for the repeat within the form. Normal, non-repeatable
     * sections can also be part of this path.
     * <ul>
     *   <li>Template/element path (unique within form template) used to locate element config.</li>
     *   <li>Helpful when joining back to template metadata.</li>
     * </ul>
     * <p>
     * db: {@code repeat_path, not null}
     */
    private String repeatPath;

    /**
     * used in template mode analysis: Ordering of rows inside the same submission and repeat path.
     * <p>
     * db: {@code repeat_index, not null}
     */
    private Long repeatIndex;

    /**
     * Soft-delete timestamp: When non-null, this row is considered soft-deleted for analytics/ETL unless archived.
     */
    protected Instant deletedAt;

    /**
     * Timestamp when the submission was completed (client-side).
     * <p>
     * db: {@code not null}
     */
    private Instant submissionCompletedAt;

    /**
     * Last update time from the client.
     * Allows reconciliation between client-side edits and server ingestion time.
     * <p>
     * db: {@code not null}
     */
    private Instant clientUpdatedAt;

    /**
     * Row creation timestamp in the ETL/source system.
     * <p>
     * db: {@code not null}
     */
    protected Instant createdDate;

    /**
     * When this repeat instance row was last updated in the pipeline.
     * <p>
     * db: {@code not null}
     */
    private Instant lastModifiedDate;

    protected String createdBy;

    protected String lastModifiedBy;
}
