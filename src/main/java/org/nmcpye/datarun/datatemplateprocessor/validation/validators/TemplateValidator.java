package org.nmcpye.datarun.datatemplateprocessor.validation.validators;

import org.nmcpye.datarun.datatemplateversion.DataTemplateVersionInterface;

/**
 * @author Hamza Assada, 25/03/2025
 */
public interface TemplateValidator {
    /**
     * Validate an expression against the given DataFormTemplate.
     *
     * @param template the DataFormTemplate containing available element names
     * @return a ValidationResult indicating success or containing error messages.
     */
    <T extends DataTemplateVersionInterface> TemplateValidationResult validate(T template);
}
