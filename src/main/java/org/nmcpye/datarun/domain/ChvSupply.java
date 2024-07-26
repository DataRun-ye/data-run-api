package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.domain.enumeration.DrugItemType;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;

/**
 * A ChvSupply.
 */
@Entity
@Table(name = "chv_supply")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChvSupply implements Serializable {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "item")
    private DrugItemType item;

    @Column(name = "previous_balance")
    private Integer previousBalance;

    @Column(name = "new_supply")
    private Integer newSupply;

    @Column(name = "consumed")
    private Integer consumed;

    @Column(name = "lost_corrupt")
    private Integer lostCorrupt;

    @Column(name = "remaining")
    private Integer remaining;

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

    public ChvSupply id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public ChvSupply uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public ChvSupply code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public ChvSupply name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DrugItemType getItem() {
        return this.item;
    }

    public ChvSupply item(DrugItemType item) {
        this.setItem(item);
        return this;
    }

    public void setItem(DrugItemType item) {
        this.item = item;
    }

    public Integer getPreviousBalance() {
        return this.previousBalance;
    }

    public ChvSupply previousBalance(Integer previousBalance) {
        this.setPreviousBalance(previousBalance);
        return this;
    }

    public void setPreviousBalance(Integer previousBalance) {
        this.previousBalance = previousBalance;
    }

    public Integer getNewSupply() {
        return this.newSupply;
    }

    public ChvSupply newSupply(Integer newSupply) {
        this.setNewSupply(newSupply);
        return this;
    }

    public void setNewSupply(Integer newSupply) {
        this.newSupply = newSupply;
    }

    public Integer getConsumed() {
        return this.consumed;
    }

    public ChvSupply consumed(Integer consumed) {
        this.setConsumed(consumed);
        return this;
    }

    public void setConsumed(Integer consumed) {
        this.consumed = consumed;
    }

    public Integer getLostCorrupt() {
        return this.lostCorrupt;
    }

    public ChvSupply lostCorrupt(Integer lostCorrupt) {
        this.setLostCorrupt(lostCorrupt);
        return this;
    }

    public void setLostCorrupt(Integer lostCorrupt) {
        this.lostCorrupt = lostCorrupt;
    }

    public Integer getRemaining() {
        return this.remaining;
    }

    public ChvSupply remaining(Integer remaining) {
        this.setRemaining(remaining);
        return this;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }

    public String getComment() {
        return this.comment;
    }

    public ChvSupply comment(String comment) {
        this.setComment(comment);
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public ChvSupply deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getStartEntryTime() {
        return this.startEntryTime;
    }

    public ChvSupply startEntryTime(Instant startEntryTime) {
        this.setStartEntryTime(startEntryTime);
        return this;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return this.finishedEntryTime;
    }

    public ChvSupply finishedEntryTime(Instant finishedEntryTime) {
        this.setFinishedEntryTime(finishedEntryTime);
        return this;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public SyncableStatus getStatus() {
        return this.status;
    }

    public ChvSupply status(SyncableStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SyncableStatus status) {
        this.status = status;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public ChvSupply activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public ChvSupply team(Team team) {
        this.setTeam(team);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChvSupply)) {
            return false;
        }
        return getId() != null && getId().equals(((ChvSupply) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChvSupply{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", item='" + getItem() + "'" +
            ", previousBalance=" + getPreviousBalance() +
            ", newSupply=" + getNewSupply() +
            ", consumed=" + getConsumed() +
            ", lostCorrupt=" + getLostCorrupt() +
            ", remaining=" + getRemaining() +
            ", comment='" + getComment() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }
}
