package org.nmcpye.datarun.jpa.datatemplate.exception;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;

/**
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public class TemplateFieldValidationException extends IllegalQueryException {
    public TemplateFieldValidationException(String message) {
        super(message);
    }
}
