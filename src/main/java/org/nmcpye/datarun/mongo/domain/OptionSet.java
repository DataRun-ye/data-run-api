//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//@Document(collection = "option_set")
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class OptionSet extends AbstractAuditingEntityMongo<String> {
//
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("uid")
//    @Indexed(unique = true, name = "option_set_uid")
//    private String uid;
//
//    @Field("code")
//    @Indexed(unique = true, name = "data_element_code")
//    private String code;
//
//    @NotNull
//    @Field("name")
//    @Indexed(unique = true, name = "option_set_name")
//    private String name;
//
//    @Field("options")
//    private List<DataOption> options = new ArrayList<>();
//
//    public OptionSet() {
//        setAutoFields();
//        if (getCode() == null || getCode().isEmpty()) {
//            setCode(getUid());
//        }
//    }
//
//    @Override
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    @Override
//    public String getId() {
//        return this.id;
//    }
//
//
//    public String getUid() {
//        return uid;
//    }
//
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    public String getCode() {
//        return code;
//    }
//
//    public void setCode(String code) {
//        this.code = code;
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
//    public List<DataOption> getOptions() {
//        return options;
//    }
//
//    public void setOptions(List<DataOption> options) {
//        this.options = options;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof OptionSet optionSet)) return false;
//        return Objects.equals(getUid(), optionSet.getUid());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hashCode(getUid());
//    }
//}
