package org.nmcpye.datarun.jpa.datavalue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
 */
@Entity
@Table(name = "data_value", uniqueConstraints = {
        @UniqueConstraint(name = "uc_data_value_on_element_instance",
                columnNames = {"instance_id", "element_id", "repeat_instance_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataValue extends JpaAuditable<Long> {

    private static final Pattern ZERO_PATTERN = Pattern.compile("^0(\\.0*)?$");

    public static final String TRUE = "true";

    public static final String FALSE = "false";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Size(max = 50000)
    @Column(name = "value", length = 50000)
    private String value;

    @Column(name = "instance_id", nullable = false)
    private String instanceId;

    @Column(name = "repeat_instance_id")
    private String repeatInstanceId;

    @Column(name = "repeat_index")
    private Long repeatIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonProperty
    @JsonIgnoreProperties(value = {"dataElementGroups", "optionSet"}, allowSetters = true)
    @JoinColumn(name = "element_id")
    private DataElement dataElement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonProperty
    private DataTemplate dataTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonProperty
    @JsonIgnoreProperties(value = {"parent", "children", "orgUnitGroups", "assignments",
            "hierarchyLevel", "ancestors", "translations"}, allowSetters = true)
    private OrgUnit orgUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
            "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity", "teamFormAccesses", "formPermissions"}, allowSetters = true)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonProperty
    @JsonIgnoreProperties(value = {"activity", "team", "orgUnit", "parent", "children", "ancestors", "level", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    private Assignment assignment;
    //   form_template_id TEXT NOT NULL,
    //  orgunit_id BIGINT,
    //  team_id BIGINT,
    //  assignment_id BIGINT,
    //  submitted_at TIMESTAMPTZ NOT NULL,
    //  element_id TEXT NOT NULL,
    //  value_text TEXT,
    //  value_num NUMERIC(18, 6),
    //  value_bool BOOLEAN,
    //
    //  -- Revised repeat metadata
    //  repeat_instance_uid UUID NULL, -- The deterministic UID from the client, NULL for non-repeated fields.
    //  repeat_index INT NULL,         -- The original 0-based index, still useful for ordering.
    //  category_id TEXT,
    public DataValue(DataElement dataElement, String instanceId, String repeatInstanceId) {
        this.dataElement = dataElement;
        this.instanceId = instanceId;
        this.repeatInstanceId = repeatInstanceId;
        this.createdDate = Instant.now();
        this.lastModifiedDate = Instant.now();
    }

    public DataValue(DataElement dataElement, String instanceId, String repeatInstanceId, String value) {
        this.dataElement = dataElement;
        this.instanceId = instanceId;
        this.repeatInstanceId = repeatInstanceId;
        this.value = value;
        this.createdDate = Instant.now();
        this.lastModifiedDate = Instant.now();
    }

    @Builder(toBuilder = true)
    public DataValue(DataElement dataElement, String instanceId,
                     String value, String repeatInstanceId, Instant deletedAt) {
        this.dataElement = dataElement;
        this.instanceId = instanceId;
        this.repeatInstanceId = repeatInstanceId;
        this.value = value;
        this.createdDate = Instant.now();
        this.deletedAt = deletedAt;
    }

    public boolean isNullValue() {
        return StringUtils.trimToNull(value) == null;
    }

    public void mergeWith(DataValue other) {
        this.value = other.getValue();
        this.createdDate = other.getCreatedDate();
        this.lastModifiedDate = other.getLastModifiedDate();
        this.deletedAt = other.getDeletedAt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!getClass().isAssignableFrom(o.getClass())) {
            return false;
        }

        final DataValue other = (DataValue) o;

        return dataElement.equals(other.getDataElement()) &&
                instanceId.equals(other.getInstanceId()) && repeatInstanceId.equals(other.getRepeatInstanceId());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = result * prime + dataElement.hashCode();
        result = result * prime + instanceId.hashCode();
        result = result * prime + repeatInstanceId.hashCode();

        return result;
    }
}
