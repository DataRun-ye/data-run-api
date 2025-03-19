package org.nmcpye.datarun.mongo.domain;

import jakarta.el.PropertyNotFoundException;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

import static java.util.Map.entry;

/**
 * A DataFormSubmission.
 */
@Document(collection = "data_form_submission")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormSubmission
    extends AbstractAuditingEntityMongo<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "data_submission_uid")
    private String uid;

    @Field("deleted")
    private Boolean deleted;

    @Field("startEntryTime")
    private Instant startEntryTime;

    @Field("finishedEntryTime")
    private Instant finishedEntryTime;

    private String form;

    private String team;
    private String teamCode;

    private String orgUnit;
    private String orgUnitCode;
    private String orgUnitName;

    private String activity;

    private String assignment;

    @Field("status")
    private AssignmentStatus status;

    private Map<String, Object> formData = new LinkedHashMap<String, Object>();
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Field("currentVersion")
    private int version;

    @Indexed(unique = true)
    Long serialNumber;

    @Field("reassignedTo")
    private String reassignedTo;

    @Field("rescheduledTo")
    private String rescheduledTo;

    @Field("mergedWith")
    private String mergedWith;

    @Field("cancelReason")
    private String cancelReason;

    public DataFormSubmission() {
        setAutoFields();
    }

    public String getReassignedTo() {
        return reassignedTo;
    }

    public void setReassignedTo(String reassignedTo) {
        this.reassignedTo = reassignedTo;
    }

    public String getRescheduledTo() {
        return rescheduledTo;
    }

    public void setRescheduledTo(String rescheduledTo) {
        this.rescheduledTo = rescheduledTo;
    }

    public String getMergedWith() {
        return mergedWith;
    }

    public void setMergedWith(String mergedWith) {
        this.mergedWith = mergedWith;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public String getId() {
        return this.id;
    }

    public DataFormSubmission id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public DataFormSubmission uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public DataFormSubmission deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getStartEntryTime() {
        return this.startEntryTime;
    }

    public DataFormSubmission startEntryTime(Instant startEntryTime) {
        this.setStartEntryTime(startEntryTime);
        return this;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return this.finishedEntryTime;
    }

    public DataFormSubmission finishedEntryTime(Instant finishedEntryTime) {
        this.setFinishedEntryTime(finishedEntryTime);
        return this;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public AssignmentStatus getStatus() {
        return this.status;
    }

    public DataFormSubmission status(AssignmentStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public String getForm() {
        return this.form;
    }

    public void setForm(String dataForm) {
        this.form = dataForm;
    }

    public DataFormSubmission form(String dataForm) {
        this.setForm(dataForm);
        return this;
    }

    public String getTeam() {
        return this.team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public DataFormSubmission team(String team) {
        this.setTeam(team);
        return this;
    }

    public String getAssignment() {
        return this.assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
    }

    public DataFormSubmission assignment(String assignment) {
        this.setAssignment(assignment);
        return this;
    }

    public String getTeamCode() {
        return teamCode;
    }

    public void setTeamCode(String teamCode) {
        this.teamCode = teamCode;
    }

    public String getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    public String getOrgUnitCode() {
        return orgUnitCode;
    }

    public void setOrgUnitCode(String orgUnitCode) {
        this.orgUnitCode = orgUnitCode;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public void setOrgUnitName(String orgUnitName) {
        this.orgUnitName = orgUnitName;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Populates the form data attributes with additional metadata.
     * This method enriches the form data with various attributes such as submission UID,
     * serial number, submission time, and version information.
     *
     * @return The current DataFormSubmission instance with updated form data
     * @throws PropertyNotFoundException if any of the main attributes (activity, team, or form) is not set
     */
    public DataFormSubmission populateFormDataAttributes() {

        if (Objects.isNull(team)) {
            throw new IllegalQueryException("Submission `" + getUid() + "` team property is not set");
        }

        if (Objects.isNull(form)) {
            throw new IllegalQueryException("Submission `" + getUid() + "` form property is not set");
        }

        Map<String, Object> map = Map.ofEntries(
            entry("_id", this.getUid()),
            entry("_deleted", deleted == Boolean.TRUE),
            entry("_form", this.getForm()),
            entry("_team", this.getForm()),
            entry("_serialNumber", this.getSerialNumber()),
            entry("_submissionTime", Objects.requireNonNullElse(this.getCreatedDate(), Instant.now())),
            entry("_lastModifiedDate", Objects.requireNonNullElse(this.getLastModifiedDate(), Instant.now())),
            entry("_version", this.getVersion())
        );

        Map<String, Object> metadata = new LinkedHashMap<>(map);

        if (assignment != null) {
            metadata.put("_assignment", this.getAssignment());
            metadata.put("_orgUnit", orgUnit);
            metadata.put("_orgUnitCode", orgUnitCode);
            metadata.put("_orgUnitName", orgUnitName);
            metadata.put("_activity", activity);
        }

        this.setMetadata(metadata);

        return this;
    }

    public DataFormSubmission createSubmission() {

        Map<String, Object> formData = this.getFormData();

//        final Object id = Objects.requireNonNullElse(formData.get("_submissionUid"), CodeGenerator.generateCode(16));

//        final Object id = formData.putIfAbsent("_id", getUid());

        Map<String, Object> updatedFormData = addGroupIndices(formData, getUid());

        this.setFormData(updatedFormData);

        return this;
    }

    private Map<String, Object> addGroupIndices(Map<String, Object> formData, Object parentId) {
        Map<String, Object> updatedFormData = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    if (containUnidentifiedRepeatItem((List<Map<String, Object>>) list)) {
                        List<Map<String, Object>> updatedList = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            Map<String, Object> objectInArray = (Map<String, Object>) list.get(i);
                            objectInArray.put("_id", CodeGenerator.generateCode(16));  // Add groupIndex (s
                            objectInArray.put("_parentId", parentId);
                            objectInArray.put("_submissionUid", this.getUid());
                            objectInArray.put("_index", i + 1);  // Add repeatIndex (starting from 1)
                            updatedList.add(objectInArray);
                        }
                        updatedFormData.put(entry.getKey(), updatedList);
                    }
                } else {
                    updatedFormData.put(entry.getKey(), list);
                }
            } else if (value instanceof Map) {
                updatedFormData.put(entry.getKey(), addGroupIndices((Map<String, Object>) value, parentId));
            } else {
                updatedFormData.put(entry.getKey(), value);
            }
        }
        return updatedFormData;
    }

    public boolean containUnidentifiedRepeatItem(List<Map<String, Object>> items) {
        return items
            .stream()
            .anyMatch(obj -> obj.get("_parentId") == null
                || obj.get("_id") == null
                || obj.get("_index") == null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataFormSubmission dataFormSubmission = (DataFormSubmission) o;
        return (id != null && id.equals(dataFormSubmission.id)) ||
            (uid != null && uid.equals(dataFormSubmission.uid));
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (uid != null ? uid.hashCode() : 0);
    }


    // prettier-ignore
    @Override
    public String toString() {
        return "DataFormSubmission{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }

    /**
     * form version
     *
     * @return The form version of the submission,
     */
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
