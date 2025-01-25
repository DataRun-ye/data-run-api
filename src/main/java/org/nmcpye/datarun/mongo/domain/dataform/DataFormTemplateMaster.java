//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.validation.constraints.Size;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.time.Instant;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//
///**
// * A DataForm.
// */
//@Document(collection = "data_form_template_master")
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class DataFormTemplateMaster
//    extends AbstractAuditingEntityMongo<String> {
//
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("uid")
//    @Indexed(unique = true, name = "template_master_uid")
//    private String uid;
//
//    @Field("name")
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
//    private Map<String, String> label;
//
//    @Field("defaultLocale")
//    private String defaultLocale;
//
//    @Field("latestVersion")
//    private Integer latestVersion;
//
//    @Field("latestVersionUid")
//    private String latestVersionUid; // Points to the latest version's uid
//
//    // Metadata for offline access
//    @Field("lastUpdated")
//    private Instant lastUpdated;
//
//    public DataFormTemplateMaster() {
//        setAutoFields();
//    }
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
//    public void setLabel(Map<String, String> label) {
//        this.label = Objects.requireNonNullElseGet(label, () -> Map.of(getDefaultLocale(), this.name));
//    }
//
//    public Map<String, String> getLabel() {
//        return Optional.ofNullable(this.label).orElse(Map.of(getDefaultLocale(), this.name));
//    }
//
//    public String getDefaultLocale() {
//        return Optional.ofNullable(defaultLocale).orElse("en");
//    }
//
//    public void setDefaultLocale(String defaultLocale) {
//        this.defaultLocale = Objects.requireNonNullElse(defaultLocale, "en");
//    }
//
//    public Integer getLatestVersion() {
//        return latestVersion;
//    }
//
//    public void setLatestVersion(Integer latestVersion) {
//        this.latestVersion = latestVersion;
//    }
//
//    public String getLatestVersionUid() {
//        return latestVersionUid;
//    }
//
//    public void setLatestVersionUid(String latestVersionUid) {
//        this.latestVersionUid = latestVersionUid;
//    }
//
//    public Instant getLastUpdated() {
//        return lastUpdated;
//    }
//
//    public void setLastUpdated(Instant lastUpdated) {
//        this.lastUpdated = lastUpdated;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        DataFormTemplateMaster dataForm = (DataFormTemplateMaster) o;
//        return (id != null && id.equals(dataForm.id)) ||
//            (uid != null && uid.equals(dataForm.uid));
//    }
//
//    @Override
//    public int hashCode() {
//        return id != null ? id.hashCode() : (uid != null ? uid.hashCode() : 0);
//    }
//
//    // prettier-ignore
//    @Override
//    public String toString() {
//        return "DataForm{" +
//            "id=" + getId() +
//            ", uid='" + getUid() + "'" +
//            ", name='" + getName() + "'" +
//            ", description='" + getDescription() + "'" +
//            ", disabled='" + isDisabled() + "'" +
//            "}";
//    }
//}
