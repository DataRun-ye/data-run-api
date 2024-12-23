package org.nmcpye.datarun.drun.postgres.common;

import java.util.HashSet;
import java.util.Set;

public class CurrentUserGroupInfo {

    private Long userId;

    private String userUID;

//    private Set<String> userGroupUIDs = new HashSet<>();

    private Set<String> teamUIDs = new HashSet<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    public Set<String> getTeamUIDs() {
        return teamUIDs;
    }

    public void setTeamUIDs(Set<String> teamUIDs) {
        this.teamUIDs = teamUIDs;
    }
}
