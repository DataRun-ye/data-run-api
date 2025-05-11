package org.nmcpye.datarun.metadatasubmission;

import jakarta.el.PropertyNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.*;

import static java.util.Map.entry;

/**
 * A DataFormSubmission.
 */
@Document(collection = "metadata_submission")
@Getter
@Setter
@CompoundIndex(name = "metadata_submission_uid", def = "{'uid': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MetadataSubmission
    extends MongoAuditableBaseObject {

    @NotNull
    @Field("metadataSchema")
    private String metadataSchema;

    @NotNull
    @Field("deleted")
    private boolean deleted;

    @NotNull
    @Field("resourceType")
    private ReferenceType resourceType;

    @NotNull
    @Field("resourceId")
    private String resourceId;

    @NotNull
    private Map<String, Object> formData = new HashMap<String, Object>();

    @Field("current_version")
    private int version;  // Reference to the latest version in the version history collection

    Long serialNumber;

    /**
     * Populates the form data attributes with additional metadata.
     * This method enriches the form data with various attributes such as submission UID,
     * serial number, submission time, and version information.
     *
     * @return The current DataFormSubmission instance with updated form data
     * @throws PropertyNotFoundException if any of the main attributes (activity, team, or form) is not set
     */
    public MetadataSubmission populateFormDataAttributes() {

        Map<String, Object> formData = this.getFormData();

        Map<String, ?> map = Map.ofEntries(
            entry("_submissionUid", this.getUid()),
            entry("_serialNumber", this.getSerialNumber()),
            entry("_submissionTime", Objects.requireNonNullElse(this.getCreatedDate(), Instant.now())),
            entry("_lastModifiedDate", Objects.requireNonNullElse(this.getLastModifiedDate(), Instant.now())),
            entry("_version", this.getVersion())
        );
        formData.putAll(map);

        this.setFormData(formData);

        return this;
    }

    public MetadataSubmission createSubmission() {

        Map<String, Object> formData = this.getFormData();

        final Object id = Objects.requireNonNullElse(formData.get("_id"), CodeGenerator.generateCode(16));

        formData.put("_id", id);

        Map<String, Object> updatedFormData = addGroupIndicesToFormData(formData, id);

        this.setFormData(updatedFormData);

        return this;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> addGroupIndicesToFormData(Map<String, Object> formData, Object parentId) {
        Map<String, Object> updatedFormData = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();

            // If it's an array of objects, add group indices
            if (value instanceof List<?> list) {
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
                // If it's a nested map, recursively process it
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
}
