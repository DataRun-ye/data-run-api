//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.validation.constraints.Size;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * A DataForm.
// */
//@Document(collection = "data_form_template")
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class DataFormTemplateVersion
//    extends AbstractAuditingEntityMongo<String> {
//
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("uid")
//    @Indexed(unique = true, name = "template_version_uid")
//    private String uid;
//
//    @Size(max = 11)
//    @Field("templateMasterUid")
//    private String templateMasterUid;
//
//    private Integer version;
//
//    @Field("isLatest")
//    private boolean isLatest; // To simplify latest-version queries
//
//    @Field("fieldsConf")
//    private Map<String, FormDataElementConf> fieldsConf = new HashMap<>();
//
//    @Field("sectionsConf")
//    private Map<String, FormSectionConf> sectionsConf = new HashMap<>();
//
//    public DataFormTemplateVersion() {
//        setAutoFields();
//    }
//
//    public String getId() {
//        return this.id;
//    }
//
//    public DataFormTemplateVersion id(String id) {
//        this.setId(id);
//        return this;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public String getUid() {
//        return this.uid;
//    }
//
//    public DataFormTemplateVersion uid(String uid) {
//        this.setUid(uid);
//        return this;
//    }
//
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    public String getTemplateMasterUid() {
//        return templateMasterUid;
//    }
//
//    public void setTemplateMasterUid(String templateMasterUid) {
//        this.templateMasterUid = templateMasterUid;
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
//    public boolean isLatest() {
//        return isLatest;
//    }
//
//    public void setLatest(boolean latest) {
//        isLatest = latest;
//    }
//
//    public Map<String, FormDataElementConf> getFieldsConf() {
//        return fieldsConf;
//    }
//
//    public void setFieldsConf(Map<String, FormDataElementConf> fieldsConf) {
//        this.fieldsConf = fieldsConf;
//    }
//
//    public DataFormTemplateVersion fieldsConf(Map<String, FormDataElementConf> fieldsConf) {
//        this.setFieldsConf(fieldsConf);
//        return this;
//    }
//
//    public Map<String, FormSectionConf> getSectionsConf() {
//        return sectionsConf;
//    }
//
//    public void setSectionsConf(Map<String, FormSectionConf> sectionsConf) {
//        this.sectionsConf = sectionsConf;
//    }
//
//    public DataFormTemplateVersion sectionsConf(Map<String, FormSectionConf> sectionsConf) {
//        this.setSectionsConf(sectionsConf);
//        return this;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        DataFormTemplateVersion dataForm = (DataFormTemplateVersion) o;
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
//            "}";
//    }
//}
