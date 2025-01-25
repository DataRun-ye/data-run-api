//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.validation.constraints.Size;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.io.Serializable;
//
//@Document(collection = "data_collection_policy")
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class DataCollectionPolicy
//    extends AbstractAuditingEntityMongo<String>
//    implements Serializable {
//
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("uid")
//    @Indexed(unique = true, name = "data_submission_uid")
//    private String uid;
//
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
//}
