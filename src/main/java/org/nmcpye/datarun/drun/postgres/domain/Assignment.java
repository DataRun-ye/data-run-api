package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.domain.AbstractAuditingEntity;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;

/**
 * A Assignment.
 */
@Entity
@Table(name = "assignment", uniqueConstraints = {
    @UniqueConstraint(name = "uc_assignment_activity_id", columnNames = {"activity_id", "orgUnit_id", "team_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Assignment extends AbstractAuditingEntity<Long> implements Serializable, Persistable<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    private String uid;

    @Column(name = "code")
    private String code;

    @Transient
    private boolean isPersisted;

    @NotNull
    @ManyToOne//(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"project", "translations"}, allowSetters = true)
    private Activity activity;

//    @JsonSerialize(contentAs = IdentifiableObject.class)
    @JsonIgnoreProperties(value = {"parent", "children", "groups","assignments", "hierarchyLevel", "ancestors", "translations"}, allowSetters = true)
    private OrgUnit orgUnit;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"activity", "operationRoom", "warehouse", "userInfo", "assignments", "users", "ancestors", "translations"}, allowSetters = true)
//    @JsonSerialize(contentAs = IdentifiableObject.class)
    private Team team;

    public Long getId() {
        return this.id;
    }

    public Assignment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public Assignment uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public Assignment code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return "";
    }

    // Inherited createdBy methods
    public Assignment createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public Assignment createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public Assignment lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public Assignment lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    @PostLoad
    @PostPersist
    public void updateEntityState() {
        this.setIsPersisted();
    }

    @Transient
    @Override
    public boolean isNew() {
        return !this.isPersisted;
    }

    public Assignment setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    @JsonSerialize(contentAs = IdentifiableObject.class)
    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Assignment activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    @JsonSerialize(contentAs = IdentifiableObject.class)
    public OrgUnit getOrgUnit() {
        return this.orgUnit;
    }

    public void setOrgUnit(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }

    public Assignment organisationUnit(OrgUnit orgUnit) {
        this.setOrgUnit(orgUnit);
        return this;
    }

    @JsonSerialize(contentAs = IdentifiableObject.class)
    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Assignment team(Team team) {
        this.setTeam(team);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Assignment)) {
            return false;
        }
        return getId() != null && getId().equals(((Assignment) o).getId());
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
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
