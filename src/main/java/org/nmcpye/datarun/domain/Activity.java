package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A Activity.
 */
@Entity
@Table(name = "activity")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Activity extends AbstractAuditingEntity<Long> implements Serializable, Persistable<Long> {

    private static final long serialVersionUID = 1L;
    private static final String PATH_SEP = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, unique = true)
    private String uid;

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "disabled")
    private Boolean disabled;

    @Column(name = "delete_client_data")
    private Boolean deleteClientData;

    @Transient
    private boolean isPersisted;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"activities"}, allowSetters = true)
    private Project project;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "activity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"activity", "team", "orgUnit", "parent", "children", "ancestors", "level", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    private Set<Assignment> assignments = new HashSet<>();

    public Long getId() {
        return this.id;
    }

    public Activity id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public Activity uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public Activity code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public Activity name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getStartDate() {
        return this.startDate;
    }

    public Activity startDate(Instant startDate) {
        this.setStartDate(startDate);
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return this.endDate;
    }

    public Activity endDate(Instant endDate) {
        this.setEndDate(endDate);
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Boolean getDisabled() {
        return this.disabled;
    }

    public Activity disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getDeleteClientData() {
        return this.deleteClientData;
    }

    public Activity deleteClientData(Boolean deleteClientData) {
        this.setDeleteClientData(deleteClientData);
        return this;
    }

    public void setDeleteClientData(Boolean deleteClientData) {
        this.deleteClientData = deleteClientData;
    }

    public Activity createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    public Activity createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public Activity lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public Activity lastModifiedDate(Instant lastModifiedDate) {
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

    public Activity setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public Project getProject() {
        return this.project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Activity project(Project project) {
        this.setProject(project);
        return this;
    }

    public Set<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(Set<Assignment> assignments) {
        this.assignments = assignments;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Activity{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            ", disabled='" + getDisabled() + "'" +
            ", deleteClientData='" + getDeleteClientData() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
