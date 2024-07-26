package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.domain.enumeration.MSessionSubject;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.springframework.data.domain.Persistable;

/**
 * A ChvSession.
 */
@Entity
@Table(name = "chv_session")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = { "new" })
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChvSession extends AbstractAuditingEntity<Long> implements Serializable, Persistable<Long> {

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

    @Column(name = "session_date")
    private Instant sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject")
    private MSessionSubject subject;

    @Column(name = "sessions")
    private Integer sessions;

    @Column(name = "people")
    private Integer people;

    @Column(name = "comment")
    private String comment;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "start_entry_time")
    private Instant startEntryTime;

    @Column(name = "finished_entry_time")
    private Instant finishedEntryTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SyncableStatus status;

    // Inherited createdBy definition
    // Inherited createdDate definition
    // Inherited lastModifiedBy definition
    // Inherited lastModifiedDate definition
    @Transient
    private boolean isPersisted;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "activity", "operationRoom", "warehouse", "userInfo", "assignments" }, allowSetters = true)
    private Team team;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Activity activity;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ChvSession id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public ChvSession uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public ChvSession code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public ChvSession name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getSessionDate() {
        return this.sessionDate;
    }

    public ChvSession sessionDate(Instant sessionDate) {
        this.setSessionDate(sessionDate);
        return this;
    }

    public void setSessionDate(Instant sessionDate) {
        this.sessionDate = sessionDate;
    }

    public MSessionSubject getSubject() {
        return this.subject;
    }

    public ChvSession subject(MSessionSubject subject) {
        this.setSubject(subject);
        return this;
    }

    public void setSubject(MSessionSubject subject) {
        this.subject = subject;
    }

    public Integer getSessions() {
        return this.sessions;
    }

    public ChvSession sessions(Integer sessions) {
        this.setSessions(sessions);
        return this;
    }

    public void setSessions(Integer sessions) {
        this.sessions = sessions;
    }

    public Integer getPeople() {
        return this.people;
    }

    public ChvSession people(Integer people) {
        this.setPeople(people);
        return this;
    }

    public void setPeople(Integer people) {
        this.people = people;
    }

    public String getComment() {
        return this.comment;
    }

    public ChvSession comment(String comment) {
        this.setComment(comment);
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public ChvSession deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getStartEntryTime() {
        return this.startEntryTime;
    }

    public ChvSession startEntryTime(Instant startEntryTime) {
        this.setStartEntryTime(startEntryTime);
        return this;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return this.finishedEntryTime;
    }

    public ChvSession finishedEntryTime(Instant finishedEntryTime) {
        this.setFinishedEntryTime(finishedEntryTime);
        return this;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public SyncableStatus getStatus() {
        return this.status;
    }

    public ChvSession status(SyncableStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SyncableStatus status) {
        this.status = status;
    }

    // Inherited createdBy methods
    public ChvSession createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public ChvSession createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public ChvSession lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public ChvSession lastModifiedDate(Instant lastModifiedDate) {
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

    public ChvSession setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public ChvSession team(Team team) {
        this.setTeam(team);
        return this;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public ChvSession activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChvSession)) {
            return false;
        }
        return getId() != null && getId().equals(((ChvSession) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChvSession{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", sessionDate='" + getSessionDate() + "'" +
            ", subject='" + getSubject() + "'" +
            ", sessions=" + getSessions() +
            ", people=" + getPeople() +
            ", comment='" + getComment() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
