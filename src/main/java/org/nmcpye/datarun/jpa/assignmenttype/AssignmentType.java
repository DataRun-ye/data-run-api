package org.nmcpye.datarun.jpa.assignmenttype;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;


/**
 * equivalent to some degree to dhis2 program
 * <pre>
 *     {@code
 *  AssignmentType {
 *       id: "malariaCampaign",
 *      name: "Malaria Case Management",
 *      planningMode: "PLANNED",
 *      submissionMode: "MULTI_STAGE",
 *      stages: [
 *        { id: "registration",   name: "Registration",   formTemplateId: "householdForm", repeatable: false },
 *        { id: "caseVisit",      name: "Case Visit",     formTemplateId: "caseForm",      repeatable: true  },
 *        { id: "treatmentFollow",name: "Follow-Up",      formTemplateId: "treatmentForm", repeatable: false }
 *       ]
 *     }
 *  }
 * </pre>
 *
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
@Entity
@Table(name = "assignment_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssignmentType extends JpaBaseIdentifiableObject {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "planning_mode")
    private PlanningMode planningMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_mode")
    private SubmissionMode submissionMode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    public enum PlanningMode {
        PLANNED, LOG_AS_YOU_GO
    }

    public enum SubmissionMode {
        SINGLE, MULTI_STAGE
    }
}
