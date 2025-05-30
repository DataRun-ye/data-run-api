package org.nmcpye.datarun.templateprocessor.validation.validators;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Hamza Assada, 25/03/2025
 */

public class FormValidationResult {
    private final boolean valid;
    private final List<? extends TemplateValidationError> errors;

    public FormValidationResult(boolean valid, List<? extends TemplateValidationError> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() {
        return valid;
    }

    public List<?> getErrors() {
        return errors;
    }

    public static FormValidationResult valid() {
        return new FormValidationResult(true, Collections.emptyList());
    }

    public static FormValidationResult invalid(List<? extends TemplateValidationError> errors) {
        return new FormValidationResult(false, errors);
    }

    @Override
    public String toString() {
        return "FormValidationResult{" +
            "errors=" + errors.stream().map(Objects::toString).toList() +
            '}';
    }
}
