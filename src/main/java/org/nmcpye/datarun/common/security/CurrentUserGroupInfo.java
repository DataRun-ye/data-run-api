package org.nmcpye.datarun.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Hamza Assada, 20/03/2025
 */
@AllArgsConstructor
@Getter
@Builder
public class CurrentUserGroupInfo {
    private Long userId;

    private String userUID;

    private Set<String> userGroupUIDs = new HashSet<>();
}
