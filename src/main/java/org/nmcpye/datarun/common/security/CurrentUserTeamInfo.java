package org.nmcpye.datarun.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * @author Hamza, 20/03/2025
 */
@AllArgsConstructor
@Getter
@Builder
public class CurrentUserTeamInfo {
    private Long userId;

    private String userUID;

    private Set<String> teamUIDs;

    private Set<String> managedTeamUIDs;
}
