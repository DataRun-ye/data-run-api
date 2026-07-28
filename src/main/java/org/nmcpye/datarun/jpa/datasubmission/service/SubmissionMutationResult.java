package org.nmcpye.datarun.jpa.datasubmission.service;

import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;

import java.util.Objects;

public record SubmissionMutationResult(
    SubmissionMutationKind kind,
    DataSubmission submission
) {
    public SubmissionMutationResult {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(submission);
    }
}
