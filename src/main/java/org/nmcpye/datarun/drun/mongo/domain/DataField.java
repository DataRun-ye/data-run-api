package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.ValueType;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A DataField.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataField implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 11)
    @Field("uid")
    private String uid;

    @Field("code")
    private String code;

    @NotNull
    @Field("name")
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    @Field("type")
    private ValueType type;

    @Field("mandatory")
    private Boolean mandatory;

    @Field("rule")
    private Set<DataFieldRule> rules = new HashSet<>();

    @Field("option")
    private Set<DataOption> options = new HashSet<>();

    private Map<String, String> label;

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of("en", this.name));
    }

    public Map<String, String> getLabel() {
        if (this.label == null) {
            return Map.of("en", this.name);
        }
        return label;
    }

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public String getUid() {
        return this.uid;
    }

    public DataField uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public DataField code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public DataField name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public DataField description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ValueType getType() {
        return this.type;
    }

    public DataField type(ValueType type) {
        this.setType(type);
        return this;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public Boolean getMandatory() {
        return this.mandatory;
    }

    public DataField mandatory(Boolean mandatory) {
        this.setMandatory(mandatory);
        return this;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Set<DataFieldRule> getRules() {
        return this.rules;
    }

    public void setRules(Set<DataFieldRule> dataFieldRules) {
        this.rules = dataFieldRules;
    }

    public DataField rules(Set<DataFieldRule> dataFieldRules) {
        this.setRules(dataFieldRules);
        return this;
    }

    public DataField addRule(DataFieldRule dataFieldRule) {
        this.rules.add(dataFieldRule);
        return this;
    }

    public DataField removeRule(DataFieldRule dataFieldRule) {
        this.rules.remove(dataFieldRule);
        return this;
    }

    public Set<DataOption> getOptions() {
        return this.options;
    }

    public void setOptions(Set<DataOption> dataOptions) {
        this.options = dataOptions;
    }

    public DataField options(Set<DataOption> dataOptions) {
        this.setOptions(dataOptions);
        return this;
    }

    public DataField addOption(DataOption dataOption) {
        this.options.add(dataOption);
        return this;
    }

    public DataField removeOption(DataOption dataOption) {
        this.options.remove(dataOption);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataField)) {
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
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", type='" + getType() + "'" +
            ", mandatory='" + getMandatory() + "'" +
            "}";
    }
}
