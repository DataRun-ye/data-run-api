package org.nmcpye.datarun.jpa.stepinstance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datavalue.DataValue;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;
import org.nmcpye.datarun.jpa.steptype.StepType;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A step instance, a submission, a data instance of a {@link DataTemplate}
 * containing a submission metadata
 *
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "step_instance", indexes = {
    @Index(name = "ix_ss_by_flow_stage", columnList = "flow_run_id, step_type_id")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StepInstance extends JpaSoftDeleteObject {
//    @JsonIgnore
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

    @Column(name = "code", unique = true)
    protected String code;

    @Column(name = "name", unique = true)
    protected String name;

    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "deleted_at")
    private Instant deletedAt;
    // flowRun here can be null (i.e, submissions are linked directly to flowType and orgUnits)
    // at such a case, selectable orgUnits can be linked directly to the assignmentType through a relation other
    // than `Assignment` (i.e, for assignmentTypes of type not `PLANNED`, users then would create/update submissions
    // against entityInstances or OrgUnit, and dataValues will be dimensioned accordingly by them. entity instances
    // can be null too when submissions are at orgUnit Unit level (but then what about team? maybe
    // a team-less submission too?!)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flow_run_id")
    @JsonSerialize(contentAs = AuditableObject.class)
    protected FlowRun flowRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "step_type_id")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    protected StepType stepType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_instance_id")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    protected EntityInstance entityInstance;

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

    @OneToMany(mappedBy = "stepInstance")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"dataElement", "stepInstance"}, allowSetters = true)
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
            "}";
    }
}
