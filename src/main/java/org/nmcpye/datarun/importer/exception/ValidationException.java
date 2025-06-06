package org.nmcpye.datarun.importer.exception;

import java.util.List;

/**
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
