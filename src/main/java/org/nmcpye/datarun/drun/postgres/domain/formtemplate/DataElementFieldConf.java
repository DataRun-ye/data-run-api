package org.nmcpye.datarun.drun.postgres.domain.formtemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.AggregationType;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.util.Map;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
@Entity
@Table(name = "data_element_field_conf", indexes = {
    @Index(name = "idx_form_template_uid_unq", columnList = "uid", unique = true)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElementFieldConf extends JpaBaseIdentifiableObject
    implements FormWithFields {
    @Column(name = "type")
    private ValueType type;

    @Column(name = "calculation")
    private String calculation;
    @Column(name = "default_value")
    private Object defaultValue;
    @Column(name = "mandatory")
    private Boolean mandatory;
    @Column(name = "read_only")
    private Boolean readOnly;
    @Column(name = "main_field")
    private Boolean mainField;

    @Column(name = "option_set")
    private String optionSet;

    @Column(name = "choice_filter")
    private String choiceFilter;

    /**
     * resourceType for ReferenceField type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ReferenceType resourceType;

    @Column(name = "constraint")
    private String constraint;

    @Type(JsonType.class)
    @Column(name = "constraint_message", columnDefinition = "jsonb")
    private Map<String, String> constraintMessage;

    @Column(name = "gs1_enabled")
    private Boolean gs1Enabled;

    @Type(JsonType.class)
    @Column(name = "scan_properties", columnDefinition = "jsonb")
    private ScannedCodeProperties properties;

//    /**
//     *  for referenceType ReferenceType.Stage
//     */
//    private StageReference stageReference;

    private ValueTypeRendering valueTypeRendering = ValueTypeRendering.DEFAULT;

    private AggregationType aggregationType = AggregationType.DEFAULT;

    @Override
    public FormDataElementConf path(String path) {
        this.setPath(path);
        return this;
    }

    public FormDataElementConf type(ValueType type) {
        this.setType(type);
        return this;
    }
}
