package org.nmcpye.datarun.mongo.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.AbstractAuditingEntity;
import org.nmcpye.datarun.mongo.mapping.serialization.ActivityDeserializer;
import org.nmcpye.datarun.mongo.mapping.serialization.OrgUnitDeserializer;
import org.nmcpye.datarun.mongo.mapping.serialization.TeamDeserializer;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * A Assignment.
 */
@Document(collection = "assignment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssignmentMongo
    extends AbstractAuditingEntity<String> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Size(max = 11)
    @Field("uid")
    private String uid;

    @NotNull
    @Size(max = 11)
    @Field("activity")
    @JsonDeserialize(using = ActivityDeserializer.class)
    private String activity;

    @Size(max = 11)
    @Field("team")
    @JsonDeserialize(using = TeamDeserializer.class)
    private String team;
//
//    @DBRef
//    @Field("team")
//    private TeamMongo team;


    @Size(max = 11)
    @Field("org_unit")
    @JsonDeserialize(using = OrgUnitDeserializer.class)
    private String orgUnit;

    private Map<String, Object> properties;

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

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

//    public TeamMongo getTeam() {
//        return team;
//    }
//
//    public void setTeam(TeamMongo team) {
//        this.team = team;
//    }

    public String getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof AssignmentMongo that)) return false;
//        return Objects.equals(getUid(), that.getUid()) && Objects.equals(getCode(), that.getCode()) && Objects.equals(getActivity(), that.getActivity()) && Objects.equals(getTeam(), that.getTeam()) && Objects.equals(getOrgUnit(), that.getOrgUnit()) && Objects.equals(getProperties(), that.getProperties());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(super.hashCode(), getUid(), getCode(), getActivity(), getTeam(), getOrgUnit(), getProperties());
//    }
}
