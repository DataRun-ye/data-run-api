package org.nmcpye.datarun.mongo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.el.PropertyNotFoundException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.mongo.common.MongoSoftDeleteObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.*;

/**
 * A DataFormSubmission, a Data instance versioned body, containing the actual submission data.
 */
@Document(collection = "data_form_submission")
@Getter
@Setter
@CompoundIndex(name = "data_submission_uid", def = "{'uid': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormSubmission extends MongoSoftDeleteObject {
    @JsonIgnore
    @Id
    private String id;
    @Indexed(unique = true)
    Long serialNumber;
    @Size(max = 11)
    @Field("uid")
    private String uid;
    @Field("deleted")
    @Indexed(name = "submission_deleted_idx")
    private Boolean deleted = false;
    @Field(name = "deletedAt")
    private Instant deletedAt;

    @NotNull
    @Indexed(name = "submission_form_idx")
    private String form;
    //    @NotNull
    @Field("formVersion")
    @Indexed(name = "submission_form_version_uid_idx")
    private String formVersion;
    /**
     * form version number
     */
    @Field("currentVersion")
//    @Indexed(name = "submission_form_version_no_idx")
    private Integer version;

    @Field("submissionVersion")
    @Indexed(name = "submission_version_idx")
    private Integer submissionVersion = 1;

    /**
     * Assigned Team uid
     */
    @Indexed(name = "submission_team_idx")
    private String team;

    /**
     * Assigned Team code
     */
    private String teamCode;

    private String orgUnit;
    private String orgUnitCode;
    private String orgUnitName;
    private String activity;
    @Indexed(name = "submission_assignment_idx")
    @Field("assignment")
    private String assignment;
    @Field("status")
    private FlowStatus status;
    @Field("startEntryTime")
    private Instant startEntryTime;
    @Field("finishedEntryTime")
    private Instant finishedEntryTime;
    private Map<String, Object> formData = new LinkedHashMap<>();

//    private JsonNode dataRow;
    /**
     * Populates the form data attributes with additional metadata.
     * This method enriches the form data with various attributes such as submission UID,
     * serial number, submission time, and version information.
     *
     * @return The current DataFormSubmission instance with updated form data
     * @throws PropertyNotFoundException if any of the main attributes (activity, team, or form) is not set
     */
    public DataFormSubmission checkAttributes() {

        if (Objects.isNull(team)) {
            throw new IllegalQueryException("Submission `" + getUid() + "` team property is not set");
        }

        if (Objects.isNull(form)) {
            throw new IllegalQueryException("Submission `" + getUid() + "` form property is not set");
        }

        return this;
    }

    public DataFormSubmission createSubmission() {

        Map<String, Object> formData = this.getFormData();

        Map<String, Object> updatedFormData = addGroupIndices(formData, getUid());

        this.setFormData(updatedFormData);

        return this;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> addGroupIndices(Map<String, Object> formData, Object parentId) {
        Map<String, Object> updatedFormData = new HashMap<>();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof List<?> list) {
                // Handle list-of-maps *only* when there are unidentified items
                if (!list.isEmpty() && list.get(0) instanceof Map
                    && containUnidentifiedRepeatItem((List<Map<String, Object>>) list)) {

                    List<Map<String, Object>> updatedList = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, Object> item = (Map<String, Object>) list.get(i);
                        item.putIfAbsent("_id", CodeGenerator.generateCode(16));
                        item.put("_parentId", parentId);
                        item.put("_submissionUid", this.getUid());
                        item.putIfAbsent("_index", i + 1);
                        updatedList.add(item);
                    }
                    updatedFormData.put(entry.getKey(), updatedList);

                } else {
                    // <— copy *any* other list back unmodified
                    updatedFormData.put(entry.getKey(), list);
                }

            } else if (value instanceof Map) {
                // recurse into nested objects
                updatedFormData.put(
                    entry.getKey(),
                    addGroupIndices((Map<String, Object>) value, parentId)
                );

            } else {
                // primitive or other, copy as-is
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
