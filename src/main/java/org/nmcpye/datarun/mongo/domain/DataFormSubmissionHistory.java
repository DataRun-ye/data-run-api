package org.nmcpye.datarun.mongo.domain;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A DataFormSubmission.
 */
@Document(collection = "data_form_submission_history")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormSubmissionHistory
    extends AbstractAuditingEntityMongo<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private String dataSubmissionId;  // Link to the main document

    @Size(max = 11)
    @Field("uid")
    private String uid;


    private int version;

    @Field("deleted")
    private Boolean deleted;

    private String form;

    private String activity;

    private String team;

    private String assignment;

    @Field("status")
    private AssignmentStatus status;

    private Map<String, Object> formData = new HashMap<String, Object>();

    private Instant timestamp;

    public DataFormSubmissionHistory(DataFormSubmission existingDataSubmissionId,  Instant timestamp) {
        this.dataSubmissionId = existingDataSubmissionId.getId();
        this.uid = existingDataSubmissionId.getUid();
        this.form = existingDataSubmissionId.getForm();
//        this.activity = existingDataSubmissionId.getActivity();
        this.assignment = existingDataSubmissionId.getAssignment();
        this.status = existingDataSubmissionId.getStatus();
        this.team = existingDataSubmissionId.getTeam();
        this.formData = existingDataSubmissionId.getFormData();
        this.version = existingDataSubmissionId.getVersion();
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

    public DataFormSubmissionHistory id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public DataFormSubmissionHistory uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

//    @Override
//    public String getName() {
//        return activity + ":" + orgUnit + ":" + form + ":" + team;
//    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public DataFormSubmissionHistory deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public AssignmentStatus getStatus() {
        return this.status;
    }

    public DataFormSubmissionHistory status(AssignmentStatus status) {
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

    public DataFormSubmissionHistory form(String dataForm) {
        this.setForm(dataForm);
        return this;
    }

    public String getActivity() {
        return this.activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public DataFormSubmissionHistory activity(String activity) {
        this.setActivity(activity);
        return this;
    }

    public String getTeam() {
        return this.team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public DataFormSubmissionHistory team(String team) {
        this.setTeam(team);
        return this;
    }

    public String getAssignment() {
        return this.assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
    }

    public DataFormSubmissionHistory assignment(String assignment) {
        this.setAssignment(assignment);
        return this;
    }


    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataFormSubmissionHistory)) {
            return false;
        }
        return getId() != null && getId().equals(((DataFormSubmissionHistory) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataFormSubmission{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDataSubmissionId() {
        return dataSubmissionId;
    }

    public void setDataSubmissionId(String dataSubmissionId) {
        this.dataSubmissionId = dataSubmissionId;
    }
}
