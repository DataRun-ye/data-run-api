package org.nmcpye.datarun.templateelementconf;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.dataelement.DataElement;
import org.nmcpye.datarun.drun.postgres.domain.formtemplate.FormTemplateVersion;
import org.nmcpye.datarun.mongo.domain.DataFieldRule;
import org.nmcpye.datarun.mongo.domain.dataelement.AggregationType;
import org.nmcpye.datarun.mongo.domain.dataelement.ElementInterface;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.util.List;
import java.util.Map;

/**
 * an {@link DataElementInstance} links an {@link FormTemplateVersion}
 * to an {@link DataElement}
 * and configure it with additional configuration properties
 *
 * @author Hamza Assada, <7amza.it@gmail.com> <29-05-2025>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
public class DataElementInstance implements ElementInterface<String> {
    private String id;
    private String name;
    private String code;
    private String dataElement;
    @Size(max = 2000)
    @Column(name = "description")
    private String description;
    @Column(name = "type")
    private ValueType type;
    private String path;
    private String parent;
    private Integer order;
    @Column(name = "calculation")
    private Object defaultValue;
    private Boolean mandatory;
    private Boolean readOnly;
    private Boolean mainField;
    private String optionSet;
    private String calculation;
    private String choiceFilter;
    private DataElementConstraint constraint;
    private ValueTypeRendering valueTypeRendering = ValueTypeRendering.DEFAULT;
    private AggregationType aggregationType = AggregationType.DEFAULT;
    private Boolean gs1Enabled;
    private ScannedCodeProperties properties;
    private Map<String, String> label;
    List<DataFieldRule> rules;

    @Override
    public DataElementInstance path(String path) {
        this.setPath(path);
        return this;
    }

    public DataElementInstance type(ValueType type) {
        this.setType(type);
        return this;
    }
}
