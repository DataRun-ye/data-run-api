package org.nmcpye.datarun.outbox.repository;

import java.util.List;

public interface OutboxWritePort {

    /** Inserts the rows, pending, in the caller's transaction. */
    int insert(List<OutboxInsert> inserts);

    /**
     * One outbox row: it announces the submission's revision at {@code submissionVersion}, which the submission
     * persistence layer has just written in the same transaction. Its time comes from the database.
     *
     * @param topic             the submission's form version uid
     * @param payload           the submission's form data, as JSON text
     * @param submissionVersion the submission's version after the change: with {@code submissionId}, the key of
     *                          the revision it announces
     * @param eventType         SAVE, UPDATE or DELETE: DELETE whenever the submission is deleted after the change
     */
    record OutboxInsert(
        String submissionId,
        String submissionUid,
        String topic,
        String payload,
        Long submissionSerialNumber,
        long submissionVersion,
        String eventType) {
    }
}
