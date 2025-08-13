//package org.nmcpye.datarun.jpa.etl;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Size;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.nmcpye.datarun.jpa.activity.Activity;
//import org.nmcpye.datarun.jpa.assignment.Assignment;
//import org.nmcpye.datarun.jpa.dataelement.DataElement;
//import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
//import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
//import org.nmcpye.datarun.jpa.team.Team;
//import org.springframework.data.annotation.CreatedDate;
//
//import java.io.Serial;
//import java.io.Serializable;
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.Objects;
//
///**
// * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
// */
//@Entity
//@Table(name = "element_data_value"/*, uniqueConstraints = {
//    @UniqueConstraint(name = "ux_element_data_value_elem_repeat",
//        columnNames = {"submission_id", "element_id", "repeat_instance_id"})
//}*/,
//    indexes = {
//        @Index(name = "ux_element_data_value_elem", columnList = "submission_id,element_id,repeat_instance_id"),
//        @Index(name = "idx_ev_repeat_id", columnList = "repeat_instance_id"),
//        @Index(name = "idx_element_data_value_deleted_at", columnList = "deleted_at")
//    })
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//@Getter
//@Setter
//@NoArgsConstructor
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class ElementDataValue implements Serializable {
//    @Serial
//    private static final long serialVersionUID = 2738519623273453182L;
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    private Long id;
//
//    @Size(max = 50000)
//    @Column(name = "value_text", length = 50000)
//    private String valueText;
//
//    @Column(name = "value_num")
//    private BigDecimal valueNum;
//
//    @Column(name = "value_bool")
//    private Boolean valueBool;
//
//    /**
//     * The DataFormSubmission id
//     */
//    @Column(name = "submission_id", nullable = false)
//    private String submission;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JsonIgnoreProperties(value = {"dataElementGroups", "optionSet"}, allowSetters = true)
//    private DataElement element;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnoreProperties(value = {"submission"}, allowSetters = true)
//    private RepeatInstance repeatInstance;
//
//    /**
//     * Form template uid
//     */
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    private DataTemplate template;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JsonProperty
//    @JsonIgnoreProperties(value = {"activity", "team", "orgUnit", "parent",
//        "children", "ancestors", "level", "createdBy",
//        "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
//    private Assignment assignment;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnoreProperties(value = {"project", "translations", "assignments"}, allowSetters = true)
//    private Activity activity;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnoreProperties(value = {"parent", "children", "orgUnitGroups", "assignments",
//        "hierarchyLevel", "ancestors", "translations"}, allowSetters = true)
//    private OrgUnit orgUnit;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
//        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity", "teamFormAccesses", "formPermissions"}, allowSetters = true)
//    private Team team;
//
//    @Column(name = "category_id")
//    private String category;
//
//    @Column(name = "deleted_at")
//    private Instant deletedAt;
//
//    @Column(name = "client_updated_date")
//    private Instant clientUpdatedDate;
//
//    @CreatedDate
//    @Column(name = "created_date", updatable = false)
//    protected Instant createdDate = Instant.now();
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof ElementDataValue that)) return false;
//        return Objects.equals(submission, that.submission) &&
//            Objects.equals(repeatInstance, that.repeatInstance) &&
//            Objects.equals(element, that.element);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(submission, repeatInstance, element);
//    }
//}
