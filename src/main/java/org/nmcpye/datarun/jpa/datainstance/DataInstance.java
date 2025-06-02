package org.nmcpye.datarun.jpa.datainstance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.enumeration.AssignmentStatus;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.datastage.DataStageDefinition;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datavalue.DataValue;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A data template instance-header (i.e, a submission-header submitted from client)
 * containing a submission metadata
 *
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
@Entity
@Table(name = "data_instance", indexes = {
    @Index(name = "idx_data_instance_template_ver_uid", columnList = "data_template_ver_uid"),
    @Index(name = "idx_data_instance_org_unit_uid", columnList = "org_unit_uid"),
    @Index(name = "idx_data_instance_assignment_type_uid", columnList = "assignment_type_uid"),
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataInstance extends JpaSoftDeleteObject {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

    protected String code;

    protected String name;

    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "assignment_status")
    private AssignmentStatus assignmentStatus;

    // assignment here can be null (i.e, submissions are linked directly to assignmentType and orgUnits)
    // at such a case, selectable orgUnits can be linked directly to the assignmentType through a relation other
    // than `Assignment` (i.e, for assignmentTypes of type not `PLANNED`, users then would create/update submissions
    // against entityInstances or OrgUnit, and dataValues will be dimensioned accordingly by them. entity instances
    // can be null too when submissions are at orgUnit Unit level (but then what about team? maybe
    // a team-less submission too?!)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    @JsonSerialize(contentAs = AuditableObject.class)
    protected Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_instance_id")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    protected EntityInstance entityInstance;

    @Column(name = "org_unit_uid", nullable = false)
    private String orgUnitUid;

    @Column(name = "assignment_type_uid", nullable = false)
    private String assignmentTypeUid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "data_stage_definition_id")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    protected DataStageDefinition dataStageDefinition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_template_id")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    protected DataTemplate dataTemplate;

    /**
     * data template version's this submission was submitted by
     */
    @Column(name = "data_template_ver_uid", nullable = false)
    private String dataTemplateVerUid;

    /**
     * this data instance version number, changed when updated and old submission's
     * versions are archived up to MAX=5
     */
    @Column(name = "data_instance_ver")
    private Integer dataInstanceVer = 1;

    /**
     * The time this instance was created and entry started at client
     */
    @Column(name = "start_entry_time")
    private Instant startEntryTime;

    /**
     * The time this instance was marked as complete at client
     */
    @Column(name = "finished_entry_time")
    private Instant finishedEntryTime;

    @OneToMany(mappedBy = "dataInstance")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"dataElement", "dataInstance"}, allowSetters = true)
    private Set<DataValue> dataValues = new LinkedHashSet<>();

    // prettier-ignore
    @Override
    public String toString() {
        return "DataFormSubmission{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", finishedEntryTime='" + getFinishedEntryTime() + "'" +
            ", status='" + getAssignmentStatus() + "'" +
            "}";
    }
}
