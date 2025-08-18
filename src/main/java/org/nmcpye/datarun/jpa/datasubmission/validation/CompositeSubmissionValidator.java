package org.nmcpye.datarun.jpa.datasubmission.validation;

import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
@Component
public class CompositeSubmissionValidator implements SubmissionValidator {

    private final List<SubmissionValidator> validators;

    public CompositeSubmissionValidator(List<SubmissionValidator> validators) {
        this.validators = validators;
    }

    @Override
    public DataSubmission validateAndEnrich(DataSubmission submission) {
        DataSubmission current = submission;
        for (SubmissionValidator v : validators) {
            current = v.validateAndEnrich(current);
        }
        return current;
    }
}
