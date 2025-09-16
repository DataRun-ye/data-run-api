package org.nmcpye.datarun.jpa.datatemplate.mapper;

import org.mapstruct.*;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface FormJpaTemplateVersionMapper
    extends BaseMapper<FormTemplateVersionDto, TemplateVersion> {

    @Mappings({
        @Mapping(target = "uid", ignore = true),
//        @Mapping(target = "id", ignore = true),
        @Mapping(target = "versionNumber", ignore = true),
        @Mapping(target = "templateUid", source = "uid")
    })
    TemplateVersion fromInstanceDto(DataTemplateInstanceDto dto);
//
//    /**
//     * After MapStruct has populated all of the simple fields (including `List<FormDataElementConf> fields`),
//     * this hook is invoked so that we can compute and set validationRule and valueTypeRendering on each element.
//     */
//    @AfterMapping
//    default void populateFieldLevelDetails(
//        @MappingTarget DataTemplateVersion entity,
//        DataFormTemplate legacy
//    ) {
//        if (entity.getFields() == null) {
//            return;
//        }
//        for (FormDataElementConf element : entity.getFields()) {
//            ElementValidationRule vr = validationRuleMap(element);
//            element.setValidationRule(vr);
//
//            ValueTypeRendering vtr = valueTypeRenderingMap(element);
//            element.setValueTypeRendering(vtr);
//        }
//    }

//    /**
//     * existing helper: if the DTO's FormDataElementConf already has a non‐null validationRule, use it;
//     * otherwise build one from constraint/constraintMessage or from any RuleAction.Error in element.getRules().
//     */
//    default ElementValidationRule validationRuleMap(FormDataElementConf element) {
//        if (element.getValidationRule() != null) {
//            return element.getValidationRule();
//        } else if (element.getConstraint() != null) {
//            return new ElementValidationRule()
//                .setExpression(element.getConstraint())
//                .setValidationMessage(element.getConstraintMessage());
//        } else {
//            var maybeError = element.getRules()
//                .stream()
//                .filter(r -> r.getAction() == RuleAction.Error)
//                .findAny();
//            if (maybeError.isPresent()) {
//                return new ElementValidationRule()
//                    .setExpression(maybeError.get().getExpression())
//                    .setValidationMessage(maybeError.get().getMessage());
//            }
//        }
//        return null;
//    }

//    /**
//     * Your existing helper: if the field's type is ScannedCode + gs1Enabled=true → GS1_DATAMATRIX,
//     * if ScannedCode + gs1Enabled=false → BAR_CODE, otherwise DEFAULT.
//     */
//    default ValueTypeRendering valueTypeRenderingMap(FormDataElementConf element) {
//        if (element.getType() == ValueType.ScannedCode) {
//            if (Boolean.TRUE.equals(element.getGs1Enabled())) {
//                return ValueTypeRendering.GS1_DATAMATRIX;
//            }
//            return ValueTypeRendering.BAR_CODE;
//        }
//        return ValueTypeRendering.DEFAULT;
//    }
}
