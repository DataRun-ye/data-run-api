package org.nmcpye.datarun.formtemplateprocessor.validation.validators;

/**
 * @author Hamza Assada, 26/03/2025
 */
public record ElementExpressionValidationError(
    String expression,
    String invalidReference,
    String message
) implements TemplateValidationError {
    @Override
    public String toString() {
        return """
            Validation Error:
              - Element: %s
              - Context: %s
              - Message: %s
            """.formatted(expression, invalidReference, message);
    }
}
