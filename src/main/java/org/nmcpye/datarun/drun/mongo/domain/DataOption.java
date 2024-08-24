package org.nmcpye.datarun.drun.mongo.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.drun.mongo.mapping.serialization.DataOptionDeserializer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A DataOption.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
//@JsonSerialize(using = DataOptionSerializer.class)
@JsonDeserialize(using = DataOptionDeserializer.class)
public class DataOption implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String listName;

    @NotNull
    private String name;

    private Integer order;

    private Map<String, String> label;

    private Map<String, Object> properties = new HashMap<>();

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void addProperty(String key, Object name) {
        this.properties.put(key, name);
    }

    public Map<String, String> getLabel() {
        if (this.label == null) {
            return Map.of("en", this.name);
        }
        return label;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = Objects.requireNonNullElseGet(order, () -> 0);
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getName() {
        return this.name;
    }

    public DataOption name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of("en", this.name));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataOption)) {
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
        return "DataOption{" +
//            ", uid='" + getUid() + "'" +
            ", name='" + getName() + "'" +
//            ", description='" + getDescription() + "'" +
            "}";
    }
}
