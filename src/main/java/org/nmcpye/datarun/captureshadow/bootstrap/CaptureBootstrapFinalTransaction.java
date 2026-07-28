package org.nmcpye.datarun.captureshadow.bootstrap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.nmcpye.datarun.captureshadow.bootstrap.CaptureShadowBootstrapReport.ItemCount;

@Component
public class CaptureBootstrapFinalTransaction {

    private final JdbcTemplate jdbc;
    private final CaptureSourceSnapshot sourceSnapshot;
    private final CaptureComparison comparison;
    private final CaptureCheckpointStore checkpointStore;

    CaptureBootstrapFinalTransaction(
        JdbcTemplate jdbc,
        CaptureSourceSnapshot sourceSnapshot,
        CaptureComparison comparison,
        CaptureCheckpointStore checkpointStore
    ) {
        this.jdbc = jdbc;
        this.sourceSnapshot = sourceSnapshot;
        this.comparison = comparison;
        this.checkpointStore = checkpointStore;
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.REPEATABLE_READ
    )
    public CaptureShadowBootstrapReport compareAndCheckpoint(
        CaptureSourceBoundary initialBoundary,
        ItemCount orgUnitAliases,
        ItemCount identities,
        ItemCount events
    ) {
        assertRepeatableRead();
        CaptureSourceBoundary finalBoundary = sourceSnapshot.captureCurrentTransaction();
        boolean sourceMoved = !initialBoundary.sameSourceAs(finalBoundary);
        CaptureComparisonResult result = comparison.compare(finalBoundary);
        List<String> samples = new ArrayList<>(result.diagnosticSamples());
        if (sourceMoved && samples.size() < CaptureComparison.MAX_DIAGNOSTIC_SAMPLES) {
            samples.add(
                "source boundary changed initial=" + initialBoundary.sourceSha256()
                    + " final=" + finalBoundary.sourceSha256()
            );
        }

        CaptureCheckpointStore.Status checkpointStatus = checkpointStore.status(finalBoundary);
        long checkpointDifference = checkpointStatus == CaptureCheckpointStore.Status.MISMATCH
            ? 1
            : 0;
        ItemCount checkpoints = checkpointStatus == CaptureCheckpointStore.Status.EXACT
            ? new ItemCount(0, 1)
            : new ItemCount(0, 0);

        boolean exactBeforeCheckpoint = !sourceMoved
            && result.differenceCount() == 0
            && checkpointDifference == 0;
        if (exactBeforeCheckpoint && checkpointStatus == CaptureCheckpointStore.Status.ABSENT) {
            checkpointStore.insert(finalBoundary);
            checkpoints = new ItemCount(1, 0);
        }

        return new CaptureShadowBootstrapReport(
            finalBoundary,
            orgUnitAliases,
            identities,
            events,
            checkpoints,
            sourceMoved ? 1 : 0,
            result.missingIdentityCount(),
            result.identityDifferenceCount(),
            result.extraIdentityCount(),
            result.missingOrgUnitAliasCount(),
            result.orgUnitAliasDifferenceCount(),
            result.missingEventCount(),
            result.eventDifferenceCount(),
            result.extraEventCount(),
            checkpointDifference,
            samples
        );
    }

    private void assertRepeatableRead() {
        String isolation = jdbc.queryForObject("SHOW transaction_isolation", String.class);
        if (!"repeatable read".equals(isolation)) {
            throw new CaptureShadowBootstrapConflictException(
                "Final comparison requires REPEATABLE_READ but transaction isolation is "
                    + isolation
            );
        }
    }
}
