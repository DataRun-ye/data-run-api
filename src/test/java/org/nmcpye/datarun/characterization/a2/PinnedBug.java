package org.nmcpye.datarun.characterization.a2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a scenario whose golden freezes behaviour that is wrong today. The step named in {@link #changeBy()} is
 * expected to change that behaviour on purpose and to re-record the golden; until then a difference is a regression.
 * <p>
 * {@link A2GoldenSupport} writes the note into the golden file as
 * {@code "pinnedBug": {"changeBy": ..., "what": ...}} and appends it to the test's display name, so the bug is visible
 * wherever the test or its golden is read.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface PinnedBug {

    /** The planned step expected to change the behaviour (a data-chain plan label such as B2), or "none planned". */
    String changeBy();

    /** What is wrong, in one self-contained line. */
    String what();
}
