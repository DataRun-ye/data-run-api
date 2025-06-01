package org.nmcpye.datarun.datatemplateprocessor.validation.validators;

import org.nmcpye.datarun.datatemplateversion.DataTemplateVersionInterface;

/**
 * @author Hamza Assada, 25/03/2025
 */
public interface ExpressionValidator {
    /**
     * Validate an expression against the given DataFormTemplate.
     *
     * @param expression the expression string to validate
     * @param template   the DataFormTemplate containing available element names
     * @return a ValidationResult indicating success or containing error messages.
     */
    ElementValidationResult validate(String expression, DataTemplateVersionInterface template);
}
