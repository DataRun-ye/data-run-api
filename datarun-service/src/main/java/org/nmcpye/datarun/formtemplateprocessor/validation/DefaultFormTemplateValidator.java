package org.nmcpye.datarun.formtemplateprocessor.validation;

import org.nmcpye.datarun.formtemplate.validation.validators.FormTemplateValidationException;
import org.nmcpye.datarun.formtemplate.validation.validators.TemplateValidator;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Hamza Assada, 26/03/2025
 */
@Component
public class DefaultFormTemplateValidator {
    final List<? extends TemplateValidator> validators;

    public DefaultFormTemplateValidator(List<? extends TemplateValidator> validators) {
        this.validators = validators;
    }

    public <T extends FormWithFields> void validate(T formTemplate) {
        for (final var validator : validators) {
            final var result = validator.validate(formTemplate);
            if (!result.isValid()) {
                throw new FormTemplateValidationException(result);
            }
        }
    }
}
