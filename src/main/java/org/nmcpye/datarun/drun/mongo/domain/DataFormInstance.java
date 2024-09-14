package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Document(collection = "data_form_instance")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormInstance
    /*extends AbstractAuditingEntityMongo<String>*/ implements Serializable {

    private static final long serialVersionUID = 1L;

    @MongoId
    private String id;

    @NotNull
    @Field("uid")
    private String uid;

    @Size(max = 16)
    @Field("form_uid")
    private String form;

    @Version
    private Integer version;

    @Field("fields")
    private Set<DataField> fields = new HashSet<>();

    @Field("options")
    private Set<DataOption> options = new HashSet<>();

    public String getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

//    @Override
//    public String getName() {
//        return "";
//    }

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
        DataFormInstance that = (DataFormInstance) o;
        return Objects.equals(form, that.form) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(form, version);
    }
}
