/**
 * The submission feed (docs/features/submission-feed): the current state of every submission for downstream
 * analytics, published in schema {@code feed} by contract version 1, with the publisher's bookkeeping in schema
 * {@code feed_ops}.
 *
 * <p>This package holds what surrounds the publisher: downstream's reading role and the startup check of its grant.
 * Only the publisher ({@code submission.feed.publisher}) writes the two schemas.</p>
 */
package org.nmcpye.datarun.submission.feed;
