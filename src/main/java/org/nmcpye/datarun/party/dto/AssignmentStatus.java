package org.nmcpye.datarun.party.dto;

import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.DataType;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;

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
}
