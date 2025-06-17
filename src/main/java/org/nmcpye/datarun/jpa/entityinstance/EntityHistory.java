package org.nmcpye.datarun.jpa.entityinstance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;

import java.util.Map;

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "entity_history",
    indexes = {
        @Index(name = "ix_eh_by_entity", columnList = "entity_instance_id, created_date"),
        @Index(name = "ix_eh_by_flow", columnList = "flow_instance_id")
    })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class EntityHistory extends JpaIdentifiableObject {
//    @Size(max = 11)
//    @Column(name = "uid", length = 11, updatable = false, unique = true)
//    private String uid;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_instance_id")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    private EntityInstance entityInstance;

    @Column(name = "flow_instance_id")
    private String flowInstanceId;

    @Column(name = "stage_submission_id")
    private String stageSubmissionId;

    private String eventType;  // e.g. "UNPACK_QUALITY_CHECK"

    @JsonProperty
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventData;

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getUid() {
        return "";
    }

    @Override
    public void setUid(String uid) {

    }
}

