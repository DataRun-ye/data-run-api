package org.nmcpye.datarun.outbox.processing;

import org.nmcpye.datarun.outbox.dto.OutboxDto;

import java.time.Duration;
import java.util.Optional;

/**
 * An output of the outbox's changes: what the runtime that claims outbox rows applies each change to. The projection
 * into tall and events is one; the submission feed is another. Outputs know nothing of each other or of the runtime,
 * and the runtime knows them only through this interface, so an output is added or retired without the others, or the
 * runtime's contract, changing.
 *
 * <p><b>How the runtime applies a change:</b> in one transaction, {@link #begin} of every output that is on, then
 * {@link #apply} of each, in order, each in a savepoint of its own, then the change's outcome on its outbox row. An
 * output that throws has what it wrote rolled back, and nothing else.</p>
 * <ul>
 *   <li>A failure the output keeps ({@link #keepsFailure}, its own retry list) leaves the change done for the
 *       runtime.</li>
 *   <li>One it does not keep is the runtime's: the outbox row is tried again after a growing wait, and is dead past its
 *       attempts. Every output then applies the change again, so each must find a change it already applied and do
 *       nothing.</li>
 *   <li>When a failure leaves the transaction unable to commit (its time ran out), the runtime applies the change again
 *       in a new transaction, to the outputs that did not fail, with the failure's outcome: no output's outcome waits for
 *       another's.</li>
 * </ul>
 */
public interface ChangeOutput {

    /** The output's name, for logs and for the runtime's messages. */
    String name();

    /** Whether the output takes changes in this process: one behind a switch is off while the switch is. */
    boolean isOn();

    /**
     * The longest one change may take in this output, which {@link #apply} is given as its deadline; empty when the
     * output keeps no limit of its own and runs within the runtime's row timeout. The runtime refuses to start while a
     * limit does not stay below its row timeout.
     */
    Optional<Duration> timeLimit();

    /**
     * Prepares the runtime's transaction before any output applies the change, outside every savepoint: where an
     * output takes the locks it holds until the commit, which no savepoint's rollback may release.
     */
    default void begin() {
    }

    /**
     * Applies one change in the runtime's transaction, in a savepoint of its own; throws when it could not.
     *
     * @param change        the claimed outbox row
     * @param deadlineNanos when the output's time is up, as a {@link System#nanoTime()} value
     */
    void apply(OutboxDto change, long deadlineNanos);

    /**
     * Takes a failure of {@link #apply}, after what it wrote was rolled back, still in the runtime's transaction.
     *
     * @return whether the output keeps the failure itself, in its own retry list, so the change is done for the
     *     runtime; false leaves it to the runtime
     */
    default boolean keepsFailure(OutboxDto change, RuntimeException failure) {
        return false;
    }
}
