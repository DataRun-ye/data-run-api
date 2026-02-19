package org.nmcpye.etl.translation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.etl.inventory.Actor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionContext {
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
    private String submissionId;
    @NotNull
    private String submissionUid;
    private FlowStatus status;
    private Instant startTime;
    private Instant submissionCreationTime;
    private Instant deletedAt;
    @NotNull
    private String createdBy;
    private String lastModifiedBy;

    public Actor getOrgUnit() {
        return Actor.builder()
            .uid(orgUnitUid)
            .actorType("orgUnit")
            .build();
    }

    public Actor getTeam() {
        return Actor.builder()
            .uid(teamUid)
            .actorType("team")
            .build();
    }

    public Actor extractJsonField(String teamUid) {
        return Actor.builder().build();
    }
}
