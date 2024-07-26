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
import org.nmcpye.datarun.domain.enumeration.SettlementEnum;
import org.nmcpye.datarun.domain.enumeration.SurveyTypeEnum;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.springframework.data.domain.Persistable;

/**
 * A ItnsVillage.
 */
@Entity
@Table(name = "itns_village")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = { "new" })
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ItnsVillage extends AbstractAuditingEntity<Long> implements Serializable, Persistable<Long> {

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

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "submission_uuid", unique = true)
    private String submissionUuid;

    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "work_day_date")
    private Instant workDayDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "surveytype")
    private SurveyTypeEnum surveytype;

    @Size(max = 2000)
    @Column(name = "other_reason_comment", length = 2000)
    private String otherReasonComment;

    @Size(max = 2000)
    @Column(name = "reason_notcomplete", length = 2000)
    private String reasonNotcomplete;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement")
    private SettlementEnum settlement;

    @Size(max = 2000)
    @Column(name = "settlement_name", length = 2000)
    private String settlementName;

    @Size(max = 2000)
    @Column(name = "tl_commenet", length = 2000)
    private String tlCommenet;

    @Column(name = "time_spent_hours")
    private Integer timeSpentHours;

    @Column(name = "time_spent_minutes")
    private Integer timeSpentMinutes;

    @Size(max = 2000)
    @Column(name = "difficulties", length = 2000)
    private String difficulties;

    @Column(name = "location_captured")
    private String locationCaptured;

    @Column(name = "location_capture_time")
    private Instant locationCaptureTime;

    @Column(name = "ho_proof")
    private String hoProof;

    @Column(name = "ho_proof_url")
    private String hoProofUrl;

    @Column(name = "submission_time")
    private Instant submissionTime;

    @Column(name = "untargeting_other_specify")
    private String untargetingOtherSpecify;

    @Size(max = 2000)
    @Column(name = "other_village_name", length = 2000)
    private String otherVillageName;

    @Size(max = 2000)
    @Column(name = "other_village_code", length = 2000)
    private String otherVillageCode;

    @Column(name = "other_team_no")
    private Long otherTeamNo;

    @Column(name = "start_entry_time")
    private Instant startEntryTime;

    @Column(name = "finished_entry_time")
    private Instant finishedEntryTime;

    @Column(name = "deleted")
    private Boolean deleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SyncableStatus status;

    // Inherited createdBy definition
    // Inherited createdDate definition
    // Inherited lastModifiedBy definition
    // Inherited lastModifiedDate definition
    @Transient
    private boolean isPersisted;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "itnsVillage")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "itnsVillage" }, allowSetters = true)
    private Set<ItnsVillageHousesDetail> houseDetails = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private ProgressStatus progressStatus;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "activity", "operationRoom", "warehouse", "userInfo", "assignments" }, allowSetters = true)
    private Team team;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "activity", "organisationUnit", "team", "warehouse" }, allowSetters = true)
    private Assignment assignment;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Activity activity;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ItnsVillage id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public ItnsVillage uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public ItnsVillage code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public ItnsVillage name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubmissionUuid() {
        return this.submissionUuid;
    }

    public ItnsVillage submissionUuid(String submissionUuid) {
        this.setSubmissionUuid(submissionUuid);
        return this;
    }

    public void setSubmissionUuid(String submissionUuid) {
        this.submissionUuid = submissionUuid;
    }

    public Long getSubmissionId() {
        return this.submissionId;
    }

    public ItnsVillage submissionId(Long submissionId) {
        this.setSubmissionId(submissionId);
        return this;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Instant getWorkDayDate() {
        return this.workDayDate;
    }

    public ItnsVillage workDayDate(Instant workDayDate) {
        this.setWorkDayDate(workDayDate);
        return this;
    }

    public void setWorkDayDate(Instant workDayDate) {
        this.workDayDate = workDayDate;
    }

    public SurveyTypeEnum getSurveytype() {
        return this.surveytype;
    }

    public ItnsVillage surveytype(SurveyTypeEnum surveytype) {
        this.setSurveytype(surveytype);
        return this;
    }

    public void setSurveytype(SurveyTypeEnum surveytype) {
        this.surveytype = surveytype;
    }

    public String getOtherReasonComment() {
        return this.otherReasonComment;
    }

    public ItnsVillage otherReasonComment(String otherReasonComment) {
        this.setOtherReasonComment(otherReasonComment);
        return this;
    }

    public void setOtherReasonComment(String otherReasonComment) {
        this.otherReasonComment = otherReasonComment;
    }

    public String getReasonNotcomplete() {
        return this.reasonNotcomplete;
    }

    public ItnsVillage reasonNotcomplete(String reasonNotcomplete) {
        this.setReasonNotcomplete(reasonNotcomplete);
        return this;
    }

    public void setReasonNotcomplete(String reasonNotcomplete) {
        this.reasonNotcomplete = reasonNotcomplete;
    }

    public SettlementEnum getSettlement() {
        return this.settlement;
    }

    public ItnsVillage settlement(SettlementEnum settlement) {
        this.setSettlement(settlement);
        return this;
    }

    public void setSettlement(SettlementEnum settlement) {
        this.settlement = settlement;
    }

    public String getSettlementName() {
        return this.settlementName;
    }

    public ItnsVillage settlementName(String settlementName) {
        this.setSettlementName(settlementName);
        return this;
    }

    public void setSettlementName(String settlementName) {
        this.settlementName = settlementName;
    }

    public String getTlCommenet() {
        return this.tlCommenet;
    }

    public ItnsVillage tlCommenet(String tlCommenet) {
        this.setTlCommenet(tlCommenet);
        return this;
    }

    public void setTlCommenet(String tlCommenet) {
        this.tlCommenet = tlCommenet;
    }

    public Integer getTimeSpentHours() {
        return this.timeSpentHours;
    }

    public ItnsVillage timeSpentHours(Integer timeSpentHours) {
        this.setTimeSpentHours(timeSpentHours);
        return this;
    }

    public void setTimeSpentHours(Integer timeSpentHours) {
        this.timeSpentHours = timeSpentHours;
    }

    public Integer getTimeSpentMinutes() {
        return this.timeSpentMinutes;
    }

    public ItnsVillage timeSpentMinutes(Integer timeSpentMinutes) {
        this.setTimeSpentMinutes(timeSpentMinutes);
        return this;
    }

    public void setTimeSpentMinutes(Integer timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public String getDifficulties() {
        return this.difficulties;
    }

    public ItnsVillage difficulties(String difficulties) {
        this.setDifficulties(difficulties);
        return this;
    }

    public void setDifficulties(String difficulties) {
        this.difficulties = difficulties;
    }

    public String getLocationCaptured() {
        return this.locationCaptured;
    }

    public ItnsVillage locationCaptured(String locationCaptured) {
        this.setLocationCaptured(locationCaptured);
        return this;
    }

    public void setLocationCaptured(String locationCaptured) {
        this.locationCaptured = locationCaptured;
    }

    public Instant getLocationCaptureTime() {
        return this.locationCaptureTime;
    }

    public ItnsVillage locationCaptureTime(Instant locationCaptureTime) {
        this.setLocationCaptureTime(locationCaptureTime);
        return this;
    }

    public void setLocationCaptureTime(Instant locationCaptureTime) {
        this.locationCaptureTime = locationCaptureTime;
    }

    public String getHoProof() {
        return this.hoProof;
    }

    public ItnsVillage hoProof(String hoProof) {
        this.setHoProof(hoProof);
        return this;
    }

    public void setHoProof(String hoProof) {
        this.hoProof = hoProof;
    }

    public String getHoProofUrl() {
        return this.hoProofUrl;
    }

    public ItnsVillage hoProofUrl(String hoProofUrl) {
        this.setHoProofUrl(hoProofUrl);
        return this;
    }

    public void setHoProofUrl(String hoProofUrl) {
        this.hoProofUrl = hoProofUrl;
    }

    public Instant getSubmissionTime() {
        return this.submissionTime;
    }

    public ItnsVillage submissionTime(Instant submissionTime) {
        this.setSubmissionTime(submissionTime);
        return this;
    }

    public void setSubmissionTime(Instant submissionTime) {
        this.submissionTime = submissionTime;
    }

    public String getUntargetingOtherSpecify() {
        return this.untargetingOtherSpecify;
    }

    public ItnsVillage untargetingOtherSpecify(String untargetingOtherSpecify) {
        this.setUntargetingOtherSpecify(untargetingOtherSpecify);
        return this;
    }

    public void setUntargetingOtherSpecify(String untargetingOtherSpecify) {
        this.untargetingOtherSpecify = untargetingOtherSpecify;
    }

    public String getOtherVillageName() {
        return this.otherVillageName;
    }

    public ItnsVillage otherVillageName(String otherVillageName) {
        this.setOtherVillageName(otherVillageName);
        return this;
    }

    public void setOtherVillageName(String otherVillageName) {
        this.otherVillageName = otherVillageName;
    }

    public String getOtherVillageCode() {
        return this.otherVillageCode;
    }

    public ItnsVillage otherVillageCode(String otherVillageCode) {
        this.setOtherVillageCode(otherVillageCode);
        return this;
    }

    public void setOtherVillageCode(String otherVillageCode) {
        this.otherVillageCode = otherVillageCode;
    }

    public Long getOtherTeamNo() {
        return this.otherTeamNo;
    }

    public ItnsVillage otherTeamNo(Long otherTeamNo) {
        this.setOtherTeamNo(otherTeamNo);
        return this;
    }

    public void setOtherTeamNo(Long otherTeamNo) {
        this.otherTeamNo = otherTeamNo;
    }

    public Instant getStartEntryTime() {
        return this.startEntryTime;
    }

    public ItnsVillage startEntryTime(Instant startEntryTime) {
        this.setStartEntryTime(startEntryTime);
        return this;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return this.finishedEntryTime;
    }

    public ItnsVillage finishedEntryTime(Instant finishedEntryTime) {
        this.setFinishedEntryTime(finishedEntryTime);
        return this;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public ItnsVillage deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public SyncableStatus getStatus() {
        return this.status;
    }

    public ItnsVillage status(SyncableStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SyncableStatus status) {
        this.status = status;
    }

    // Inherited createdBy methods
    public ItnsVillage createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public ItnsVillage createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public ItnsVillage lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public ItnsVillage lastModifiedDate(Instant lastModifiedDate) {
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

    public ItnsVillage setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public Set<ItnsVillageHousesDetail> getHouseDetails() {
        return this.houseDetails;
    }

    public void setHouseDetails(Set<ItnsVillageHousesDetail> itnsVillageHousesDetails) {
        if (this.houseDetails != null) {
            this.houseDetails.forEach(i -> i.setItnsVillage(null));
        }
        if (itnsVillageHousesDetails != null) {
            itnsVillageHousesDetails.forEach(i -> i.setItnsVillage(this));
        }
        this.houseDetails = itnsVillageHousesDetails;
    }

    public ItnsVillage houseDetails(Set<ItnsVillageHousesDetail> itnsVillageHousesDetails) {
        this.setHouseDetails(itnsVillageHousesDetails);
        return this;
    }

    public ItnsVillage addHouseDetail(ItnsVillageHousesDetail itnsVillageHousesDetail) {
        this.houseDetails.add(itnsVillageHousesDetail);
        itnsVillageHousesDetail.setItnsVillage(this);
        return this;
    }

    public ItnsVillage removeHouseDetail(ItnsVillageHousesDetail itnsVillageHousesDetail) {
        this.houseDetails.remove(itnsVillageHousesDetail);
        itnsVillageHousesDetail.setItnsVillage(null);
        return this;
    }

    public ProgressStatus getProgressStatus() {
        return this.progressStatus;
    }

    public void setProgressStatus(ProgressStatus progressStatus) {
        this.progressStatus = progressStatus;
    }

    public ItnsVillage progressStatus(ProgressStatus progressStatus) {
        this.setProgressStatus(progressStatus);
        return this;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public ItnsVillage team(Team team) {
        this.setTeam(team);
        return this;
    }

    public Assignment getAssignment() {
        return this.assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public ItnsVillage assignment(Assignment assignment) {
        this.setAssignment(assignment);
        return this;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public ItnsVillage activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItnsVillage)) {
            return false;
        }
        return getId() != null && getId().equals(((ItnsVillage) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ItnsVillage{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", submissionUuid='" + getSubmissionUuid() + "'" +
            ", submissionId=" + getSubmissionId() +
            ", workDayDate='" + getWorkDayDate() + "'" +
            ", surveytype='" + getSurveytype() + "'" +
            ", otherReasonComment='" + getOtherReasonComment() + "'" +
            ", reasonNotcomplete='" + getReasonNotcomplete() + "'" +
            ", settlement='" + getSettlement() + "'" +
            ", settlementName='" + getSettlementName() + "'" +
            ", tlCommenet='" + getTlCommenet() + "'" +
            ", timeSpentHours=" + getTimeSpentHours() +
            ", timeSpentMinutes=" + getTimeSpentMinutes() +
            ", difficulties='" + getDifficulties() + "'" +
            ", locationCaptured='" + getLocationCaptured() + "'" +
            ", locationCaptureTime='" + getLocationCaptureTime() + "'" +
            ", hoProof='" + getHoProof() + "'" +
            ", hoProofUrl='" + getHoProofUrl() + "'" +
            ", submissionTime='" + getSubmissionTime() + "'" +
            ", untargetingOtherSpecify='" + getUntargetingOtherSpecify() + "'" +
            ", otherVillageName='" + getOtherVillageName() + "'" +
            ", otherVillageCode='" + getOtherVillageCode() + "'" +
            ", otherTeamNo=" + getOtherTeamNo() +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
