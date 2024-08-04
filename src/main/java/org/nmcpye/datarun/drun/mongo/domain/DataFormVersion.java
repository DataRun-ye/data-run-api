package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Document(collection = "data_form_version")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormVersion {

    @MongoId
    private String id;

    @Size(max = 16)
    @Field("formUid")
    private String form;

    @Version
    private Integer version;

    @Field("fields")
    private Set<DataField> fields = new HashSet<>();

    @Field("rules")
    private Set<DataFieldRule> rules = new HashSet<>();

    @Field("options")
    private Set<DataOption> options = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Set<DataField> getFields() {
        return fields;
    }

    public void setFields(Set<DataField> fields) {
        this.fields = fields;
    }

    public Set<DataFieldRule> getRules() {
        return rules;
    }

    public void setRules(Set<DataFieldRule> rules) {
        this.rules = rules;
    }

    public Set<DataOption> getOptions() {
        return options;
    }

    public void setOptions(Set<DataOption> options) {
        this.options = options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataFormVersion that = (DataFormVersion) o;
        return Objects.equals(form, that.form) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(form, version);
    }
}
