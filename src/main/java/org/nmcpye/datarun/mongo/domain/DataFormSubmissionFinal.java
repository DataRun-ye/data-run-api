package org.nmcpye.datarun.mongo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.el.PropertyNotFoundException;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.mongo.common.MongoBaseIdentifiableObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.*;

import static java.util.Map.entry;

/**
 * A DataFormSubmission.
 */
@Document(collection = "data_form_submission_final")
@Getter
@Setter
@CompoundIndex(name = "data_submission_final_uid", def = "{'uid': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormSubmissionFinal extends MongoBaseIdentifiableObject {
    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    private String uid;

    @Field("deleted")
    private Boolean deleted;

    private String activity;

    private String assignment;

    @Field("teamOld")
    private String teamOld;

    private String team;

    private String teamCode;

    private String orgUnit;
    private String orgUnitCode;
    private String orgUnitName;

    private String workDay;

    @Field("status")
    private FlowStatus status;

    private Map<String, Object> formData = new LinkedHashMap<>();

    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Field("startEntryTime")
    private Instant startEntryTime;

    @Field("finishedEntryTime")
    private Instant finishedEntryTime;

    private String form;
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

    /**
     * Populates the form data attributes with additional metadata.
     * This method enriches the form data with various attributes such as submission UID,
     * serial number, submission time, and version information.
     *
     * @return The current DataFormSubmission instance with updated form data
     * @throws PropertyNotFoundException if any of the main attributes (activity, team, or form) is not set
     */
    public DataFormSubmissionFinal populateFormDataAttributes() {

        if (Objects.isNull(team) || Objects.isNull(form)) {
            throw new PropertyNotFoundException("one or more of the MainAttributes activity, team, or form is not set");
        }

        Map<String, Object> formData = this.getFormData();

        Map<String, ?> map = Map.ofEntries(
            entry("_deleted", this.getDeleted()),
//            entry("_assignment", this.getAssignment()),
            entry("_form", this.getForm()),
            entry("_submissionUid", this.getUid()),
            entry("_serialNumber", this.getSerialNumber()),
            entry("_submissionTime", Objects.requireNonNullElse(this.getCreatedDate(), Instant.now())),
            entry("_lastModifiedDate", Objects.requireNonNullElse(this.getLastModifiedDate(), Instant.now())),
            entry("_version", this.getVersion())
        );

        if (assignment != null) {
            formData.put("_assignment", this.getAssignment());
        }
        formData.putIfAbsent("_startEntryTime", this.getStartEntryTime());
        formData.putIfAbsent("_finishedEntryTime", this.getFinishedEntryTime());
        formData.putAll(map);

        this.setFormData(formData);

        return this;
    }

    public DataFormSubmissionFinal createSubmission() {

        Map<String, Object> formData = this.getFormData();

        final Object id = Objects.requireNonNullElse(formData.get("_id"), CodeGenerator.generateCode(16));

        formData.put("_id", id);

        Map<String, Object> updatedFormData = addGroupIndices(formData, id);

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
                            objectInArray.put("_parentId", parentId);
                            objectInArray.put("_id", CodeGenerator.generateCode(16));  // Add groupIndex (s
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
