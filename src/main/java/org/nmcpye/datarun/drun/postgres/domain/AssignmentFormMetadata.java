//package org.nmcpye.datarun.drun.postgres.domain;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//
//import java.io.Serializable;
//import java.util.Objects;
//import java.util.Set;
//
///**
// * A Assignment.
// */
//@Entity
//@Table(name = "assignment_form_metadata"/*, uniqueConstraints = {
//    @UniqueConstraint(name = "uc_assignemt_metadata_form_team",
//        columnNames = {"activity_id", "orgUnit_id", "team_id"})
//}*/)
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//public class AssignmentFormMetadata
//    implements Serializable {
//
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    private Long id;
//
//    @Size(max = 11)
//    @Column(name = "dataForm", length = 11, nullable = false)
//    private String dataForm;
//
////    @ManyToOne(optional = false)
////    @NotNull
////    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
////        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity"}, allowSetters = true)
////    private Team team;
//
//    @OneToMany
//    @NotNull
//    @JsonIgnoreProperties(value = {"parent", "children", "ancestors", "activity", "orgUnit", "team",
//        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
//    private Set<Assignment> assignments;
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
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
////    public Team getTeam() {
////        return team;
////    }
////
////    public void setTeam(Team team) {
////        this.team = team;
////    }
//
//    public Set<Assignment> getAssignments() {
//        return assignments;
//    }
//
//    public void setAssignments(Set<Assignment> assignments) {
//        this.assignments = assignments;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof AssignmentFormMetadata that)) return false;
//        return Objects.equals(id, that.id) && Objects.equals(dataForm, that.dataForm);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id, dataForm);
//    }
//}
