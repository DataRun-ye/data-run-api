//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.el.PropertyNotFoundException;
//import jakarta.validation.constraints.Size;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
//import org.nmcpye.datarun.utils.CodeGenerator;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.io.Serializable;
//import java.time.Instant;
//import java.util.*;
//
//import static java.util.Map.entry;
//
///**
// * A DataFormSubmission.
// */
//public class FormSubmission
//    extends AbstractAuditingEntityMongo<String> implements Serializable {
//
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("uid")
//    @Indexed(unique = true, name = "data_submission_uid")
//    private String uid;
//
//    @Field("deleted")
//    private Boolean deleted;
//
//    @Field("startEntryTime")
//    private Instant startEntryTime;
//
//    @Field("finishedEntryTime")
//    private Instant finishedEntryTime;
//
//    private String form;
//
//    private String team;
//
//    private String assignment;
//
//    @Field("status")
//    private AssignmentStatus status;
//
//    @Field("currentVersion")
//    private int version;
//
//    @Indexed(unique = true)
//    Long serialNumber;
//
//    private Map<String, Object> formData = new HashMap<String, Object>();
//
//    @Field("dataVersion")
//    private int dataVersion;  // Reference to the latest version in the version history collection
//
//    public int getDataVersion() {
//        return dataVersion;
//    }
//
//    public void setDataVersion(int dataVersion) {
//        this.dataVersion = dataVersion;
//    }
//
//    public Long getSerialNumber() {
//        return serialNumber;
//    }
//
//    public void setSerialNumber(Long serialNumber) {
//        this.serialNumber = serialNumber;
//    }
//
//    public Map<String, Object> getFormData() {
//        return formData;
//    }
//
//    public void setFormData(Map<String, Object> formData) {
//        this.formData = formData;
//    }
//
//    public String getId() {
//        return this.id;
//    }
//
//    public FormSubmission id(String id) {
//        this.setId(id);
//        return this;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public String getUid() {
//        return this.uid;
//    }
//
//    public FormSubmission uid(String uid) {
//        this.setUid(uid);
//        return this;
//    }
//
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    public Boolean getDeleted() {
//        return this.deleted;
//    }
//
//    public FormSubmission deleted(Boolean deleted) {
//        this.setDeleted(deleted);
//        return this;
//    }
//
//    public void setDeleted(Boolean deleted) {
//        this.deleted = deleted;
//    }
//
//    public Instant getStartEntryTime() {
//        return this.startEntryTime;
//    }
//
//    public FormSubmission startEntryTime(Instant startEntryTime) {
//        this.setStartEntryTime(startEntryTime);
//        return this;
//    }
//
//    public void setStartEntryTime(Instant startEntryTime) {
//        this.startEntryTime = startEntryTime;
//    }
//
//    public Instant getFinishedEntryTime() {
//        return this.finishedEntryTime;
//    }
//
//    public FormSubmission finishedEntryTime(Instant finishedEntryTime) {
//        this.setFinishedEntryTime(finishedEntryTime);
//        return this;
//    }
//
//    public void setFinishedEntryTime(Instant finishedEntryTime) {
//        this.finishedEntryTime = finishedEntryTime;
//    }
//
//    public AssignmentStatus getStatus() {
//        return this.status;
//    }
//
//    public FormSubmission status(AssignmentStatus status) {
//        this.setStatus(status);
//        return this;
//    }
//
//    public void setStatus(AssignmentStatus status) {
//        this.status = status;
//    }
//
//    public String getForm() {
//        return this.form;
//    }
//
//    public void setForm(String dataForm) {
//        this.form = dataForm;
//    }
//
//    public FormSubmission form(String dataForm) {
//        this.setForm(dataForm);
//        return this;
//    }
//
////    public String getActivity() {
////        return this.activity;
////    }
////
////    public void setActivity(String activity) {
////        this.activity = activity;
////    }
//
////    public DataFormSubmission activity(String activity) {
////        this.setActivity(activity);
////        return this;
////    }
//
//    public String getTeam() {
//        return this.team;
//    }
//
//    public void setTeam(String team) {
//        this.team = team;
//    }
//
//    public FormSubmission team(String team) {
//        this.setTeam(team);
//        return this;
//    }
//
//    public String getAssignment() {
//        return this.assignment;
//    }
//
//    public void setAssignment(String assignment) {
//        this.assignment = assignment;
//    }
//
//    public FormSubmission assignment(String assignment) {
//        this.setAssignment(assignment);
//        return this;
//    }
//
//    /**
//     * Populates the form data attributes with additional metadata.
//     * This method enriches the form data with various attributes such as submission UID,
//     * serial number, submission time, and version information.
//     *
//     * @return The current DataFormSubmission instance with updated form data
//     * @throws PropertyNotFoundException if any of the main attributes (activity, team, or form) is not set
//     */
//    public FormSubmission populateFormDataAttributes() {
//
//        if (Objects.isNull(team) || Objects.isNull(form)) {
//            throw new PropertyNotFoundException("one or more of the MainAttributes activity, team, or form is not set");
//        }
//
//        Map<String, Object> formData = this.getFormData();
//
//        Map<String, ?> map = Map.ofEntries(
//            entry("_deleted", this.getDeleted()),
//            entry("_assignment", this.getAssignment()),
//            entry("_submissionUid", this.getUid()),
//            entry("_serialNumber", this.getSerialNumber()),
//            entry("_submissionTime", Objects.requireNonNullElse(this.getCreatedDate(), Instant.now())),
//            entry("_lastModifiedDate", Objects.requireNonNullElse(this.getLastModifiedDate(), Instant.now())),
//            entry("_version", this.getVersion())
//        );
//
//        formData.putIfAbsent("_startEntryTime", this.getStartEntryTime());
//        formData.putIfAbsent("_finishedEntryTime", this.getFinishedEntryTime());
//        formData.putAll(map);
//
//        this.setFormData(formData);
//
//        return this;
//    }
//
//    public FormSubmission createSubmission() {
//
//        Map<String, Object> formData = this.getFormData();
//
//        final Object id = Objects.requireNonNullElse(formData.get("_id"), CodeGenerator.generateCode(16));
//
//        formData.put("_id", id);
//
//        Map<String, Object> updatedFormData = addGroupIndices(formData, id);
//
//        this.setFormData(updatedFormData);
//
//        return this;
//    }
//
//    private Map<String, Object> addGroupIndices(Map<String, Object> formData, Object parentId) {
//        Map<String, Object> updatedFormData = new HashMap<>();
//        for (Map.Entry<String, Object> entry : formData.entrySet()) {
//            Object value = entry.getValue();
//
//            if (value instanceof List) {
//                List<?> list = (List<?>) value;
//                if (!list.isEmpty() && list.get(0) instanceof Map) {
//                    if (containUnidentifiedRepeatItem((List<Map<String, Object>>) list)) {
//                        List<Map<String, Object>> updatedList = new ArrayList<>();
//                        for (int i = 0; i < list.size(); i++) {
//                            Map<String, Object> objectInArray = (Map<String, Object>) list.get(i);
//                            objectInArray.put("_parentId", parentId);
//                            objectInArray.put("_id", CodeGenerator.generateCode(16));  // Add groupIndex (s
//                            objectInArray.put("_index", i + 1);  // Add repeatIndex (starting from 1)
//                            updatedList.add(objectInArray);
//                        }
//                        updatedFormData.put(entry.getKey(), updatedList);
//                    }
//                } else {
//                    updatedFormData.put(entry.getKey(), list);
//                }
//            } else if (value instanceof Map) {
//                updatedFormData.put(entry.getKey(), addGroupIndices((Map<String, Object>) value, parentId));
//            } else {
//                updatedFormData.put(entry.getKey(), value);
//            }
//        }
//        return updatedFormData;
//    }
//
//    public boolean containUnidentifiedRepeatItem(List<Map<String, Object>> items) {
//        return items
//            .stream()
//            .anyMatch(obj -> obj.get("_parentId") == null
//                || obj.get("_id") == null
//                || obj.get("_index") == null);
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        FormSubmission dataFormSubmission = (FormSubmission) o;
//        return (id != null && id.equals(dataFormSubmission.id)) ||
//            (uid != null && uid.equals(dataFormSubmission.uid));
//    }
//
//    @Override
//    public int hashCode() {
//        return id != null ? id.hashCode() : (uid != null ? uid.hashCode() : 0);
//    }
//
//
//    // prettier-ignore
//    @Override
//    public String toString() {
//        return "DataFormSubmission{" +
//            "id=" + getId() +
//            ", uid='" + getUid() + "'" +
//            ", deleted='" + getDeleted() + "'" +
//            ", startEntryTime='" + getStartEntryTime() + "'" +
//            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
//            ", status='" + getStatus() + "'" +
//            "}";
//    }
//
//    /**
//     * form version
//     *
//     * @return The form version of the submission,
//     */
//    public int getVersion() {
//        return version;
//    }
//
//    public void setVersion(int version) {
//        this.version = version;
//    }
//}
