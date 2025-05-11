package org.nmcpye.datarun.datasubmission;

import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A DataFormSubmission.
 */
@Document(collection = "data_form_submission_history")
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormSubmissionHistory
    extends MongoAuditableBaseObject {

    private String dataSubmissionId;  // Link to the main document

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

    public DataFormSubmissionHistory(DataFormSubmission existingDataSubmissionId, Instant timestamp) {
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
}
