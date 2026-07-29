package org.nmcpye.datarun.captureshadow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class CaptureCurrentProjectionPersistenceIntegrationTest {

    private static final UUID CAPTURE_ID =
        UUID.fromString("81000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_CAPTURE_ID =
        UUID.fromString("81000000-0000-0000-0000-000000000002");
    private static final UUID EVENT_ID =
        UUID.fromString("82000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_EVENT_ID =
        UUID.fromString("82000000-0000-0000-0000-000000000002");

    @Autowired
    private CaptureCurrentProjectionPort currentProjection;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanShadowTables() {
        jdbc.execute(
            """
                TRUNCATE TABLE
                    transition_checkpoint,
                    capture_current_projection,
                    capture_identity_link,
                    event_journal
                RESTART IDENTITY CASCADE
                """
        );
    }

    @Test
    void schemaHasOnlyTheCurrentPointerColumnsAndRequiredConstraints() {
        assertThat(jdbc.queryForList(
            """
                SELECT column_name || ':' || data_type || ':' || is_nullable
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'capture_current_projection'
                ORDER BY ordinal_position
                """,
            String.class
        )).containsExactly(
            "capture_id:uuid:NO",
            "source_event_id:uuid:NO"
        );
        assertThat(jdbc.queryForList(
            """
                SELECT constraint_name || ':' || constraint_type
                FROM information_schema.table_constraints
                WHERE table_schema = current_schema()
                  AND table_name = 'capture_current_projection'
                  AND constraint_type <> 'CHECK'
                ORDER BY constraint_name
                """,
            String.class
        )).containsExactly(
            "fk_capture_current_identity:FOREIGN KEY",
            "fk_capture_current_source_event:FOREIGN KEY",
            "pk_capture_current_projection:PRIMARY KEY",
            "uq_capture_current_source_event:UNIQUE"
        );
        assertThat(jdbc.queryForList(
            """
                SELECT tc.constraint_name || ':' || kcu.column_name
                       || '->' || ccu.table_name || '.' || ccu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_schema = tc.constraint_schema
                 AND kcu.constraint_name = tc.constraint_name
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_schema = tc.constraint_schema
                 AND ccu.constraint_name = tc.constraint_name
                WHERE tc.table_schema = current_schema()
                  AND tc.table_name = 'capture_current_projection'
                  AND tc.constraint_type = 'FOREIGN KEY'
                ORDER BY tc.constraint_name
                """,
            String.class
        )).containsExactly(
            "fk_capture_current_identity:capture_id->capture_identity_link.capture_id",
            "fk_capture_current_source_event:source_event_id->event_journal.event_id"
        );
    }

    @Test
    void strictInsertReadsPointerAndCannotOverwriteOrReuseIt() {
        insertIdentity(CAPTURE_ID, "D8100000001", "01JCUR00000000000000000001", 810_001);
        insertIdentity(OTHER_CAPTURE_ID, "D8100000002", "01JCUR00000000000000000002", 810_002);
        insertEvent(EVENT_ID);
        insertEvent(OTHER_EVENT_ID);

        currentProjection.insertBootstrapPointer(CAPTURE_ID, EVENT_ID);

        assertThat(currentProjection.findSourceEventId(CAPTURE_ID)).contains(EVENT_ID);
        assertThat(currentProjection.findSourceEventId(OTHER_CAPTURE_ID)).isEmpty();
        assertThatThrownBy(() ->
            currentProjection.insertBootstrapPointer(CAPTURE_ID, OTHER_EVENT_ID)
        ).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(currentProjection.findSourceEventId(CAPTURE_ID)).contains(EVENT_ID);
        assertThatThrownBy(() ->
            currentProjection.insertBootstrapPointer(OTHER_CAPTURE_ID, EVENT_ID)
        ).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(currentProjection.findSourceEventId(OTHER_CAPTURE_ID)).isEmpty();
    }

    @Test
    void outerTransactionRollbackRemovesInsertedPointer() {
        insertIdentity(CAPTURE_ID, "D8100000001", "01JCUR00000000000000000001", 810_001);
        insertEvent(EVENT_ID);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            currentProjection.insertBootstrapPointer(CAPTURE_ID, EVENT_ID);
            status.setRollbackOnly();
        });

        assertThat(currentProjection.findSourceEventId(CAPTURE_ID)).isEmpty();
    }

    private void insertIdentity(UUID captureId, String uid, String id, long serialNumber) {
        jdbc.update(
            """
                INSERT INTO capture_identity_link (
                    capture_id,
                    baseline_submission_uid,
                    baseline_submission_id,
                    baseline_serial_number
                ) VALUES (?, ?, ?, ?)
                """,
            captureId,
            uid,
            id,
            serialNumber
        );
    }

    private void insertEvent(UUID eventId) {
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id,
                    event_type,
                    shape_ref,
                    activity_ref,
                    subject_type,
                    subject_id,
                    actor_id,
                    recorded_at,
                    payload
                ) VALUES (?, 'capture', 'test/v1', NULL, 'subject', ?,
                          'system:test', now(), '{}'::jsonb)
                """,
            eventId,
            UUID.randomUUID()
        );
    }
}
