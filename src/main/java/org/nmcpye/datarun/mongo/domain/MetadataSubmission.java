package org.nmcpye.datarun.mongo.domain;

import jakarta.el.PropertyNotFoundException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
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
@Document(collection = "metadata_submission")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MetadataSubmission
    extends AbstractAuditingEntityMongo<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "metadata_submission_uid")
    private String uid;

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

//    private Map<String, Object> updatedMetadata = new HashMap<String, Object>();

    @Field("current_version")
    private int version;  // Reference to the latest version in the version history collection

    //    @Indexed(unique = true)
    Long serialNumber;

//    public Map<String, Object> getUpdatedMetadata() {
//        return updatedMetadata;
//    }
//
//    public void setUpdatedMetadata(Map<String, Object> updatedMetadata) {
//        this.updatedMetadata = updatedMetadata;
//


    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public ReferenceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ReferenceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

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

    public MetadataSubmission id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public MetadataSubmission uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

//    @Override
//    public String getName() {
//        return "entity=" + resourceType + ", entityUid=" + resourceId + ", form=" + metadataSchema;
//    }

    public String getMetadataSchema() {
        return this.metadataSchema;
    }

    public void setMetadataSchema(String dataForm) {
        this.metadataSchema = dataForm;
    }

    public MetadataSubmission form(String dataForm) {
        this.setMetadataSchema(dataForm);
        return this;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadataSubmission metadataSubmission = (MetadataSubmission) o;
        return (id != null && id.equals(metadataSubmission.id)) ||
            (uid != null && uid.equals(metadataSubmission.uid));
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
            "}";
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
