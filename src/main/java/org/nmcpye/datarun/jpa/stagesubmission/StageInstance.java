package org.nmcpye.datarun.jpa.stagesubmission;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonNodeBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.scopeinstance.StageContext;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;

import java.time.Instant;

/**
 * Represents an actual submission of a stage within a given FlowInstance.
 * Contains form data and links to its StageDefinition.
 *
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "stage_submission")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StageInstance extends JpaSoftDeleteObject {
    public enum SubmissionStatus {PENDING, SUBMITTED, REJECTED}

    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Indicating which flow this submission belongs to.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "flow_instance_id", nullable = false)
    private FlowInstance flowInstance;

    /**
     * Indicating which stage template this submission fulfills.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "stage_definition_id", nullable = false)
    private StageDefinition stageDefinition;

    /**
     * optional (null if stage not scope-bound).
     */
    @OneToOne(mappedBy = "stageInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    private StageContext context;

    /**
     * JSONB field to store the actual form data.
     */
    @Type(JsonNodeBinaryType.class)
    @Column(name = "data_row", columnDefinition = "jsonb", nullable = false)
    private JsonNode dataRow;

    /**
     * Status of the stage submission (e.g., PENDING, SUBMITTED).
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    public StageInstance(String id) {
        this.setId(id);
    }

    public void setContext(StageContext stageContext) {
        if (stageContext != null) {
            stageContext.setStageInstance(this);
        }
        this.context = stageContext;
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }
}
