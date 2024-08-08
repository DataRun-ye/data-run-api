package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.domain.Persistable;

/**
 * A Team.
 */
@Entity
@Table(name = "team")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = { "new" })
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Team extends AbstractAuditingEntity<Long> implements Serializable, Persistable<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    private String uid;

    @NotNull
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "workers")
    private Integer workers;

    @Column(name = "mobility")
    private String mobility;

    @Column(name = "disabled")
    private Boolean disabled;

    @Column(name = "delete_client_data")
    private Boolean deleteClientData;

    // Inherited createdBy definition
    // Inherited createdDate definition
    // Inherited lastModifiedBy definition
    // Inherited lastModifiedDate definition
    @Transient
    private boolean isPersisted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "activity" }, allowSetters = true)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    private User userInfo;
//
//    @OneToMany(fetch = FetchType.LAZY, mappedBy = "team")
//    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//    @JsonIgnoreProperties(value = { "activity", "organisationUnit", "team", "warehouse" }, allowSetters = true)
//    private Set<Assignment> assignments = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Team id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public Team uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public Team code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public Team name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public Team description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMobile() {
        return this.mobile;
    }

    public Team mobile(String mobile) {
        this.setMobile(mobile);
        return this;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getWorkers() {
        return this.workers;
    }

    public Team workers(Integer workers) {
        this.setWorkers(workers);
        return this;
    }

    public void setWorkers(Integer workers) {
        this.workers = workers;
    }

    public String getMobility() {
        return this.mobility;
    }

    public Team mobility(String mobility) {
        this.setMobility(mobility);
        return this;
    }

    public void setMobility(String mobility) {
        this.mobility = mobility;
    }

    public Boolean getDisabled() {
        return this.disabled;
    }

    public Team disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getDeleteClientData() {
        return this.deleteClientData;
    }

    public Team deleteClientData(Boolean deleteClientData) {
        this.setDeleteClientData(deleteClientData);
        return this;
    }

    public void setDeleteClientData(Boolean deleteClientData) {
        this.deleteClientData = deleteClientData;
    }

    // Inherited createdBy methods
    public Team createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public Team createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public Team lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public Team lastModifiedDate(Instant lastModifiedDate) {
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

    public Team setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Team activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    public Warehouse getWarehouse() {
        return this.warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Team warehouse(Warehouse warehouse) {
        this.setWarehouse(warehouse);
        return this;
    }

    public User getUserInfo() {
        return this.userInfo;
    }

    public void setUserInfo(User user) {
        this.userInfo = user;
    }

    public Team userInfo(User user) {
        this.setUserInfo(user);
        return this;
    }

//    public Set<Assignment> getAssignments() {
//        return this.assignments;
//    }
//
//    public void setAssignments(Set<Assignment> assignments) {
//        if (this.assignments != null) {
//            this.assignments.forEach(i -> i.setTeam(null));
//        }
//        if (assignments != null) {
//            assignments.forEach(i -> i.setTeam(this));
//        }
//        this.assignments = assignments;
//    }
//
//    public Team assignments(Set<Assignment> assignments) {
//        this.setAssignments(assignments);
//        return this;
//    }
//
//    public Team addAssignment(Assignment assignment) {
//        this.assignments.add(assignment);
//        assignment.setTeam(this);
//        return this;
//    }
//
//    public Team removeAssignment(Assignment assignment) {
//        this.assignments.remove(assignment);
//        assignment.setTeam(null);
//        return this;
//    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Team)) {
            return false;
        }
        return getId() != null && getId().equals(((Team) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Team{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", mobile='" + getMobile() + "'" +
            ", workers=" + getWorkers() +
            ", mobility='" + getMobility() + "'" +
            ", disabled='" + getDisabled() + "'" +
            ", deleteClientData='" + getDeleteClientData() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
