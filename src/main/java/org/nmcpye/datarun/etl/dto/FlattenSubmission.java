package org.nmcpye.datarun.etl.dto;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 10/02/2026
 */
@Getter
@Setter
@Builder
@Accessors(chain = true, fluent = true)
@AllArgsConstructor
public class FlattenSubmission {
//    private final UUID ingestId;
//    private final Long outboxId;
//    private final String submissionId;
//    private final String submissionUid;
//    /// submission version
//    private final Integer version;
//    private final Long submissionSerial;
//    // context
//    private final String assignmentUid;
//    private final String activityUid;
//    private final String orgUnitUid;
//    private final String teamUid;
//    private final String templateUid;
//    private final String templateVersionUid;
//
//    /// submission created at server
//    private final Instant submissionCreationTime;
//    /// submission creation time at client, opening form template
//    private final Instant startTime;
//
//    /// submission created by
//    private final String createdBy;
//    /// submission created by
//    private final String lastModifiedBy;
//
//    private final Instant deletedAt;

    @Singular
    private List<EventRow> eventRows;

    @Singular
    private List<TallCanonicalValue> tallCanonicalRows;
}
