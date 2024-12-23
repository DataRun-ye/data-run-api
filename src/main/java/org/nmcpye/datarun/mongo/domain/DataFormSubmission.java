package org.nmcpye.datarun.mongo.domain;

import jakarta.el.PropertyNotFoundException;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
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

    @Field("status")
    private SyncableStatus status;

    private String form;

    private String activity;

    private String team;

    private String orgUnit;

    private Map<String, Object> formData = new HashMap<String, Object>();

    @Field("currentVersion")
    private int version;

    @Indexed(unique = true)
    Long serialNumber;

//    @Field("submissionVersion")
//    private int submissionVersion;  // Reference to the latest version in the version history collection

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

    public SyncableStatus getStatus() {
        return this.status;
    }

    public DataFormSubmission status(SyncableStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SyncableStatus status) {
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

    public String getActivity() {
        return this.activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public DataFormSubmission activity(String activity) {
        this.setActivity(activity);
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

    public String getOrgUnit() {
        return this.orgUnit;
    }

    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    public DataFormSubmission assignment(String assignment) {
        this.setOrgUnit(assignment);
        return this;
    }

    /**
     * Populates the form data attributes with additional metadata.
     * This method enriches the form data with various attributes such as submission UID,
     * serial number, submission time, and version information.
     *
     * @throws PropertyNotFoundException if any of the main attributes (activity, team, or form) is not set
     * @return The current DataFormSubmission instance with updated form data
     */
    public DataFormSubmission populateFormDataAttributes() {

        if (Objects.isNull(activity) || Objects.isNull(team) || Objects.isNull(form)) {
            throw new PropertyNotFoundException("one or more of the MainAttributes activity, team, or form is not set");
        }

        Map<String, Object> formData = this.getFormData();

        Map<String, ?> map = Map.ofEntries(
            entry("_deleted", this.getDeleted()),
            entry("_submissionUid", this.getUid()),
            entry("_serialNumber", this.getSerialNumber()),
            entry("_submissionTime", Objects.requireNonNullElse(this.getCreatedDate(), Instant.now())),
            entry("_lastModifiedDate", Objects.requireNonNullElse(this.getLastModifiedDate(), Instant.now())),
            entry("_version", this.getVersion())
        );

        formData.putIfAbsent("_startEntryTime", this.getStartEntryTime());
        formData.putIfAbsent("_finishedEntryTime", this.getFinishedEntryTime());
        formData.putAll(map);

        this.setFormData(formData);

        return this;
    }

    public DataFormSubmission createSubmission() {

        Map<String, Object> formData = this.getFormData();

        final Object id = Objects.requireNonNullElse(formData.get("_id"), CodeGenerator.generateCode(16));

        formData.put("_id", id);

        Map<String, Object> updatedFormData = addGroupIndicesToFormData(formData, id);

        this.setFormData(updatedFormData);

        return this;
    }

    private Map<String, Object> addGroupIndicesToFormData(Map<String, Object> formData, Object parentId) {
        Map<String, Object> updatedFormData = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();

            // If it's an array of objects, add group indices
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    if (containUnidentifiedRepeatItem((List<Map<String, Object>>) list)) {
                        List<Map<String, Object>> updatedList = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            Map<String, Object> objectInArray = (Map<String, Object>) list.get(i);
                            objectInArray.put("_parentId", parentId);
                            objectInArray.put("_id", CodeGenerator.generateCode(16));  // Add groupIndex (s
                            // Add groupIndex (starting from 1)
                            objectInArray.put("_index", i + 1);  // Add repeatIndex (starting from 1)
                            updatedList.add(objectInArray);
                        }
                        updatedFormData.put(entry.getKey(), updatedList);
                    }
                } else {
                    // If it's not an array of objects, just copy as is
                    updatedFormData.put(entry.getKey(), list);
                }
            } else if (value instanceof Map) {
//                // If it's a nested map, recursively process it
//                ((Map<String, Object>) value).remove("uid");
//                ((Map<String, Object>) value).remove("_uid");
//                ((Map<String, Object>) value).remove("_uuid");
                updatedFormData.put(entry.getKey(), addGroupIndicesToFormData((Map<String, Object>) value, parentId));
            } else {
                // If it's a simple value, just copy as is
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
