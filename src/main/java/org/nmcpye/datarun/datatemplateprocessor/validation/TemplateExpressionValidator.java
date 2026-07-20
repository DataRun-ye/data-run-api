package org.nmcpye.datarun.datatemplateprocessor.validation;

import org.nmcpye.datarun.datatemplateelement.DataFieldRule;
import org.nmcpye.datarun.datatemplateelement.FieldTemplateElementDto;
import org.nmcpye.datarun.datatemplateelement.SectionTemplateElementDto;
import org.nmcpye.datarun.datatemplateprocessor.validation.validators.*;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateVersionInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 25/03/2025 (7amza.it@gmail.com)
 */
@Component
public class TemplateExpressionValidator
    implements TemplateValidator {

    private final List<ExpressionValidator> validators = List.of(new ElementReferenceValidator());

    /**
     * Validates all expressions (rules and constraints) within a DataFormTemplate.
     *
     * @param template the template to validate.
     * @return a ValidationResult summarizing all issues.
     */
    @Override
    public TemplateValidationResult validate(DataTemplateVersionInterface template) {
        List<FormValidationError> errors = new ArrayList<>();

        // Validate rule expressions in fields.
        for (FieldTemplateElementDto field : template.getFields()) {
            if (field.getRules() != null) {
                for (DataFieldRule rule : field.getRules()) {
                    String expression = rule.getExpression();
                    for (ExpressionValidator validator : validators) {
                        ElementValidationResult result = validator.validate(expression, template);
                        if (!result.isValid()) {
                            errors.addAll(result.getErrors().stream()
                                .map(err -> new FormValidationError(field.getName(), "rule error", err))
                                .toList());
                            // "Field '" + field.getName() + "' rule error: " + err
                        }
                    }
                }
            }
            // Validate constraint expressions if present.

            String expression = field.getConstraint() != null && !field.getConstraint().isEmpty() ?
                field.getConstraint() : field.getValidationRule() != null ?
                field.getValidationRule().getExpression() : null;
            if (expression != null) {
                for (ExpressionValidator validator : validators) {
                    ElementValidationResult result = validator.validate(expression.trim(), template);
                    if (!result.isValid()) {
                        errors.addAll(result.getErrors().stream()
                            .map(err -> new FormValidationError(field.getName(), "constraint error", err))
                            .toList());
                    }
                }
            }

        }

        // Similarly, validate rules in sections
        for (SectionTemplateElementDto section : template.getSections()) {
            if (section.getRules() != null) {
                for (DataFieldRule rule : section.getRules()) {
                    String expression = rule.getExpression();
                    for (ExpressionValidator validator : validators) {
                        ElementValidationResult result = validator.validate(expression, template);
                        if (!result.isValid()) {
                            errors.addAll(result.getErrors().stream()
                                .map(err -> new FormValidationError(section.getName(), "Section", err))
                                .toList());
                        }
                    }
                }
            }
        }

        return errors.isEmpty() ? TemplateValidationResult.valid() : TemplateValidationResult.invalid(errors);
    }
}
