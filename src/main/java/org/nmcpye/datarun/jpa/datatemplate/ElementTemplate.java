package org.nmcpye.datarun.jpa.datatemplate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.time.Instant;

/**
 * Lightweight DTO used to insert/select from template_field table.
 * displayLabel and definition are JSON strings (jsonb in DB).
 *
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
@Entity
@Table(name = "element_template", uniqueConstraints = {
    @UniqueConstraint(name = "uc_data_template_element_version",
        columnNames = {"template_id", "template_version_id", "data_element_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ElementTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    private String templateId;

    @NotNull
    private String templateVersionId;

    @NotNull
    private Integer versionNo;

    @NotNull
    private String dataElementId;

    @NotNull
    private String path;

    @NotNull
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", updatable = false, nullable = false)
    protected ValueType valueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", updatable = false, nullable = false)
    private AggregationType aggregationType;

    /**
     * if ValueType any of: Team, OrgUnit, Activity, SingleOption
     */
    private Boolean isReference;

    private String referenceTable; // team, org_unit, activity
    private String optionSetId;

    private Boolean isRepeatable;
    private String repeatPath;
    private Boolean isMulti;
    private Boolean isMeasure;

    /**
     * (element_id or null) of this element's repeat
     */
    private String categoryForRepeat;

    /**
     * JSON as strings; repository writes them to jsonb
     */
    private String displayLabelJson;
    /**
     * JSON as strings; repository writes them to jsonb
     */
    private String definitionJson;
    private Instant createdAt;
}
