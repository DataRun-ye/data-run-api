//package org.nmcpye.datarun.mongo.domain.dataform;
//
//import jakarta.validation.constraints.Size;
//import lombok.Getter;
//import lombok.Setter;
//import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
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
//@Document(collection = "multi_stage_template")
//@Getter
//@Setter
//@CompoundIndex(name = "multi_stage_template_uid", def = "{'uid': 1}", unique = true)
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class MultiStageTemplate
//    extends MongoBaseIdentifiableObject {
//
//    @Field("code")
//    private String code;
//
//    @Field("name")
//    @Indexed(unique = true, name = "multi_stage_template_name")
//    private String name;
//
//    @Size(max = 2000)
//    @Field("description")
//    private String description;
//
//    @Field("deleted")
//    private boolean deleted;
//
//    private Integer version; // latestVersion
//
//    @Getter
//    @Field("defaultLocale")
//    private String defaultLocale;
//
//    private Boolean openFirstStageForNew = true;
//
//    private Map<String, String> label;
//
//    @Field("stages")
//    private List<TemplateStage> stages = new LinkedList<>();
//
//    public MultiStageTemplate() {
//        setAutoFields();
//    }
//
//    public MultiStageTemplate version(Integer version) {
//        this.setVersion(version);
//        return this;
//    }
//
//    public MultiStageTemplate stages(List<TemplateStage> stages) {
//        this.setStages(stages);
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
