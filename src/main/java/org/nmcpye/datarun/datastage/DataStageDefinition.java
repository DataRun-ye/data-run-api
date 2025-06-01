package org.nmcpye.datarun.datastage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.assignmenttype.AssignmentType;
import org.nmcpye.datarun.common.SoftDeleteObject;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.datatemplate.DataTemplate;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
@Entity
@Table(name = "data_stage_definition", uniqueConstraints = {
    @UniqueConstraint(name = "uc_stage_definition_type_template_id",
        columnNames = {"assignment_type_id", "data_template_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
public class DataStageDefinition extends JpaBaseIdentifiableObject implements SoftDeleteObject<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

    @Column(name = "deleted")
    protected Boolean deleted = false;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "description")
    private String description;

    @Column(name = "repeatable")
    private Boolean repeatable = false;

    @Column(name = "stage_order")
    private Integer stageOrder;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"activity", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    @JoinColumn(name = "assignment_type_id")
    private AssignmentType assignmentType;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_template_id")
    private DataTemplate dataTemplate;
}
