package org.nmcpye.datarun.jpa.etl.exception;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;

/**
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public class InvalidCategoryException extends IllegalQueryException {
    public InvalidCategoryException(String elementId, String valueType) {
        super(ErrorCode.E1122, elementId, valueType);
    }
}
