package org.nmcpye.datarun.web.v2.idempotency;

/**
 * The work of one v2 command, run by {@link IdempotentCommands} inside the transaction that claimed its key. In
 * order, it locks the resource's row (its first read: {@code SELECT ... FOR UPDATE}, or {@code FOR NO KEY UPDATE},
 * which a JPA pessimistic write lock issues on PostgreSQL), calls {@link CommandContext#checkVersion}, applies the
 * change and its records, and returns the outcome. Throwing rolls everything back, the key's claim included.
 *
 * <p>{@code FOR NO KEY UPDATE} is correct whenever the change leaves the row's key columns as they are: it keeps out
 * every other writer of the row, as {@code FOR UPDATE} would, and lets only foreign-key checks ({@code KEY SHARE})
 * through. A submission's lock is one ({@code SubmissionLock}): no key column of a submission ever changes, and the
 * journal's foreign-key checks on it stay compatible.</p>
 */
@FunctionalInterface
public interface Command {

    CommandOutcome run(CommandContext context);
}
