package org.nmcpye.datarun.drun.postgres.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.domain.Activity;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
@Entity
@Table(name = "assignment_type")
@Getter
@Setter
public class AssignmentType {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activityDefinition;

    private String name;

    @Enumerated(EnumType.STRING)
    private PlanningMode planningMode;

    @Enumerated(EnumType.STRING)
    private SubmissionMode submissionMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum PlanningMode {
        PLANNED, LOG_AS_YOU_GO
    }

    public enum SubmissionMode {
        SINGLE, MULTI_STAGE
    }
}
