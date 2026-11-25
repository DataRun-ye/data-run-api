package org.nmcpye.datarun.submission.normalized.jdbc;

/**
 * What a submission is tied to, as ids: its assignment, team, org unit and activity ({@link PlacementResolver}). The
 * model of placement is under review; this record and its resolver are the one place it lives.
 *
 * @param assignmentId the assignment's id, or {@code null} when the stored uid names no assignment
 * @param teamId       the team's id, or {@code null}
 * @param orgUnitId    the org unit's id, or {@code null}
 * @param activityId   the activity's id, or {@code null}
 */
public record Placement(String assignmentId, String teamId, String orgUnitId, String activityId) {

    /** Whether every stored uid named a row. */
    public boolean complete() {
        return assignmentId != null && teamId != null && orgUnitId != null && activityId != null;
    }
}
