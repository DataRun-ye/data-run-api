package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.MetadataResourceType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
    @Field("resourceType")
    private MetadataResourceType resourceType;

    @NotNull
    @Field("resourceId")
    private String resourceId;

    @NotNull
    private Map<String, Object> formData = new HashMap<String, Object>();

    @Field("current_version")
    private int version;  // Reference to the latest version in the version history collection

    //    @Indexed(unique = true)
    Long serialNumber;

    public MetadataResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(MetadataResourceType resourceType) {
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

    @Override
    public String getName() {
        return "entity=" + resourceType + ", entityUid=" + resourceId + ", form=" + metadataSchema;
    }

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

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetadataSubmission)) {
            return false;
        }
        return getId() != null && getId().equals(((MetadataSubmission) o).getId());
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
            "}";
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
