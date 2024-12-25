//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.CompoundIndex;
//import org.springframework.data.mongodb.core.index.CompoundIndexes;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.io.Serial;
//import java.io.Serializable;
//import java.util.List;
//import java.util.Objects;
//
///**
// * A Assignment.
// */
//@Document(collection = "assignment_data_form")
//@CompoundIndexes(
//    @CompoundIndex(name = "assignment_data_form_index", def = "{'dataForm' : 1, 'team' : 1}", unique = true)
//)
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class DataFormAccess
//    implements Serializable {
//
//    @Serial
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("dataForm")
//    private String dataForm;
//
//    @Size(max = 11)
//    @Field("team")
//    private String team;
//
//    @NotNull
//    @Size(max = 11)
//    @Field("assignment")
//    private List<String> assignment;
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
//    public String getTeam() {
//        return team;
//    }
//
//    public void setTeam(String team) {
//        this.team = team;
//    }
//
//    public List<String> getAssignment() {
//        return assignment;
//    }
//
//    public void setAssignment(List<String> assignment) {
//        this.assignment = assignment;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof DataFormAccess that)) return false;
//        return Objects.equals(dataForm, that.dataForm) && Objects.equals(assignment, that.assignment);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(dataForm, assignment);
//    }
//}
