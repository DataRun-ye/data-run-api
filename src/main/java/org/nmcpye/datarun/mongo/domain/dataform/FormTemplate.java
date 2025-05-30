package org.nmcpye.datarun.mongo.domain.dataform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
import org.nmcpye.datarun.drun.postgres.common.translation.Translation;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A FormTemplate.
 */
@Document(collection = "form_template")
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormTemplate
    extends MongoBaseIdentifiableObject {
    @JsonIgnore
    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "form_template_uid_idx")
    private String uid;

    @Size(max = 11)
    @Field("formVersion")
    @Indexed(unique = true, name = "form_template_version_uid_idx")
    private String formVersion;

    @Field("versionNumber")
    private Integer versionNumber = 0;

    /// metadata get updated on this without versioning
    @Field("disabled")
    private Boolean disabled = false;

    @Field("deleted")
    private Boolean deleted = false;

    @Field("name")
    @NotNull
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    String defaultLocale = "ar";

    private Map<String, String> label;

    public FormTemplate() {
        setAutoFields();
    }

    public void setLabel(Map<String, String> label) {
        final var translations = label.entrySet().stream()
            .map(entry -> Translation
                .builder()
                .locale(entry.getKey())
                .property("name")
                .value(entry.getValue()).build()).collect(Collectors.toSet());
        setTranslations(translations);
        this.label = label;
    }

    public FormTemplate versionNumber(Integer currentVersion) {
        setVersionNumber(currentVersion);
        return this;
    }

    public FormTemplate id(String id) {
        setId(id);
        return this;
    }

    public FormTemplate uid(String uid) {
        setUid(uid);
        return this;
    }

    public FormTemplate formVersion(String formVersion) {
        this.setFormVersion(formVersion);
        return this;
    }

//    public FormTemplate createdBy(String createdBy) {
//        setCreatedBy(createdBy);
//        return this;
//    }
//
//    public FormTemplate lastModifiedBy(String lastModifiedBy) {
//        setLastModifiedBy(lastModifiedBy);
//        return this;
//    }
//
//    public FormTemplate createdDate(Instant createdDate) {
//        setCreatedDate(createdDate);
//        return this;
//    }
//
//    public FormTemplate lastModifiedDate(Instant lastModifiedDate) {
//        setLastModifiedDate(lastModifiedDate);
//        return this;
//    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormTemplate that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getUid(), that.getUid()) &&
            Objects.equals(getVersionNumber(), that.getVersionNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getUid(), getVersionNumber());
    }

    @JsonIgnore
    @Override
    public String getCode() {
        return "";
    }
}
