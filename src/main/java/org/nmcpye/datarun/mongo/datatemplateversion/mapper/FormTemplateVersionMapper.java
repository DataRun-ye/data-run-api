package org.nmcpye.datarun.mongo.datatemplateversion.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.common.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.datatemplateelement.ElementValidationRule;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.RuleAction;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormTemplateVersionMapper
    extends BaseMapper<FormTemplateVersionDto, DataTemplateVersion> {

    @Mappings({
        @Mapping(target = "uid", ignore = true),
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "versionNumber", ignore = true),
    })
    DataTemplateVersion fromInstanceDto(DataTemplateInstanceDto dto);

    /// TODO remove legacy constraints and remove this after migration
    /**
     * After MapStruct has populated all of the simple fields (including `List<FormDataElementConf> fields`),
     * this hook is invoked so that we can compute and set validationRule and valueTypeRendering on each element.
     */
    @AfterMapping
    default void populateFieldLevelDetails(
        @MappingTarget DataTemplateVersion entity,
        DataFormTemplate legacy
    ) {
        if (entity.getFields() == null) {
            return;
        }
        for (FormDataElementConf element : entity.getFields()) {
            // 1) Compute ElementValidationRule from (flat) constraint / constraintMessage / rules[]
            ElementValidationRule vr = validationRuleMap(element);
            element.setValidationRule(vr);

            // 2) Compute the ValueTypeRendering enum from element.getType() + element.getGs1Enabled()
            ValueTypeRendering vtr = valueTypeRenderingMap(element);
            element.setValueTypeRendering(vtr);
        }
    }

    /**
     * Your existing helper: if the DTO's FormDataElementConf already has a non‐null validationRule, use it;
     * otherwise build one from constraint/constraintMessage or from any RuleAction.Error in element.getRules().
     */
    default ElementValidationRule validationRuleMap(FormDataElementConf element) {
        if (element.getValidationRule() != null) {
            return element.getValidationRule();
        } else if (element.getConstraint() != null) {
            return new ElementValidationRule()
                .setExpression(element.getConstraint())
                .setValidationMessage(element.getConstraintMessage());
        } else {
            var maybeError = element.getRules()
                .stream()
                .filter(r -> r.getAction() == RuleAction.Error)
                .findAny();
            if (maybeError.isPresent()) {
                return new ElementValidationRule()
                    .setExpression(maybeError.get().getExpression())
                    .setValidationMessage(maybeError.get().getMessage());
            }
        }
        return null;
    }

    /**
     * Your existing helper: if the field's type is ScannedCode + gs1Enabled=true → GS1_DATAMATRIX,
     * if ScannedCode + gs1Enabled=false → BAR_CODE, otherwise DEFAULT.
     */
    default ValueTypeRendering valueTypeRenderingMap(FormDataElementConf element) {
        if (element.getType() == ValueType.ScannedCode) {
            if (Boolean.TRUE.equals(element.getGs1Enabled())) {
                return ValueTypeRendering.GS1_DATAMATRIX;
            }
            return ValueTypeRendering.BAR_CODE;
        }
        return ValueTypeRendering.DEFAULT;
    }
}
