package org.nmcpye.datarun.datatemplateprocessor.validation.validators;

/**
 * @author Hamza Assada, 26/03/2025
 */
public record ElementGeneralValidationError(
    String message
) implements TemplateValidationError {
    @Override
    public String toString() {
        return """
            Duplicate Validation Error:
              - message: %s
            """.formatted(message);
    }
}
