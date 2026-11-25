package org.nmcpye.datarun.web.v2.idempotency;

/**
 * SEAM: renders a command's response body from its outcome, the resource at the outcome's version. The same
 * renderer answers the first request and every replay, so a replay returns what the first request returned.
 * <p>
 * Each module supplies its own. Rendering a past version needs that module's history: for submissions, the
 * revision journal (data-chain step C, not yet in production) read through the translation (step E1a). Until a
 * module has a history it cannot offer replayable commands. The renderer runs after the command's transaction has
 * committed and manages its own reads.
 */
@FunctionalInterface
public interface OutcomeRenderer<B> {

    B render(CommandOutcome outcome);
}
