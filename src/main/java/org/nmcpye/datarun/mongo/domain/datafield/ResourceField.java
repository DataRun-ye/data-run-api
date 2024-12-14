package org.nmcpye.datarun.mongo.domain.datafield;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.mongo.domain.enumeration.MetadataResourceType;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Objects;

public class ResourceField extends DefaultField {
    @NotNull
    @Field("resourceType")
    private MetadataResourceType resourceType;

    @NotNull
    @Field("resourceMetadataSchema")
    private String resourceMetadataSchema;

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
