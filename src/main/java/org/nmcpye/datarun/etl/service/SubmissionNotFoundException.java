package org.nmcpye.datarun.etl.service;

/**
 * An outbox row names a submission that no longer exists, so there is nothing to project, now or on any later
 * try. Only a removal outside the application leads here: the outbox row is written with the submission.
 */
public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException() {
        super("submission not found");
    }
}
