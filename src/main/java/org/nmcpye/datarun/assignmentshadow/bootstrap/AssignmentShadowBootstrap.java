package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class AssignmentShadowBootstrap {

    static final int BATCH_SIZE = 1_000;
    static final int MAX_TRANSACTION_ATTEMPTS = 3;

    private final AssignmentShadowBootstrapTransaction transaction;

    public AssignmentShadowBootstrap(AssignmentShadowBootstrapTransaction transaction) {
        this.transaction = transaction;
    }

    public AssignmentShadowBootstrapReport run() {
        for (int attempt = 1; attempt <= MAX_TRANSACTION_ATTEMPTS; attempt++) {
            try {
                return transaction.runOnce();
            } catch (PessimisticLockingFailureException exception) {
                if (attempt == MAX_TRANSACTION_ATTEMPTS) {
                    throw new AssignmentShadowBootstrapConflictException(
                        "Bootstrap transaction did not stabilize after "
                            + MAX_TRANSACTION_ATTEMPTS + " attempts",
                        exception
                    );
                }
            }
        }
        throw new IllegalStateException("Unreachable bootstrap retry state");
    }
}
