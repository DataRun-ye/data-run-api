package org.nmcpye.datarun.captureshadow.bootstrap;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.nmcpye.datarun.captureshadow.bootstrap.CaptureShadowBootstrapReport.ItemCount;

@Service
public class CaptureShadowBootstrap {

    public static final int BATCH_SIZE = 250;
    public static final long ADVISORY_LOCK_KEY = 0x4452434150545552L;
    static final long INITIAL_SERIAL_CURSOR = Long.MIN_VALUE;

    private final DataSource dataSource;
    private final CaptureSourceSnapshot sourceSnapshot;
    private final CaptureBootstrapBatchTransaction batchTransaction;
    private final CaptureBootstrapFinalTransaction finalTransaction;

    public CaptureShadowBootstrap(
        DataSource dataSource,
        CaptureSourceSnapshot sourceSnapshot,
        CaptureBootstrapBatchTransaction batchTransaction,
        CaptureBootstrapFinalTransaction finalTransaction
    ) {
        this.dataSource = dataSource;
        this.sourceSnapshot = sourceSnapshot;
        this.batchTransaction = batchTransaction;
        this.finalTransaction = finalTransaction;
    }

    public CaptureShadowBootstrapReport run() {
        try (Connection lockConnection = dataSource.getConnection()) {
            lockConnection.setAutoCommit(true);
            acquireLock(lockConnection);
            try {
                return runLocked();
            } finally {
                releaseLock(lockConnection);
            }
        } catch (SQLException exception) {
            throw new CaptureShadowBootstrapConflictException(
                "Capture bootstrap advisory lock failed",
                exception
            );
        }
    }

    private CaptureShadowBootstrapReport runLocked() {
        CaptureSourceBoundary boundary = sourceSnapshot.capture();
        MutableProgress progress = new MutableProgress();
        long lastSerial = INITIAL_SERIAL_CURSOR;
        while (true) {
            CaptureBootstrapBatchResult batch = batchTransaction.persistNextBatch(
                boundary,
                lastSerial
            );
            if (batch.rows() == 0) {
                break;
            }
            progress.add(batch);
            lastSerial = batch.nextSerial();
        }
        CaptureShadowBootstrapReport report = finalTransaction.compareAndCheckpoint(
            boundary,
            progress.orgUnitAliases(),
            progress.identities(),
            progress.events(),
            progress.currentPointers()
        );
        if (!report.successful()) {
            throw new CaptureShadowBootstrapMismatchException(report);
        }
        return report;
    }

    private void acquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT pg_try_advisory_lock(?)"
        )) {
            statement.setLong(1, ADVISORY_LOCK_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || !resultSet.getBoolean(1)) {
                    throw new CaptureShadowBootstrapConflictException(
                        "Another capture bootstrap command holds the advisory lock"
                    );
                }
            }
        }
    }

    private void releaseLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT pg_advisory_unlock(?)"
        )) {
            statement.setLong(1, ADVISORY_LOCK_KEY);
            statement.execute();
        }
    }

    private static final class MutableProgress {
        private long orgUnitAliasesCreated;
        private long orgUnitAliasesExisting;
        private long identitiesCreated;
        private long identitiesExisting;
        private long eventsCreated;
        private long eventsExisting;
        private long currentPointersCreated;
        private long currentPointersExisting;

        private void add(CaptureBootstrapBatchResult batch) {
            orgUnitAliasesCreated += batch.orgUnitAliasesCreated();
            orgUnitAliasesExisting += batch.orgUnitAliasesExisting();
            identitiesCreated += batch.identitiesCreated();
            identitiesExisting += batch.identitiesExisting();
            eventsCreated += batch.eventsCreated();
            eventsExisting += batch.eventsExisting();
            currentPointersCreated += batch.currentPointersCreated();
            currentPointersExisting += batch.currentPointersExisting();
        }

        private ItemCount orgUnitAliases() {
            return new ItemCount(orgUnitAliasesCreated, orgUnitAliasesExisting);
        }

        private ItemCount identities() {
            return new ItemCount(identitiesCreated, identitiesExisting);
        }

        private ItemCount events() {
            return new ItemCount(eventsCreated, eventsExisting);
        }

        private ItemCount currentPointers() {
            return new ItemCount(currentPointersCreated, currentPointersExisting);
        }
    }
}
