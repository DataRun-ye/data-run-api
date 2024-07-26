package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.springframework.data.domain.Persistable;

/**
 * A WarehouseTransaction.
 */
@Entity
@Table(name = "warehouse_transaction")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = { "new" })
@SuppressWarnings("common-java:DuplicatedBlocks")
public class WarehouseTransaction extends AbstractAuditingEntity<Long> implements Serializable, Persistable<Long> {

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

    @Column(name = "imov_uid", unique = true)
    private String imovUid;

    @Column(name = "transaction_date")
    private Instant transactionDate;

    @Column(name = "phase_no")
    private Integer phaseNo;

    @Column(name = "entry_type")
    private String entryType;

    @Min(value = 0)
    @Column(name = "quantity")
    private Integer quantity;

    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;

    @Size(max = 2000)
    @Column(name = "person_name", length = 2000)
    private String personName;

    @Column(name = "work_day_id")
    private Integer workDayId;

    @Column(name = "submission_time")
    private Instant submissionTime;

    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "submission_uuid")
    private String submissionUuid;

    @Column(name = "start_entry_time")
    private Instant startEntryTime;

    @Column(name = "finished_entry_time")
    private Instant finishedEntryTime;

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
    private WarehouseItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "activity" }, allowSetters = true)
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "activity", "operationRoom", "warehouse", "userInfo", "assignments" }, allowSetters = true)
    private Team team;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "activity" }, allowSetters = true)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Activity activity;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public WarehouseTransaction id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public WarehouseTransaction uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public WarehouseTransaction code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public WarehouseTransaction name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImovUid() {
        return this.imovUid;
    }

    public WarehouseTransaction imovUid(String imovUid) {
        this.setImovUid(imovUid);
        return this;
    }

    public void setImovUid(String imovUid) {
        this.imovUid = imovUid;
    }

    public Instant getTransactionDate() {
        return this.transactionDate;
    }

    public WarehouseTransaction transactionDate(Instant transactionDate) {
        this.setTransactionDate(transactionDate);
        return this;
    }

    public void setTransactionDate(Instant transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Integer getPhaseNo() {
        return this.phaseNo;
    }

    public WarehouseTransaction phaseNo(Integer phaseNo) {
        this.setPhaseNo(phaseNo);
        return this;
    }

    public void setPhaseNo(Integer phaseNo) {
        this.phaseNo = phaseNo;
    }

    public String getEntryType() {
        return this.entryType;
    }

    public WarehouseTransaction entryType(String entryType) {
        this.setEntryType(entryType);
        return this;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public WarehouseTransaction quantity(Integer quantity) {
        this.setQuantity(quantity);
        return this;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return this.notes;
    }

    public WarehouseTransaction notes(String notes) {
        this.setNotes(notes);
        return this;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPersonName() {
        return this.personName;
    }

    public WarehouseTransaction personName(String personName) {
        this.setPersonName(personName);
        return this;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public Integer getWorkDayId() {
        return this.workDayId;
    }

    public WarehouseTransaction workDayId(Integer workDayId) {
        this.setWorkDayId(workDayId);
        return this;
    }

    public void setWorkDayId(Integer workDayId) {
        this.workDayId = workDayId;
    }

    public Instant getSubmissionTime() {
        return this.submissionTime;
    }

    public WarehouseTransaction submissionTime(Instant submissionTime) {
        this.setSubmissionTime(submissionTime);
        return this;
    }

    public void setSubmissionTime(Instant submissionTime) {
        this.submissionTime = submissionTime;
    }

    public Long getSubmissionId() {
        return this.submissionId;
    }

    public WarehouseTransaction submissionId(Long submissionId) {
        this.setSubmissionId(submissionId);
        return this;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public WarehouseTransaction deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getSubmissionUuid() {
        return this.submissionUuid;
    }

    public WarehouseTransaction submissionUuid(String submissionUuid) {
        this.setSubmissionUuid(submissionUuid);
        return this;
    }

    public void setSubmissionUuid(String submissionUuid) {
        this.submissionUuid = submissionUuid;
    }

    public Instant getStartEntryTime() {
        return this.startEntryTime;
    }

    public WarehouseTransaction startEntryTime(Instant startEntryTime) {
        this.setStartEntryTime(startEntryTime);
        return this;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return this.finishedEntryTime;
    }

    public WarehouseTransaction finishedEntryTime(Instant finishedEntryTime) {
        this.setFinishedEntryTime(finishedEntryTime);
        return this;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public SyncableStatus getStatus() {
        return this.status;
    }

    public WarehouseTransaction status(SyncableStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SyncableStatus status) {
        this.status = status;
    }

    // Inherited createdBy methods
    public WarehouseTransaction createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public WarehouseTransaction createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public WarehouseTransaction lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public WarehouseTransaction lastModifiedDate(Instant lastModifiedDate) {
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

    public WarehouseTransaction setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public WarehouseItem getItem() {
        return this.item;
    }

    public void setItem(WarehouseItem warehouseItem) {
        this.item = warehouseItem;
    }

    public WarehouseTransaction item(WarehouseItem warehouseItem) {
        this.setItem(warehouseItem);
        return this;
    }

    public Warehouse getSourceWarehouse() {
        return this.sourceWarehouse;
    }

    public void setSourceWarehouse(Warehouse warehouse) {
        this.sourceWarehouse = warehouse;
    }

    public WarehouseTransaction sourceWarehouse(Warehouse warehouse) {
        this.setSourceWarehouse(warehouse);
        return this;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public WarehouseTransaction team(Team team) {
        this.setTeam(team);
        return this;
    }

    public Warehouse getWarehouse() {
        return this.warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public WarehouseTransaction warehouse(Warehouse warehouse) {
        this.setWarehouse(warehouse);
        return this;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public WarehouseTransaction activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WarehouseTransaction)) {
            return false;
        }
        return getId() != null && getId().equals(((WarehouseTransaction) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "WarehouseTransaction{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", imovUid='" + getImovUid() + "'" +
            ", transactionDate='" + getTransactionDate() + "'" +
            ", phaseNo=" + getPhaseNo() +
            ", entryType='" + getEntryType() + "'" +
            ", quantity=" + getQuantity() +
            ", notes='" + getNotes() + "'" +
            ", personName='" + getPersonName() + "'" +
            ", workDayId=" + getWorkDayId() +
            ", submissionTime='" + getSubmissionTime() + "'" +
            ", submissionId=" + getSubmissionId() +
            ", deleted='" + getDeleted() + "'" +
            ", submissionUuid='" + getSubmissionUuid() + "'" +
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
