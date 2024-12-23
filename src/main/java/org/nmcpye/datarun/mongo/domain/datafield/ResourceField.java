package org.nmcpye.datarun.mongo.domain.datafield;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.mongo.domain.enumeration.MetadataResourceType;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Objects;

enum AllowedAction {
    Add,
    Update,
    SoftDelete,
}

public class ResourceField extends DefaultField {
    @NotNull
    @Field("resourceType")
    private MetadataResourceType resourceType;

    @NotNull
    @Field("resourceMetadataSchema")
    private String resourceMetadataSchema;

    @Field("displayAttributes")
    private List<String> displayAttributes;

    @Field("allowedActions")
    private List<AllowedAction> allowedActions;

    public MetadataResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(MetadataResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceMetadataSchema() {
        return resourceMetadataSchema;
    }

    public void setResourceMetadataSchema(String resourceMetadataSchema) {
        this.resourceMetadataSchema = resourceMetadataSchema;
    }

    public List<String> getDisplayAttributes() {
        return displayAttributes;
    }

    public void setDisplayAttributes(List<String> displayAttributes) {
        this.displayAttributes = displayAttributes;
    }

    public List<AllowedAction> getAllowedActions() {
        return allowedActions;
    }

    public void setAllowedActions(List<AllowedAction> allowedActions) {
        this.allowedActions = allowedActions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceField that)) return false;
        if (!super.equals(o)) return false;
        return resourceType == that.resourceType && Objects.equals(resourceMetadataSchema, that.resourceMetadataSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resourceType, resourceMetadataSchema);
    }
}
