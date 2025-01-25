package org.nmcpye.datarun.mongo.domain.dataelement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.mongo.domain.DataFieldRule;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class FormElementConf implements Serializable {
    private static final long serialVersionUID = 1L;

    private String path;

    private String parent;

    @NotNull
    @Field("name")
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    private Map<String, String> label;

    private List<DataFieldRule> rules;
    private List<String> appearance;

    private Integer order = 0;

    public abstract String getId();

    public abstract void setId(String path);

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of("en", this.name));
    }

    public Map<String, String> getLabel() {
        if (this.label == null) {
            return Map.of("en", this.name);
        }
        return label;
    }

    public List<DataFieldRule> getRules() {
        return rules;
    }

    public void setRules(List<DataFieldRule> rules) {
        this.rules = rules;
    }

    public List<String> getAppearance() {
        return appearance;
    }

    public void setAppearance(List<String> appearance) {
        this.appearance = appearance;
    }


    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormElementConf element)) return false;
        return Objects.equals(getPath(), element.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPath());
    }
}
