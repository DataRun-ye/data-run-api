package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.WarehouseTransaction} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class WarehouseTransactionDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

    @NotNull
    private String imovUid;

    @NotNull
    private Instant transactionDate;

    private Integer phaseNo;

    @NotNull
    private String entryType;

    @NotNull
    @Min(value = 0)
    private Integer quantity;

    @Size(max = 2000)
    private String notes;

    @Size(max = 2000)
    private String personName;

    private Integer workDayId;

    private Instant submissionTime;

    private Long submissionId;

    private Boolean deleted;

    private String submissionUuid;

    private WarehouseItemDTO item;

    private WarehouseDTO sourceWarehouse;

    private TeamDTO team;

    @NotNull
    private WarehouseDTO warehouse;

    private ActivityDTO activity;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImovUid() {
        return imovUid;
    }

    public void setImovUid(String imovUid) {
        this.imovUid = imovUid;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Instant transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Integer getPhaseNo() {
        return phaseNo;
    }

    public void setPhaseNo(Integer phaseNo) {
        this.phaseNo = phaseNo;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public Integer getWorkDayId() {
        return workDayId;
    }

    public void setWorkDayId(Integer workDayId) {
        this.workDayId = workDayId;
    }

    public Instant getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(Instant submissionTime) {
        this.submissionTime = submissionTime;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getSubmissionUuid() {
        return submissionUuid;
    }

    public void setSubmissionUuid(String submissionUuid) {
        this.submissionUuid = submissionUuid;
    }

    public WarehouseItemDTO getItem() {
        return item;
    }

    public void setItem(WarehouseItemDTO item) {
        this.item = item;
    }

    public WarehouseDTO getSourceWarehouse() {
        return sourceWarehouse;
    }

    public void setSourceWarehouse(WarehouseDTO sourceWarehouse) {
        this.sourceWarehouse = sourceWarehouse;
    }

    public TeamDTO getTeam() {
        return team;
    }

    public void setTeam(TeamDTO team) {
        this.team = team;
    }

    public WarehouseDTO getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(WarehouseDTO warehouse) {
        this.warehouse = warehouse;
    }

    public ActivityDTO getActivity() {
        return activity;
    }

    public void setActivity(ActivityDTO activity) {
        this.activity = activity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WarehouseTransactionDTO)) {
            return false;
        }

        WarehouseTransactionDTO warehouseTransactionDTO = (WarehouseTransactionDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, warehouseTransactionDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "WarehouseTransactionDTO{" +
            "uid=" + getUid() +
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
            ", item=" + getItem() +
            ", sourceWarehouse=" + getSourceWarehouse() +
            ", team=" + getTeam() +
            ", warehouse=" + getWarehouse() +
            ", activity=" + getActivity() +
            "}";
    }
}
