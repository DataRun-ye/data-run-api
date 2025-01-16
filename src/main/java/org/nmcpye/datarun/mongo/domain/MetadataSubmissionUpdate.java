package org.nmcpye.datarun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
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
@Document(collection = "metadata_submission_update")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MetadataSubmissionUpdate
    extends AbstractAuditingEntityMongo<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "metadata_submission__uid")
    private String uid;

    @NotNull
    @Field("submissionId")
    private String submissionId;

    @NotNull
    @Field("metadataSubmission")
    private String metadataSubmission;

    @NotNull
    @Field("resourceType")
    private ReferenceType resourceType;


    @NotNull
    @Field("resourceId")
    private String resourceId;

    @NotNull
    private Map<String, Object> formData = new HashMap<>();

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

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public String getId() {
        return this.id;
    }

    public MetadataSubmissionUpdate id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public MetadataSubmissionUpdate uid(String uid) {
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

    public String getMetadataSubmission() {
        return this.metadataSubmission;
    }

    public void setMetadataSubmission(String dataForm) {
        this.metadataSubmission = dataForm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadataSubmissionUpdate metadataSubmission = (MetadataSubmissionUpdate) o;
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
}
