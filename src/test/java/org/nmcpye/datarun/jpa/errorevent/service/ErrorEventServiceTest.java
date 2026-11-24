package org.nmcpye.datarun.jpa.errorevent.service;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorEventServiceTest {

    /** The printed trace keeps every exception's type and frames, as printStackTrace does, and none of its messages. */
    @Test
    void theStoredStackTraceNamesTypesAndFramesWithoutMessages() {
        SQLException root = new SQLException("Key (name)=(secret-root) already exists", "23505");
        IllegalStateException failure = new IllegalStateException("could not save secret-outer", root);
        failure.addSuppressed(new IllegalArgumentException("closing secret-suppressed"));

        String trace = ErrorEventService.stackTraceWithoutMessages(failure);

        assertThat(trace)
            .startsWith("java.lang.IllegalStateException\n\tat ")
            .contains("\tSuppressed: java.lang.IllegalArgumentException\n")
            .contains("Caused by: java.sql.SQLException\n")
            .contains("theStoredStackTraceNamesTypesAndFramesWithoutMessages")
            .doesNotContain("secret-root", "secret-outer", "secret-suppressed");
    }

    @Test
    void aCircularCauseIsPrintedOnce() {
        IllegalStateException first = new IllegalStateException("secret-first");
        IllegalArgumentException second = new IllegalArgumentException("secret-second", first);
        first.initCause(second);

        String trace = ErrorEventService.stackTraceWithoutMessages(first);

        assertThat(trace).contains("Caused by: java.lang.IllegalArgumentException\n")
            .contains("[CIRCULAR REFERENCE: java.lang.IllegalStateException]")
            .doesNotContain("secret-first", "secret-second");
    }
}
