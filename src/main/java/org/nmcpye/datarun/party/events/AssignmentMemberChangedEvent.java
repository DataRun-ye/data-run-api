package org.nmcpye.datarun.party.events;

import org.nmcpye.datarun.jpa.assignment.AssignmentMember;

public record AssignmentMemberChangedEvent(AssignmentMember member) {
}
