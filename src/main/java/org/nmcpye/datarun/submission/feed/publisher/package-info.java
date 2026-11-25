/**
 * The submission feed's publisher: the only writer of schemas {@code feed} and {@code feed_ops}
 * ({@code FeedWriteOwnershipTest}). It writes only while the switch {@code submission-feed} is on
 * ({@link org.nmcpye.datarun.submission.feed.FeedActivation}).
 *
 * <p>The dictionary ({@code feed.form_field}, {@code feed.form_section}) is rewritten in place, in one transaction, at
 * start and whenever the stored form definitions change ({@link DictionaryPublisher}).</p>
 *
 * <p>The submission streams ({@code feed.submission}, {@code feed.submission_row}) are written from the journal's heads
 * by {@link SubmissionPublisher}, the ETL's entry point into this package, under the feed's lock ({@code FeedLock}).</p>
 */
package org.nmcpye.datarun.submission.feed.publisher;
