package org.nmcpye.datarun.party.events;

import org.nmcpye.datarun.jpa.assignment.AssignmentRolePartyPolicy;
import org.nmcpye.datarun.jpa.datasubmission.events.EventChangeType;

public record AssignmentBindingChangedEvent(AssignmentRolePartyPolicy binding, EventChangeType type) {
}

