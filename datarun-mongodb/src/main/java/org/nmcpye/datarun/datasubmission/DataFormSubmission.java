package org.nmcpye.datarun.datasubmission;

import jakarta.el.PropertyNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.SoftDeleteObject;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.nmcpye.datarun.utils.CodeGenerator;
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
@Document(collection = "data_form_submission")
@Getter
@Setter
@CompoundIndex(name = "data_submission_uid", def = "{'uid': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormSubmission
    extends MongoAuditableBaseObject implements SoftDeleteObject<String> {

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

    private Map<String, Object> formData = new LinkedHashMap<>();
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
            entry("_team", this.getTeam()),
            entry("_teamCode", this.getTeamCode()),
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
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    if (containUnidentifiedRepeatItem((List<Map<String, Object>>) list)) {
                        List<Map<String, Object>> updatedList = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            Map<String, Object> objectInArray = (Map<String, Object>) list.get(i);
                            objectInArray.putIfAbsent("_id", CodeGenerator.generateCode(16));  // Add groupIndex (s
                            objectInArray.put("_parentId", parentId);
                            objectInArray.put("_submissionUid", this.getUid());
                            objectInArray.putIfAbsent("_index", i + 1);  // Add repeatIndex (starting from 1)
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
}
