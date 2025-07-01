package org.nmcpye.datarun.datatemplateprocessor.validation.validators;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;

/**
 * @author Hamza Assada 26/03/2025 (7amza.it@gmail.com)
 */
public class TemplateValidationException extends IllegalQueryException {
    private final TemplateValidationResult result;

    public TemplateValidationException(TemplateValidationResult result) {
        super(new ErrorMessage(ErrorCode.E1111, result.toString()));
        this.result = result;
    }

    public TemplateValidationResult getResult() {
        return result;
    }
}
