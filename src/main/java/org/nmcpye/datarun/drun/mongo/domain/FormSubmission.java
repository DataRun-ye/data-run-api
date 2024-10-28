package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A DataFormSubmission.
 */
@Document(collection = "data_form_submission")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormSubmission
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

    @Field("start_entry_time")
    private Instant startEntryTime;

    @Field("finished_entry_time")
    private Instant finishedEntryTime;

    @Field("status")
    private SyncableStatus status;

    private String form;

    private String activity;

    private String team;

    private String orgUnit;

    private Map<String, Object> formData = new HashMap<String, Object>();

    @Field("current_version")
    private int version;  // Reference to the latest version in the version history collection

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public String getId() {
        return this.id;
    }

    public FormSubmission id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public FormSubmission uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String getName() {
        return activity + ":" + orgUnit + ":" + form + ":" + team;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public FormSubmission deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getStartEntryTime() {
        return this.startEntryTime;
    }

    public FormSubmission startEntryTime(Instant startEntryTime) {
        this.setStartEntryTime(startEntryTime);
        return this;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Instant getFinishedEntryTime() {
        return this.finishedEntryTime;
    }

    public FormSubmission finishedEntryTime(Instant finishedEntryTime) {
        this.setFinishedEntryTime(finishedEntryTime);
        return this;
    }

    public void setFinishedEntryTime(Instant finishedEntryTime) {
        this.finishedEntryTime = finishedEntryTime;
    }

    public SyncableStatus getStatus() {
        return this.status;
    }

    public FormSubmission status(SyncableStatus status) {
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

    public FormSubmission form(String dataForm) {
        this.setForm(dataForm);
        return this;
    }

    public String getActivity() {
        return this.activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public FormSubmission activity(String activity) {
        this.setActivity(activity);
        return this;
    }

    public String getTeam() {
        return this.team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public FormSubmission team(String team) {
        this.setTeam(team);
        return this;
    }

    public String getOrgUnit() {
        return this.orgUnit;
    }

    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    public FormSubmission assignment(String assignment) {
        this.setOrgUnit(assignment);
        return this;
    }


    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FormSubmission)) {
            return false;
        }
        return getId() != null && getId().equals(((FormSubmission) o).getId());
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
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
