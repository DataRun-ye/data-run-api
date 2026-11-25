/**
 * The submission feed (docs/features/submission-feed): the current state of every submission for downstream
 * analytics, published in schema {@code feed} by contract version 1, with the publisher's bookkeeping in schema
 * {@code feed_ops}.
 *
 * <p>This package holds what surrounds the publisher: its activation switch, and downstream's reader role, to which
 * the grants on schema {@code feed} converge at every start. Only the publisher ({@code submission.feed.publisher})
 * writes the two schemas.</p>
 */
package org.nmcpye.datarun.submission.feed;
