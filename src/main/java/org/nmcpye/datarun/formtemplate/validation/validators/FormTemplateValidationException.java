package org.nmcpye.datarun.formtemplate.validation.validators;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;

/**
 * @author Hamza, 26/03/2025
 */
public class FormTemplateValidationException extends IllegalQueryException {
    private final FormValidationResult result;

    public FormTemplateValidationException(FormValidationResult result) {
        super(new ErrorMessage(ErrorCode.E1111, result.toString()));
        this.result = result;
    }

    public FormValidationResult getResult() {
        return result;
    }
}
