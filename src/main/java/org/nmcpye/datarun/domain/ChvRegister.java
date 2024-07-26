package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.domain.enumeration.Gender;
import org.nmcpye.datarun.domain.enumeration.MDetectionType;
import org.nmcpye.datarun.domain.enumeration.MSeverity;
import org.nmcpye.datarun.domain.enumeration.MTestResult;
import org.nmcpye.datarun.domain.enumeration.MTreatment;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.springframework.data.domain.Persistable;

/**
 * A ChvRegister.
 */
@Entity
@Table(name = "chv_register")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = { "new" })
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChvRegister extends AbstractAuditingEntity<Long> implements Serializable, Persistable<Long> {

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

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "visit_date")
    private Instant visitDate;

    @Column(name = "pregnant")
    private Boolean pregnant;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_result")
    private MTestResult testResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_type")
    private MDetectionType detectionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private MSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment")
    private MTreatment treatment;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "start_entry_time")
    private Instant startEntryTime;

    @Column(name = "finished_entry_time")
    private Instant finishedEntryTime;

    @Size(max = 2000)
    @Column(name = "comment", length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SyncableStatus status;

    // Inherited createdBy definition
    // Inherited createdDate definition
    // Inherited lastModifiedBy definition
    // Inherited lastModifiedDate definition
    @Transient
    private boolean isPersisted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "activity", "organisationUnit", "team", "warehouse" }, allowSetters = true)
    private Assignment location;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Activity activity;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "activity", "operationRoom", "warehouse", "userInfo", "assignments" }, allowSetters = true)
    private Team team;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ChvRegister id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public ChvRegister uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public ChvRegister code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public ChvRegister name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocationName() {
        return this.locationName;
    }

    public ChvRegister locationName(String locationName) {
        this.setLocationName(locationName);
        return this;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Integer getAge() {
        return this.age;
    }

    public ChvRegister age(Integer age) {
        this.setAge(age);
        return this;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Gender getGender() {
        return this.gender;
    }

    public ChvRegister gender(Gender gender) {
        this.setGender(gender);
        return this;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Instant getVisitDate() {
        return this.visitDate;
    }

    public ChvRegister visitDate(Instant visitDate) {
        this.setVisitDate(visitDate);
        return this;
    }

    public void setVisitDate(Instant visitDate) {
        this.visitDate = visitDate;
    }

    public Boolean getPregnant() {
        return this.pregnant;
    }

    public ChvRegister pregnant(Boolean pregnant) {
        this.setPregnant(pregnant);
        return this;
    }

    public void setPregnant(Boolean pregnant) {
        this.pregnant = pregnant;
    }

    public MTestResult getTestResult() {
        return this.testResult;
    }

    public ChvRegister testResult(MTestResult testResult) {
        this.setTestResult(testResult);
        return this;
    }

    public void setTestResult(MTestResult testResult) {
        this.testResult = testResult;
    }

    public MDetectionType getDetectionType() {
        return this.detectionType;
    }

    public ChvRegister detectionType(MDetectionType detectionType) {
        this.setDetectionType(detectionType);
        return this;
    }

    public void setDetectionType(MDetectionType detectionType) {
        this.detectionType = detectionType;
    }

    public MSeverity getSeverity() {
        return this.severity;
    }

    public ChvRegister severity(MSeverity severity) {
        this.setSeverity(severity);
        return this;
    }

    public void setSeverity(MSeverity severity) {
        this.severity = severity;
    }

    public MTreatment getTreatment() {
        return this.treatment;
    }

    public ChvRegister treatment(MTreatment treatment) {
        this.setTreatment(treatment);
        return this;
    }

    public void setTreatment(MTreatment treatment) {
        this.treatment = treatment;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public ChvRegister deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getStartEntryTime() {
        return this.startEntryTime;
    }

    public ChvRegister startEntryTime(Instant startEntryTime) {
        this.setStartEntryTime(startEntryTime);
        return this;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return this.finishedEntryTime;
    }

    public ChvRegister finishedEntryTime(Instant finishedEntryTime) {
        this.setFinishedEntryTime(finishedEntryTime);
        return this;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public String getComment() {
        return this.comment;
    }

    public ChvRegister comment(String comment) {
        this.setComment(comment);
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public SyncableStatus getStatus() {
        return this.status;
    }

    public ChvRegister status(SyncableStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SyncableStatus status) {
        this.status = status;
    }

    // Inherited createdBy methods
    public ChvRegister createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public ChvRegister createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public ChvRegister lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public ChvRegister lastModifiedDate(Instant lastModifiedDate) {
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

    public ChvRegister setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public Assignment getLocation() {
        return this.location;
    }

    public void setLocation(Assignment assignment) {
        this.location = assignment;
    }

    public ChvRegister location(Assignment assignment) {
        this.setLocation(assignment);
        return this;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public ChvRegister activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public ChvRegister team(Team team) {
        this.setTeam(team);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChvRegister)) {
            return false;
        }
        return getId() != null && getId().equals(((ChvRegister) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChvRegister{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", locationName='" + getLocationName() + "'" +
            ", age=" + getAge() +
            ", gender='" + getGender() + "'" +
            ", visitDate='" + getVisitDate() + "'" +
            ", pregnant='" + getPregnant() + "'" +
            ", testResult='" + getTestResult() + "'" +
            ", detectionType='" + getDetectionType() + "'" +
            ", severity='" + getSeverity() + "'" +
            ", treatment='" + getTreatment() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", comment='" + getComment() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
