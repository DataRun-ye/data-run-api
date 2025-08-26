package org.nmcpye.datarun.jpa.etl.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * @author Hamza Assada
 * @since 02/06/2025
 */
@Data
@Builder
public class RepeatInstance {
    /// ULID id (Universally Unique Lexicographically Sortable Identifier).
    /// Length: 26 characters, Base32 encoded.
    protected String id;

    // ------------------------
    // LEVEL 2: Repeat Dimensions
    // ------------------------
    /// db: submission_id
    private String submissionId;

    /// system table name: 'team'|'org_unit'|'activity'|'entity'|'option'|...,
    /// db: category_kind
    private String categoryKind;
    /// The specific subject ID, db: category_id
    private String categoryId;

    /// The specific subject name
    ///  db: category_name
    private String categoryName;

    /// jsonb map for localized display labels
    private String categoryLabel;
    /// db: parent_repeat_instance_id
    private String parentRepeatInstanceId;
    // ------------------------

    /// db: repeat_section_label (jsonb)
    private String repeatSectionLabel;
    //
    private String repeatPath;
    private Long repeatIndex;

    protected Instant deletedAt;

    /// db: "submission_completed_at"
    private Instant submissionCompletedAt;
    private Instant clientUpdatedAt;

    protected Instant createdDate;
    private Instant lastModifiedDate;
    protected String createdBy;
    protected String lastModifiedBy;
}
