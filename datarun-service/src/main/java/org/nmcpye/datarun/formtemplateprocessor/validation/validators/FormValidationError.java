package org.nmcpye.datarun.formtemplateprocessor.validation.validators;

/**
 * @author Hamza Assada, 26/03/2025
 */
public record FormValidationError(
    String sourceElement,
    String contextType,
    TemplateValidationError elementValidationError
) implements TemplateValidationError {
    @Override
    public String toString() {
        return """
            Validation Error:
              - Element: %s
              - Context: %s
              - Element Validation Error: %s
            """.formatted(sourceElement,
            contextType,
            elementValidationError.toString());
    }
}
