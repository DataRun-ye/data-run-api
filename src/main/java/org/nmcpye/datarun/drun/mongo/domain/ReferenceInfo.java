package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.ResourceType;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

/**
 * A ReferenceInfo.
 */
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A Resource Name: DataForm, Activity, Team...
     */
    @NotNull
    @Field("resource")
    private ResourceType resource;

    /**
     * A Resource uid
     */
    @NotNull
    @Field("resource_id")
    private String resourceId;

    /**
     * A Resource Property name,
     * Example: if resource is DataForm -> the field name inside it
     * if empty it will consider the resource identifiable properties
     */
    @Field("resource_property")
    private String resourceProperty;

    @Size(max = 2000)
    @Field("description")
    private String description;

    @NotNull
    @Field("type")
    private ReferenceType type;

    public ResourceType getResource() {
        return resource;
    }

    public void setResource(ResourceType resource) {
        this.resource = resource;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceProperty() {
        return resourceProperty;
    }

    public void setResourceProperty(String resourceProperty) {
        this.resourceProperty = resourceProperty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReferenceType getType() {
        return type;
    }

    public void setType(ReferenceType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceInfo)) {
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataField{" +
            ", description='" + getDescription() + "'" +
            ", type='" + getType() + "'" +
            "}";
    }
}
