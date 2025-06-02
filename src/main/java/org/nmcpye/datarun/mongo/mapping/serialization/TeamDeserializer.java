package org.nmcpye.datarun.mongo.mapping.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.service.TeamService;

import java.io.IOException;

public class TeamDeserializer extends JsonDeserializer<Team> {

    final private TeamService teamService;  // or a repository to fetch Team by UID

    public TeamDeserializer(TeamService teamService) {
        this.teamService = teamService;
    }

    @Override
    public Team deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String uid = p.getValueAsString();
        return teamService.findByUid(uid).orElseThrow();  // Fetch Team object using the UID
    }
}
