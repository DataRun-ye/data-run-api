package org.nmcpye.datarun.captureshadow;

import org.nmcpye.datarun.assignmentshadow.VersionedUploadAuthorityReceipt;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;

import java.util.Objects;

public record VersionedCaptureSubmissionCommand(
    DataSubmission submission,
    VersionedUploadAuthorityReceipt authority
) {
    public VersionedCaptureSubmissionCommand {
        Objects.requireNonNull(submission);
        Objects.requireNonNull(authority);
    }
}
