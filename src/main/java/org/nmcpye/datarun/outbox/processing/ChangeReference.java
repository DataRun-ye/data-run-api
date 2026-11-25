package org.nmcpye.datarun.outbox.processing;

import java.util.Objects;

/**
 * The change an output applies, as the runtime hands it over: the submission it changed, and the version the change
 * made. The runtime builds it from its queue's record; an output sees nothing else of the queue, so a change of queue
 * (the journal as the change record, say) touches only the runtime.
 *
 * @param submissionId the changed submission's id ({@code data_submission.id})
 * @param version      the submission's version after the change, or {@code null} for a change recorded before
 *                     versions were (changes queued before the journal)
 */
public record ChangeReference(String submissionId, Long version) {

    public ChangeReference {
        Objects.requireNonNull(submissionId, "submissionId");
    }
}
