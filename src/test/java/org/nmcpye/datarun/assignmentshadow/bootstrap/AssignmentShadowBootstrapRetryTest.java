package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentShadowBootstrapRetryTest {

    @Test
    void retriesTheWholeTransactionAfterSerializationFailure() {
        AssignmentShadowBootstrapTransaction transaction =
            mock(AssignmentShadowBootstrapTransaction.class);
        AssignmentShadowBootstrapReport expected = emptyReport();
        when(transaction.runOnce())
            .thenThrow(new PessimisticLockingFailureException("stale snapshot"))
            .thenReturn(expected);

        AssignmentShadowBootstrapReport actual = new AssignmentShadowBootstrap(transaction).run();

        assertThat(actual).isSameAs(expected);
        verify(transaction, times(2)).runOnce();
    }

    @Test
    void stopsAfterTheBoundedSerializationRetryLimit() {
        AssignmentShadowBootstrapTransaction transaction =
            mock(AssignmentShadowBootstrapTransaction.class);
        when(transaction.runOnce())
            .thenThrow(new PessimisticLockingFailureException("stale snapshot"));

        assertThatThrownBy(() -> new AssignmentShadowBootstrap(transaction).run())
            .isInstanceOf(AssignmentShadowBootstrapConflictException.class)
            .hasMessageContaining("after 3 attempts");
        verify(transaction, times(AssignmentShadowBootstrap.MAX_TRANSACTION_ATTEMPTS)).runOnce();
    }

    private static AssignmentShadowBootstrapReport emptyReport() {
        AssignmentShadowBootstrapReport.ItemCount zero =
            new AssignmentShadowBootstrapReport.ItemCount(0, 0);
        return new AssignmentShadowBootstrapReport(
            0, 0, 0, 0, List.of(), zero, zero, zero, zero, zero, zero, zero,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
    }
}
