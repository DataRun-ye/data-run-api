package org.nmcpye.datarun.etl.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nmcpye.datarun.common.enumeration.FlowStatus;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.datasubmission.DataSubmission}
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
//@Accessors(fluent = true)
public class SubmissionContext implements Serializable {
    @NotNull
    @Size(max = 11)
    private String templateUid;

    @NotNull
    @Size(max = 11)
    private String templateVersionUid;

    @NotNull
    @Size(max = 11)
    private String teamUid;

    @NotNull
    private Integer version;

    @NotNull
    @Size(max = 11)
    private String orgUnitUid;

    @NotNull
    @Size(max = 11)
    private String activityUid;

    @NotNull
    @Size(max = 11)
    private String assignmentUid;

    @NotNull
    private Long submissionSerial;

    @NotNull
    @Size(max = 26)
    private String submissionId;
    @NotNull
    @Size(max = 11)
    private String submissionUid;

    private FlowStatus status;

    private Instant startTime;
    @Builder.Default
    private Instant submissionCreationTime = Instant.now();

    @NotNull
    private String createdBy;
    private String lastModifiedBy;

}
