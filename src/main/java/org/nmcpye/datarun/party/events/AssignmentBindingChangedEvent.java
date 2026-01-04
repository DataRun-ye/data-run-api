package org.nmcpye.datarun.party.events;

import org.nmcpye.datarun.jpa.assignment.AssignmentPartyBinding;
import org.nmcpye.datarun.jpa.datasubmission.events.EventChangeType;

public record AssignmentBindingChangedEvent(AssignmentPartyBinding binding, EventChangeType type) {
}

