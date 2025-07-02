/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nmcpye.datarun.acl;

import lombok.Getter;
import org.nmcpye.datarun.jpa.team.Team;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;

@Getter
public class TeamSid implements Sid {

    private final String team;

    public TeamSid(String team) {
        Assert.hasText(team, "Team required");
        this.team = team;
    }

    public TeamSid(Team team) {
        Assert.notNull(team, "Team required");
        Assert.notNull(team.getUid(), "Uid required");
        this.team = team.getUid();
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof TeamSid)) {
            return false;
        }
        return ((TeamSid) object).getTeam().equals(this.getTeam());
    }

    @Override
    public int hashCode() {
        return this.getTeam().hashCode();
    }

    @Override
    public String toString() {
        return "TeamSid[" + this.team + "]";
    }
}
