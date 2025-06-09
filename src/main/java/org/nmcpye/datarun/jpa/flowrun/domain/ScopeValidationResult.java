package org.nmcpye.datarun.jpa.flowrun.domain;

import org.nmcpye.datarun.common.feedback.ErrorMessage;

import java.util.Collections;
import java.util.List;

/**
 * @author Hamza Assada 25/03/2025 <7amza.it@gmail.com>
 */

public class ScopeValidationResult {
    private final boolean valid;
    private final List<ErrorMessage> errors;

    public ScopeValidationResult(boolean valid, List<ErrorMessage> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() {
        return valid;
    }

    public List<ErrorMessage> getErrors() {
        return errors;
    }

    public static ScopeValidationResult valid() {
        return new ScopeValidationResult(true, Collections.emptyList());
    }

    public static ScopeValidationResult invalid(List<ErrorMessage> errors) {
        return new ScopeValidationResult(false, errors);
    }

    @Override
    public String toString() {
        return "ScopeValidationResult{" +
            "valid=" + valid +
            ", errors=" + errors.stream().map(ErrorMessage::toString)
            .reduce("", (pre, current) -> pre + "\n" + current) +
            '}';
    }
}
