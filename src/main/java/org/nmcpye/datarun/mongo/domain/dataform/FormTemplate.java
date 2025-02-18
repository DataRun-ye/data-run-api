//package org.nmcpye.datarun.mongo.domain.dataform;
//
//import jakarta.validation.constraints.Size;
//import org.nmcpye.datarun.mongo.domain.AbstractAuditingEntityMongo;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//
//public class FormTemplate extends AbstractAuditingEntityMongo<String> {
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("uid")
//    @Indexed(unique = true, name = "form_template_uid")
//    private String uid;
//
//    @Field("name")
//    @Indexed(unique = true, name = "form_template_name")
//    private String name;
//
//    @Size(max = 2000)
//    @Field("description")
//    private String description;
//
//    @Field("disabled")
//    private boolean disabled;
//
//    @Field("deleted")
//    private boolean deleted;
//
//    private Integer version;
//
//    @Field("defaultLocale")
//    private String defaultLocale;
//
//    private Map<String, String> label;
//
//    @Override
//    public String getId() {
//        return id;
//    }
//
//    @Override
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    @Override
//    public String getUid() {
//        return uid;
//    }
//
//    @Override
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }
//
//    public boolean isDisabled() {
//        return disabled;
//    }
//
//    public void setDisabled(boolean disabled) {
//        this.disabled = disabled;
//    }
//
//    public boolean isDeleted() {
//        return deleted;
//    }
//
//    public void setDeleted(boolean deleted) {
//        this.deleted = deleted;
//    }
//
//    public Integer getVersion() {
//        return version;
//    }
//
//    public void setVersion(Integer version) {
//        this.version = version;
//    }
//
//    public String getDefaultLocale() {
//        return defaultLocale;
//    }
//
//    public void setDefaultLocale(String defaultLocale) {
//        this.defaultLocale = defaultLocale;
//    }
//
//    public Map<String, String> getLabel() {
//        return Optional.ofNullable(this.label).orElse(Map.of(getDefaultLocale(), this.name));
//    }
//
//    public void setLabel(Map<String, String> label) {
//        this.label = Objects.requireNonNullElseGet(label, () -> Map.of(getDefaultLocale(), this.name));
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof FormTemplate that)) return false;
//        return Objects.equals(getUid(), that.getUid());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(super.hashCode(), getUid());
//    }
//}
