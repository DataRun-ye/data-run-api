package org.nmcpye.datarun.importer.exception;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorMessage;

/**
 * Custom exception to force rollback or skip
 *
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
public class DryRunException extends IllegalQueryException {
    public DryRunException(String message) {
        super(message);
    }

    public DryRunException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
