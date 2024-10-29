package org.nmcpye.datarun.drun.mongo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.service.dto.UserDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Objects;


/**
 * A Assignment.
 */
@Document(collection = "team_users")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TeamUser
    implements Serializable, Identifiable<String> {

    private static final String PATH_SEP = ",";

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Size(max = 11)
    @Field("uid")
    private String uid;

    @DBRef
    @Field("team")
    @NotNull
    @JsonIgnoreProperties(value = {"teamUsers"}, allowSetters = true)
    private TeamMongo team;

    @NotNull
    @Field("user")
    private UserDTO user;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public void setUid(String uid) {
        this.uid = uid;
    }

    public TeamMongo getTeam() {
        return team;
    }

    public void setTeam(TeamMongo team) {
        this.team = team;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeamUser teamUser)) return false;
        return Objects.equals(getUid(), teamUser.getUid()) && Objects.equals(getTeam(), teamUser.getTeam()) && Objects.equals(getUser(), teamUser.getUser());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUid(), getTeam(), getUser());
    }
}
