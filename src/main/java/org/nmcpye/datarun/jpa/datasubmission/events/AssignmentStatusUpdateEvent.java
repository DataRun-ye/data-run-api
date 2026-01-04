package org.nmcpye.datarun.jpa.datasubmission.events;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * @author Hamza Assada
 * @since 14/08/2025
 */
public class AssignmentStatusUpdateEvent implements Serializable {
    private final String submissionId;   // DataSubmission.id (ULID)
    private final EventChangeType changeType;
    private final Integer submissionVersion;
    private final Instant occurredAt;

    public AssignmentStatusUpdateEvent(String submissionId, EventChangeType changeType, Integer submissionVersion) {
        this.submissionId = submissionId;
        this.changeType = changeType;
        this.submissionVersion = submissionVersion;
        this.occurredAt = Instant.now();
    }

    public String getSubmissionId() { return submissionId; }
    public EventChangeType getChangeType() { return changeType; }
    public Integer getSubmissionVersion() { return submissionVersion; }
    public Instant getOccurredAt() { return occurredAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentStatusUpdateEvent)) return false;
        AssignmentStatusUpdateEvent that = (AssignmentStatusUpdateEvent) o;
        return Objects.equals(submissionId, that.submissionId)
            && changeType == that.changeType
            && Objects.equals(submissionVersion, that.submissionVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submissionId, changeType, submissionVersion);
    }
}
