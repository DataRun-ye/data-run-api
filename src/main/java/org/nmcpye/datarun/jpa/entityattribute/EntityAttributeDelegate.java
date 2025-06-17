package org.nmcpye.datarun.jpa.entityattribute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.optionset.OptionSet;

/**
 * an {@link EntityAttributeDelegate} links an {@link EntityType} to an {@link EntityAttributeType}
 * and configure it with additional configuration properties
 *
 * @author Hamza Assada 29/05/2025 <7amza.it@gmail.com>
 */
@Getter
@Setter
public class EntityAttributeDelegate extends EntityAttributeType {

    @Getter(value = AccessLevel.PRIVATE)
    private EntityAttributeType attributeType;

    private Boolean displayInList = false;

    private Boolean searchable = false;

    public EntityAttributeDelegate(EntityAttributeType attributeType) {
        this.attributeType = attributeType;
    }

    @Override
    public String getId() {
        return getAttributeType().getId();
    }

    @Override
    public String getCode() {
        return getAttributeType().getCode();
    }

    @Override
    public String getName() {
        return getAttributeType().getName();
    }

    @Override
    public String getShortName() {
        return getAttributeType().getShortName();
    }

    @Override
    public String getDescription() {
        return getAttributeType().getDescription();
    }

    @Override
    public void setCode(String code) {
        getAttributeType().setCode(code);
    }

    @Override
    public void setName(String name) {
        getAttributeType().setName(name);
    }

    @Override
    public void setShortName(String shortName) {
        getAttributeType().setShortName(shortName);
    }

    @Override
    public void setDescription(String description) {
        getAttributeType().setDescription(description);
    }

    @Override
    public ValueType getValueType() {
        return getAttributeType().getValueType();
    }

    @Override
    public Boolean getMandatory() {
        return getAttributeType().getMandatory();
    }

    @Override
    public OptionSet getOptionSet() {
        return getAttributeType().getOptionSet();
    }

    @Override
    public Boolean getDisplayOnPlanned() {
        return getAttributeType().getDisplayOnPlanned();
    }

    @Override
    public void setValueType(ValueType valueType) {
        getAttributeType().setValueType(valueType);
    }

    @Override
    public void setMandatory(Boolean mandatory) {
        getAttributeType().setMandatory(mandatory);
    }

    @Override
    @JsonIgnoreProperties(value = {"options"}, allowSetters = true)
    public void setOptionSet(OptionSet optionSet) {
        getAttributeType().setOptionSet(optionSet);
    }

    @Override
    public void setDisplayOnPlanned(Boolean displayOnPlanned) {
        getAttributeType().setDisplayOnPlanned(displayOnPlanned);
    }
}
