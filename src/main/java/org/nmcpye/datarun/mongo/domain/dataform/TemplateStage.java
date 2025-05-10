//package org.nmcpye.datarun.mongo.domain.dataform;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import lombok.Getter;
//import lombok.Setter;
//import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
//import org.springframework.data.mongodb.core.index.CompoundIndex;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
///**
// * A DataFormTemplate.
// */
//@Document(collection = "template_stage")
//@Getter
//@Setter
//@CompoundIndex(name = "template_stage_uid", def = "{'uid': 1}", unique = true)
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class TemplateStage
//    extends MongoBaseIdentifiableObject {
//
//    @Field("order")
//    private Integer order = 0;
//
//
//    @Field("code")
//    private String code;
//
//    @Field("name")
//    @Indexed(unique = true, name = "template_stage_name")
//    private String name;
//
//    @Size(max = 2000)
//    @Field("description")
//    private String description;
//
//    @NotNull
//    @Field("templateUid")
//    private String templateUid;
//
//    @Field("repeatable")
//    private Boolean repeatable = false; // points to latest Version uid
//
//    private Map<String, String> label;
//
//    @Field("fields")
//    private List<FormDataElementConf> fields = new LinkedList<>();
//
//    @Field("sections")
//    private List<FormSectionConf> sections = new LinkedList<>();
//
//    public TemplateStage() {
//        setAutoFields();
//    }
//
//    public TemplateStage fields(List<FormDataElementConf> fieldsConf) {
//        this.setFields(fieldsConf);
//        return this;
//    }
//
//    public TemplateStage sections(List<FormSectionConf> sections) {
//        this.setSections(sections);
//        return this;
//    }
//
//    // prettier-ignore
//    @Override
//    public String toString() {
//        return "DataFormTemplate{" +
//            "id=" + getId() +
//            ", uid='" + getUid() + "'" +
//            ", name='" + getName() + "'" +
//            ", description='" + getDescription() + "'" +
//            "}";
//    }
//}
