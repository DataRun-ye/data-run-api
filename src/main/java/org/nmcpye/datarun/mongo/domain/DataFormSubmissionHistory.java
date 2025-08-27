package org.nmcpye.datarun.mongo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.mongo.common.MongoBaseIdentifiableObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * A DataFormSubmissionHistory.
 */
@Document(collection = "data_form_submission_history")
@CompoundIndex(name = "submission_history_uid_time_idx", def = "{'id': 1, 'timestamp': -1}")
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormSubmissionHistory
    extends MongoBaseIdentifiableObject {
    @Id
    private String id;
    /**
     * Submission id, not unique
     */
    @Size(max = 11)
    @Field("id")
    private String uid;
    @NotNull
    private String form;
    /**
     * form version id
     */
    @NotNull
    @Field("formVersion")
    private String formVersion;
    /**
     * form version number
     */
    @Field("currentVersion")
//    @Indexed(name = "history_submission_form_version_no_idx")
    private Integer version;
    /**
     * previous submission's version, pumped up on master submission with each update
     */
    @NotNull
    @Field("submissionVersion")
    @Indexed(name = "history_submission_version_idx")
    private Integer submissionVersion;
    private String team;
    private String teamCode;
    private String orgUnit;
    private String orgUnitCode;
    private String orgUnitName;
    private String activity;
    @Field("assignment")
    private String assignment;
    @Field("status")
    private FlowStatus status;
    @Field("deleted")
    private Boolean deleted;
    private Map<String, Object> formData;
    @Field("startEntryTime")
    private Instant startEntryTime;
    @Field("finishedEntryTime")
    private Instant finishedEntryTime;
    private Instant timestamp;

    // prettier-ignore
    @Override
    public String toString() {
        return "DataFormSubmissionHistory {" +
            "id=" + getId() +
            ", id='" + getUid() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }

    @JsonIgnore
    @Override
    public String getCode() {
        return null;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return null;
    }
}
