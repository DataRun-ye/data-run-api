package org.nmcpye.datarun.party.events;

import org.nmcpye.datarun.jpa.user.User;

public record UserSavedEvent(User user) {
}
