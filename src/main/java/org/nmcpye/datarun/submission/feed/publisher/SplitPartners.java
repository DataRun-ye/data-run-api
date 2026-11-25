package org.nmcpye.datarun.submission.feed.publisher;

import java.util.List;

/**
 * The submissions a revision was split with (contract 8, item 5): when the publisher writes a submission's head, it
 * writes the heads of these submissions in the same feed part, so a split lands at once and no consumer sees the moved
 * rows under both submissions, or under neither. The partner's own outbox row then finds its head held and is skipped.
 *
 * <p>No change splits a submission yet, so the application declares no implementation and the publisher uses
 * {@link #NONE}. Submission review's split, when it is built, records the two revisions naming each other and supplies
 * them here.</p>
 */
interface SplitPartners {

    /** No submission has split partners. */
    SplitPartners NONE = (submissionId, version) -> List.of();

    /**
     * The ids of the submissions whose heads are written with this submission's head.
     *
     * @param submissionId the submission being published
     * @param version      the version of its head
     */
    List<String> of(String submissionId, long version);
}
