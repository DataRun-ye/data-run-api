package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.enumeration.SettlementEnum;
import org.nmcpye.datarun.domain.enumeration.SurveyTypeEnum;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.ItnsVillage} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ItnsVillageDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

    @NotNull
    private String submissionUuid;

    @NotNull
    private Long submissionId;

    private Instant workDayDate;

    private SurveyTypeEnum surveytype;

    @Size(max = 2000)
    private String otherReasonComment;

    @Size(max = 2000)
    private String reasonNotcomplete;

    private SettlementEnum settlement;

    @Size(max = 2000)
    private String settlementName;

    @Size(max = 2000)
    private String tlCommenet;

    private Integer timeSpentHours;

    private Integer timeSpentMinutes;

    @Size(max = 2000)
    private String difficulties;

    private String locationCaptured;

    private Instant locationCaptureTime;

    private String hoProof;

    private Instant startEntryTime;

    private Instant endEntryTime;

    private Instant finishedEntryTime;

    private String hoProofUrl;

    private Instant submissionTime;

    private String untargetingOtherSpecify;

    @Size(max = 2000)
    private String otherVillageName;

    @Size(max = 2000)
    private String otherVillageCode;

    private Long otherTeamNo;

    private Boolean deleted;

    private ProgressStatusDTO progressStatus;

    @NotNull
    private TeamDTO team;

    @NotNull
    private AssignmentDTO assignment;

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

    public String getSubmissionUuid() {
        return submissionUuid;
    }

    public void setSubmissionUuid(String submissionUuid) {
        this.submissionUuid = submissionUuid;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Instant getWorkDayDate() {
        return workDayDate;
    }

    public void setWorkDayDate(Instant workDayDate) {
        this.workDayDate = workDayDate;
    }

    public SurveyTypeEnum getSurveytype() {
        return surveytype;
    }

    public void setSurveytype(SurveyTypeEnum surveytype) {
        this.surveytype = surveytype;
    }

    public String getOtherReasonComment() {
        return otherReasonComment;
    }

    public void setOtherReasonComment(String otherReasonComment) {
        this.otherReasonComment = otherReasonComment;
    }

    public String getReasonNotcomplete() {
        return reasonNotcomplete;
    }

    public void setReasonNotcomplete(String reasonNotcomplete) {
        this.reasonNotcomplete = reasonNotcomplete;
    }

    public SettlementEnum getSettlement() {
        return settlement;
    }

    public void setSettlement(SettlementEnum settlement) {
        this.settlement = settlement;
    }

    public String getSettlementName() {
        return settlementName;
    }

    public void setSettlementName(String settlementName) {
        this.settlementName = settlementName;
    }

    public String getTlCommenet() {
        return tlCommenet;
    }

    public void setTlCommenet(String tlCommenet) {
        this.tlCommenet = tlCommenet;
    }

    public Integer getTimeSpentHours() {
        return timeSpentHours;
    }

    public void setTimeSpentHours(Integer timeSpentHours) {
        this.timeSpentHours = timeSpentHours;
    }

    public Integer getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(Integer timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public String getDifficulties() {
        return difficulties;
    }

    public void setDifficulties(String difficulties) {
        this.difficulties = difficulties;
    }

    public String getLocationCaptured() {
        return locationCaptured;
    }

    public void setLocationCaptured(String locationCaptured) {
        this.locationCaptured = locationCaptured;
    }

    public Instant getLocationCaptureTime() {
        return locationCaptureTime;
    }

    public void setLocationCaptureTime(Instant locationCaptureTime) {
        this.locationCaptureTime = locationCaptureTime;
    }

    public String getHoProof() {
        return hoProof;
    }

    public void setHoProof(String hoProof) {
        this.hoProof = hoProof;
    }

    public Instant getStartEntryTime() {
        return startEntryTime;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getEndEntryTime() {
        return endEntryTime;
    }

    public void setEndEntryTime(Instant endEntryTime) {
        this.endEntryTime = endEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return finishedEntryTime;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public String getHoProofUrl() {
        return hoProofUrl;
    }

    public void setHoProofUrl(String hoProofUrl) {
        this.hoProofUrl = hoProofUrl;
    }

    public Instant getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(Instant submissionTime) {
        this.submissionTime = submissionTime;
    }

    public String getUntargetingOtherSpecify() {
        return untargetingOtherSpecify;
    }

    public void setUntargetingOtherSpecify(String untargetingOtherSpecify) {
        this.untargetingOtherSpecify = untargetingOtherSpecify;
    }

    public String getOtherVillageName() {
        return otherVillageName;
    }

    public void setOtherVillageName(String otherVillageName) {
        this.otherVillageName = otherVillageName;
    }

    public String getOtherVillageCode() {
        return otherVillageCode;
    }

    public void setOtherVillageCode(String otherVillageCode) {
        this.otherVillageCode = otherVillageCode;
    }

    public Long getOtherTeamNo() {
        return otherTeamNo;
    }

    public void setOtherTeamNo(Long otherTeamNo) {
        this.otherTeamNo = otherTeamNo;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public ProgressStatusDTO getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(ProgressStatusDTO progressStatus) {
        this.progressStatus = progressStatus;
    }

    public TeamDTO getTeam() {
        return team;
    }

    public void setTeam(TeamDTO team) {
        this.team = team;
    }

    public AssignmentDTO getAssignment() {
        return assignment;
    }

    public void setAssignment(AssignmentDTO assignment) {
        this.assignment = assignment;
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
        if (!(o instanceof ItnsVillageDTO)) {
            return false;
        }

        ItnsVillageDTO itnsVillageDTO = (ItnsVillageDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, itnsVillageDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ItnsVillageDTO{" +
            "uid=" + getUid() +
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
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", endEntryTime='" + getEndEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", hoProofUrl='" + getHoProofUrl() + "'" +
            ", submissionTime='" + getSubmissionTime() + "'" +
            ", untargetingOtherSpecify='" + getUntargetingOtherSpecify() + "'" +
            ", otherVillageName='" + getOtherVillageName() + "'" +
            ", otherVillageCode='" + getOtherVillageCode() + "'" +
            ", otherTeamNo=" + getOtherTeamNo() +
            ", deleted='" + getDeleted() + "'" +
            ", progressStatus=" + getProgressStatus() +
            ", team=" + getTeam() +
            ", assignment=" + getAssignment() +
            ", activity=" + getActivity() +
            "}";
    }
}
