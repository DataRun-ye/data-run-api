package org.nmcpye.datarun.jpa.steptype;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;
import org.nmcpye.datarun.jpa.flowtype.FlowType;

import java.time.Instant;

/**
 * @author Hamza Assada 27/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "step_type", uniqueConstraints = {
    @UniqueConstraint(name = "uc_step_type_template_id",
        columnNames = {"flow_type_id", "data_template_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
public class StepType extends JpaSoftDeleteObject {
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    private String uid;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "description")
    private String description;

    @Column(name = "repeatable")
    private Boolean repeatable = false;

    @NotNull
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_type_id", nullable = false)
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    private FlowType flowType;

    //    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "data_template_id")
//    @JsonSerialize(contentAs = JpaSoftDeleteObject.class)
//    private DataTemplate dataTemplate;
    @NotNull
    private String formTemplateId;

    /**
     * If this step is an entity-bound step specify EntityType or = null (i.e, client need to specify
     * an EntityInstance (e.g, Household, Patient) of the specifies EntityType to
     * submit the step data instances against.
     * If entity instance is already available, it can be selected/searched.
     * if not then `add new, save, and select it` (maybe adding new is configurable?),
     * Consider optional uniqueness (across a FlowType, a FlowRun}
     * an OrgUnit, or mix, or across the whole Activity  running at the time
     *
     * @see EntityType
     * @see EntityInstance
     * @see FlowType
     * @see FlowRun
     */
    private String entityBoundTypeId;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "bound_entity_type_id")
//    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
//    private EntityType entityType;

//    /**
//     * data template version's this submission was submitted by
//     */
//    @Column(name = "data_template_ver_uid", nullable = false)
//    private String dataTemplateVerUid;

//    /**
//     * this data instance version number, changed when updated and old submission's
//     * versions are archived up to MAX=5
//     */
//    @Column(name = "step_ver")
//    private Integer stepVer = 1;

    //    public void setLabel(Map<String, String> label) {
//        final var translations = label.entrySet().stream()
//            .map(entry -> Translation
//                .builder()
//                .locale(entry.getKey())
//                .property("name")
//                .value(entry.getValue()).build()).collect(Collectors.toSet());
//        setTranslations(translations);
//    }

//    @JsonProperty
//    public Map<String, String> getLabel() {
//        if (translations == null || translations.isEmpty()) {
//            return null;
//        }
//        return translations
//            .stream()
//            .collect(Collectors
//                .toMap(Translation::getLocale, Translation::getValue));
//    }
}
