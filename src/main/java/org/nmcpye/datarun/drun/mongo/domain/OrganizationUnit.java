package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.io.Serializable;
import java.util.*;

/**
 * A OrganizationUnit.
 */
@Document(collection = "organization_unit")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrganizationUnit
    extends AbstractAuditingEntityMongo<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @MongoId
    private String id;

    @Size(max = 11)
    @NotNull
    @Field("uid")
    @Indexed(unique = true, name = "ou_uid")
    private String uid;

    @Field("code")
    @Indexed(unique = true, name = "ou_code")
    private String code;

    @Size(max = 2000)
    @Field("description")
    private String description;

    @Field("disabled")
    private Boolean disabled;

    private Map<String, String> label;

    @Field("path")
    @Indexed(unique = true, name = "ou_path")
    @NotNull
    private String path;

    @Field("level")
    @NotNull
    private Integer level;

    @Field("levelName")
    private String levelName;

    @Field("parentCode")
    private String parentCode;


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, Collections::emptyMap);
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public String getLabel(String local) {
        return label.get(local);
    }


    public OrganizationUnit addLabel(String local, String label) {
        this.label.put(local, label);
        return this;
    }


    public String getId() {
        return this.id;
    }

    public OrganizationUnit id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public OrganizationUnit uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public OrganizationUnit code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

//    public String getName() {
//        return this.name;
//    }
//
//    public OrganizationUnit name(String name) {
//        this.setName(name);
//        return this;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }

    public String getDescription() {
        return this.description;
    }

    public OrganizationUnit description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDisabled() {
        return this.disabled;
    }

    public OrganizationUnit disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrganizationUnit dataForm = (OrganizationUnit) o;
        return getUid().equals(dataForm.getUid());
    }

    @Override
    public int hashCode() {
        return getUid().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrganizationUnit{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + getDisabled() + "'" +
            "}";
    }
}
