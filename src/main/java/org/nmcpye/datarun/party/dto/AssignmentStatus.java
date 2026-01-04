package org.nmcpye.datarun.party.dto;

import org.nmcpye.datarun.common.enumeration.FlowStatus;

public enum AssignmentStatus {
    PLANNED, // SCHEDULED
    IN_PROGRESS,
    DONE,
    RESCHEDULED,
    MERGED,
    REASSIGNED,
    CANCELLED,
    EXPIRED;

    public static AssignmentStatus getAssignmentStatus(FlowStatus flowStatus) {
        for (AssignmentStatus status : AssignmentStatus.values()) {
            if (status.name().equalsIgnoreCase(flowStatus.name())) {
                return status;
            }
        }

        return null;
    }

    public static FlowStatus getAssignmentStatus(AssignmentStatus flowStatus) {
        for (FlowStatus status : FlowStatus.values()) {
            if (status.name().equalsIgnoreCase(flowStatus.name())) {
                return status;
            }
        }

        return null;
    }
}
