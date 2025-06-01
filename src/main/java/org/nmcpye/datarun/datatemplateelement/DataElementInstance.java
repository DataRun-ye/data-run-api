package org.nmcpye.datarun.datatemplateelement;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.dataelement.DataElement;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.datatemplateversion.DataTemplateTemplateVersion;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;

import java.util.Objects;

/**
 * an {@link DataElementInstance} links an {@link DataTemplateTemplateVersion}
 * to an {@link DataElement}
 * and configure it with additional configuration properties
 *
 * @author Hamza Assada, <7amza.it@gmail.com> <29-05-2025>
 */
@Getter
@Setter
@NoArgsConstructor
public class DataElementInstance extends AbstractElement {
    private String uid;
    private String dataElement;
    @Column(name = "type")
    private ValueType type;
    @Column(name = "calculation")
    private Object defaultValue;
    private Boolean mandatory;
    private Boolean readOnly;
    private Boolean mainField;
    private String optionSet;
    private String calculation;
    private String choiceFilter;
    private ElementValidationRule validationRule;
    private ValueTypeRendering valueTypeRendering = ValueTypeRendering.DEFAULT;
    private AggregationType aggregationType = AggregationType.DEFAULT;
    private Boolean gs1Enabled;
    private ScannedCodeProperties properties;

    @Override
    public DataElementInstance path(String path) {
        this.setPath(path);
        return this;
    }

    public DataElementInstance type(ValueType type) {
        this.setType(type);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataElementInstance that)) return false;
        return Objects.equals(getUid(), that.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUid());
    }
}
