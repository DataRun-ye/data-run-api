package org.nmcpye.datarun.outbox.processing;

/**
 * Work an output keeps for when no outbox change is due, such as the submission feed's retries, republication and
 * reconcile. The runtime that claims outbox rows runs it one batch at a time, claiming the live changes first again
 * after each batch, for as long as its poll may run.
 */
public interface DeferredWork {

    /** Whether there is such work in this process: work behind a switch is off while the switch is. */
    boolean isOn();

    /**
     * Runs one batch, in transactions of the work's own.
     *
     * @return whether it did anything; false when nothing waits
     */
    boolean runBatch();
}
