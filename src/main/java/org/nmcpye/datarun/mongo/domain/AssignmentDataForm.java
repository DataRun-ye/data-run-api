//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.bson.types.ObjectId;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.CompoundIndex;
//import org.springframework.data.mongodb.core.index.CompoundIndexes;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.io.Serial;
//import java.io.Serializable;
//import java.util.Objects;
//
///**
// * A Assignment.
// */
//@Document(collection = "assignment_data_form")
//@CompoundIndexes(
//    @CompoundIndex(name = "assignment_data_form_index", def = "{'dataForm' : 1, 'assignment' : 1}", unique = true)
//)
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class AssignmentDataForm
//    implements Serializable {
//
//    @Serial
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    private String id;
//
////    @Size(max = 11)
////    @Field("dataForm")
////    @Field("dataForm")
////    @DocumentReference(lookup = "{ 'uid' : ?#{#target} }")
//    private String dataForm;
//
//    ObjectId dataFormId;
//
//    @NotNull
//    @Size(max = 11)
//    @Field("assignment")
//    private String assignment;
//
//    public String getId() {
//        return id;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public String getDataForm() {
//        return dataForm;
//    }
//
//    public void setDataForm(String dataForm) {
//        this.dataForm = dataForm;
//    }
//
//    public String getAssignment() {
//        return assignment;
//    }
//
//    public void setAssignment(String assignment) {
//        this.assignment = assignment;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof AssignmentDataForm that)) return false;
//        return Objects.equals(dataForm, that.dataForm) && Objects.equals(assignment, that.assignment);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(dataForm, assignment);
//    }
//}
