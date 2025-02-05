package org.nmcpye.datarun.mongo.domain;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * A DataOption.
 */
//@JsonSerialize(using = DataOptionSerializer.class)
//@JsonDeserialize(using = DataOptionDeserializer.class)
public class DataOption implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String listName;

    @NotNull
    private String name;

    private Integer order;

    private Map<String, String> label;

    private Map<String, Object> properties;


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataOption name(String name) {
        this.setName(name);
        return this;
    }

    public DataOption listName(String listName) {
        this.setListName(listName);
        return this;
    }


    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public Map<String, String> getLabel() {
        if (this.label == null) {
            return Map.of("en", this.name);
        }
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of("en", this.name));
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = Objects.requireNonNullElseGet(order, () -> 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataOption that = (DataOption) o;
        return listName.equals(that.listName) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listName, name);
    }

}
