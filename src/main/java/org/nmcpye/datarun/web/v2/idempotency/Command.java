package org.nmcpye.datarun.web.v2.idempotency;

/**
 * The work of one v2 command, run by {@link IdempotentCommands} inside the transaction that claimed its key. In
 * order, it locks the resource's row (its first read, {@code SELECT ... FOR UPDATE}), calls
 * {@link CommandContext#checkVersion}, applies the change and its records, and returns the outcome. Throwing rolls
 * everything back, the key's claim included.
 */
@FunctionalInterface
public interface Command {

    CommandOutcome run(CommandContext context);
}
