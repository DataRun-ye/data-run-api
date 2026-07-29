package org.nmcpye.datarun.eventjournal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class EventJournalPersistenceIntegrationTest {

    private static final Instant RECORDED_AT = Instant.parse("2026-07-28T09:30:00Z");

    @Autowired
    private EventJournalPort eventJournal;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanShadowTables() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            jdbc.execute("""
                TRUNCATE TABLE
                    transition_checkpoint,
                    assignment_grant_projection,
                    assignment_identity_link,
                    assignment_role_definition,
                    org_unit_identity_link,
                    actor_identity_link,
                    event_journal
                RESTART IDENTITY CASCADE
                """)
        );
    }

    @Test
    void roundTripsNativeUuidAndJsonbJournalValues() {
        UUID eventId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        AppendJournalEvent append = event(
            eventId,
            subjectId,
            assignmentCreatedPayload()
        );

        JournalEvent persisted = eventJournal.append(append);
        JournalEvent read = eventJournal.findByEventId(eventId).orElseThrow();
        JournalStorageTypes storageTypes = jdbc.queryForObject(
            """
                SELECT
                    pg_typeof(event_id)::text AS event_id_type,
                    pg_typeof(subject_id)::text AS subject_id_type,
                    pg_typeof(payload)::text AS payload_type
                FROM event_journal
                WHERE event_id = ?
                """,
            (resultSet, rowNumber) -> new JournalStorageTypes(
                resultSet.getString("event_id_type"),
                resultSet.getString("subject_id_type"),
                resultSet.getString("payload_type")
            ),
            eventId
        );

        assertThat(persisted.journalPosition()).isPositive();
        assertThat(read).isEqualTo(persisted);
        assertThat(read.eventId()).isEqualTo(eventId);
        assertThat(read.subjectId()).isEqualTo(subjectId);
        assertThat(read.recordedAt()).isEqualTo(RECORDED_AT);
        assertThat(read.payload()).isEqualTo(append.payload());
        assertThat(storageTypes).isEqualTo(new JournalStorageTypes("uuid", "uuid", "jsonb"));
    }

    @Test
    void duplicateEventIdsFail() {
        UUID eventId = UUID.randomUUID();
        eventJournal.append(event(eventId, UUID.randomUUID(), assignmentCreatedPayload()));

        assertThatThrownBy(() ->
            eventJournal.append(event(eventId, UUID.randomUUID(), assignmentCreatedPayload()))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transactionRollbackRemovesAnAppendedEvent() {
        UUID eventId = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            eventJournal.append(event(eventId, UUID.randomUUID(), assignmentCreatedPayload()));
            status.setRollbackOnly();
        });

        assertThat(eventJournal.findByEventId(eventId)).isEmpty();
    }

    @Test
    void appendRejectsValuesOutsideTheTransitionEventContract() {
        AppendJournalEvent valid = event(
            UUID.randomUUID(),
            UUID.randomUUID(),
            assignmentCreatedPayload()
        );

        assertThatThrownBy(() -> EventContract.requireValid(new AppendJournalEvent(
            valid.eventId(),
            "transition_checkpoint",
            valid.shapeRef(),
            valid.activityRef(),
            valid.subjectType(),
            valid.subjectId(),
            valid.actorId(),
            valid.recordedAt(),
            valid.payload()
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("structural event type");

        assertThatThrownBy(() -> EventContract.requireValid(new AppendJournalEvent(
            valid.eventId(),
            valid.eventType(),
            "assignment-created/v1",
            valid.activityRef(),
            valid.subjectType(),
            valid.subjectId(),
            valid.actorId(),
            valid.recordedAt(),
            valid.payload()
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("shape reference");

        assertThatThrownBy(() -> EventContract.requireValid(new AppendJournalEvent(
            valid.eventId(),
            valid.eventType(),
            valid.shapeRef(),
            valid.activityRef(),
            "org_unit",
            valid.subjectId(),
            valid.actorId(),
            valid.recordedAt(),
            valid.payload()
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("subject type");

        assertThatThrownBy(() -> EventContract.requireValid(new AppendJournalEvent(
            valid.eventId(),
            valid.eventType(),
            valid.shapeRef(),
            valid.activityRef(),
            valid.subjectType(),
            valid.subjectId(),
            valid.actorId(),
            valid.recordedAt(),
            objectMapper.createArrayNode()
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("required value");

        assertThatThrownBy(() -> EventContract.requireValid(new AppendJournalEvent(
            valid.eventId(),
            "capture",
            valid.shapeRef(),
            valid.activityRef(),
            "subject",
            valid.subjectId(),
            valid.actorId(),
            valid.recordedAt(),
            valid.payload()
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("requires event type assignment_changed");
    }

    @Test
    void databaseRejectsDirectWritesOutsideTheEventContract() {
        assertDirectWriteRejected(
            "transition_checkpoint",
            "assignment_created/v1",
            "assignment",
            "{}"
        );
        assertDirectWriteRejected(
            "assignment_changed",
            "assignment-created/v1",
            "assignment",
            "{}"
        );
        assertDirectWriteRejected(
            "assignment_changed",
            "assignment_created/v1",
            "org_unit",
            "{}"
        );
        assertDirectWriteRejected(
            "assignment_changed",
            "assignment_created/v1",
            "assignment",
            "[]"
        );
        assertDirectWriteRejected(
            "capture",
            "assignment_created/v1",
            "subject",
            "{}"
        );
    }

    private AppendJournalEvent event(UUID eventId, UUID subjectId, JsonNode payload) {
        return new AppendJournalEvent(
            eventId,
            "assignment_changed",
            "assignment_created/v1",
            "Act00000001",
            "assignment",
            subjectId,
            UUID.randomUUID().toString(),
            RECORDED_AT,
            payload
        );
    }

    private JsonNode assignmentCreatedPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putObject("target_actor")
            .put("type", "actor")
            .put("id", UUID.randomUUID().toString());
        payload.put("role", "test-role");
        ObjectNode scope = payload.putObject("scope");
        scope.put("geographic", UUID.randomUUID().toString());
        scope.putNull("subject_list");
        scope.putArray("activity").add("Act00000001");
        payload.put("valid_from", RECORDED_AT.toString());
        payload.putNull("valid_to");
        return payload;
    }

    private void assertDirectWriteRejected(
        String eventType,
        String shapeRef,
        String subjectType,
        String payload
    ) {
        assertThatThrownBy(() -> jdbc.update(
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
                ) VALUES (?, ?, ?, NULL, ?, ?, 'system:test', now(),
                          CAST(? AS jsonb))
                """,
            UUID.randomUUID(),
            eventType,
            shapeRef,
            subjectType,
            UUID.randomUUID(),
            payload
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private record JournalStorageTypes(String eventIdType, String subjectIdType, String payloadType) {
    }
}
