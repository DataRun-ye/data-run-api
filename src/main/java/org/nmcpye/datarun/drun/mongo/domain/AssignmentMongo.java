package org.nmcpye.datarun.drun.mongo.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.AbstractAuditingEntity;
import org.nmcpye.datarun.drun.mongo.mapping.assignment.ActivityDeserializer;
import org.nmcpye.datarun.drun.mongo.mapping.assignment.OrgUnitDeserializer;
import org.nmcpye.datarun.drun.mongo.mapping.assignment.TeamDeserializer;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * A Assignment.
 */
@Document(collection = "assignment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssignmentMongo extends AbstractAuditingEntity<String> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Size(max = 11)
    @Field("uid")
    private String uid;

    @Field("code")
    private String code;

    @NotNull
    @Size(max = 11)
    @Field("activity")
    @JsonDeserialize(using = ActivityDeserializer.class)
    private String activity;

    @Size(max = 11)
    @Field("team")
    @JsonDeserialize(using = TeamDeserializer.class)
    private String team;

    @Size(max = 11)
    @Field("org_unit")
    @JsonDeserialize(using = OrgUnitDeserializer.class)
    private String orgUnit;

    @Field("disabled")
    private Boolean disabled;

    private Map<String, Object> properties;

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

// jhipster-needle-entity-add-field - JHipster will add fields here

    public String getId() {
        return this.id;
    }

    public AssignmentMongo id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public AssignmentMongo uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public AssignmentMongo code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getDisabled() {
        return this.disabled;
    }

    public AssignmentMongo disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public String getActivity() {
        return this.activity;
    }

    public AssignmentMongo activity(String activity) {
        this.setActivity(activity);
        return this;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getTeam() {
        return this.team;
    }

    public AssignmentMongo team(String team) {
        this.setTeam(team);
        return this;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getOrgUnit() {
        return this.orgUnit;
    }

    public AssignmentMongo orgUnit(String orgUnit) {
        this.setOrgUnit(orgUnit);
        return this;
    }

    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    // Inherited createdBy methods
    public AssignmentMongo createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public AssignmentMongo createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public AssignmentMongo lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public AssignmentMongo lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssignmentMongo)) {
            return false;
        }
        return getId() != null && getId().equals(((AssignmentMongo) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Assignment{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", disabled='" + getDisabled() + "'" +
            ", activity='" + getActivity() + "'" +
            ", team='" + getTeam() + "'" +
            ", orgUnit='" + getOrgUnit() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
