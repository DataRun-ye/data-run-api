package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Component
public class CaptureBootstrapBatchTransaction {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final CaptureSourceReader sourceReader;
    private final CaptureCanonicalizer canonicalizer;

    CaptureBootstrapBatchTransaction(
        JdbcTemplate jdbc,
        ObjectMapper objectMapper,
        CaptureSourceReader sourceReader,
        CaptureCanonicalizer canonicalizer
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.sourceReader = sourceReader;
        this.canonicalizer = canonicalizer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CaptureBootstrapBatchResult persistNextBatch(
        CaptureSourceBoundary boundary,
        long afterSerial
    ) {
        List<CaptureSourceRow> rows = sourceReader.readPage(
            afterSerial,
            boundary.sourceMaxSerial(),
            CaptureShadowBootstrap.BATCH_SIZE
        );
        if (rows.isEmpty()) {
            return CaptureBootstrapBatchResult.empty(afterSerial);
        }

        MutableCounts counts = new MutableCounts();
        for (CaptureSourceRow row : rows) {
            CanonicalCapture capture = canonicalizer.canonicalize(row);
            persistOrgUnitAlias(capture, counts);
            persistIdentity(capture, counts);
            persistEvent(capture, counts);
        }
        return counts.result(rows.get(rows.size() - 1).serialNumber(), rows.size());
    }

    private void persistOrgUnitAlias(CanonicalCapture capture, MutableCounts counts) {
        long currentOrgUnits = count(
            "SELECT count(*) FROM org_unit WHERE uid = ?",
            capture.source().orgUnitUid()
        );
        if (currentOrgUnits != 1) {
            throw new CaptureShadowBootstrapConflictException(
                "Submission " + capture.source().uid()
                    + " references unresolved organization unit "
                    + capture.source().orgUnitUid()
            );
        }

        boolean existed = count(
            """
                SELECT count(*)
                FROM org_unit_identity_link
                WHERE org_unit_id = ?
                  AND baseline_org_unit_uid = ?
                """,
            capture.orgUnitId(),
            capture.source().orgUnitUid()
        ) == 1;
        if (!existed) {
            jdbc.update(
                """
                    INSERT INTO org_unit_identity_link (org_unit_id, baseline_org_unit_uid)
                    VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """,
                capture.orgUnitId(),
                capture.source().orgUnitUid()
            );
        }
        requireExact(
            "organization-unit alias",
            capture.source().uid(),
            """
                SELECT count(*)
                FROM org_unit_identity_link
                WHERE org_unit_id = ?
                  AND baseline_org_unit_uid = ?
                """,
            capture.orgUnitId(),
            capture.source().orgUnitUid()
        );
        if (existed) counts.orgUnitAliasesExisting++; else counts.orgUnitAliasesCreated++;
    }

    private void persistIdentity(CanonicalCapture capture, MutableCounts counts) {
        boolean existed = count(
            exactIdentitySql(),
            capture.captureId(),
            capture.source().uid(),
            capture.source().submissionId(),
            capture.source().serialNumber()
        ) == 1;
        if (!existed) {
            jdbc.update(
                """
                    INSERT INTO capture_identity_link (
                        capture_id,
                        baseline_submission_uid,
                        baseline_submission_id,
                        baseline_serial_number
                    ) VALUES (?, ?, ?, ?)
                    ON CONFLICT DO NOTHING
                    """,
                capture.captureId(),
                capture.source().uid(),
                capture.source().submissionId(),
                capture.source().serialNumber()
            );
        }
        requireExact(
            "capture identity",
            capture.source().uid(),
            exactIdentitySql(),
            capture.captureId(),
            capture.source().uid(),
            capture.source().submissionId(),
            capture.source().serialNumber()
        );
        if (existed) counts.identitiesExisting++; else counts.identitiesCreated++;
    }

    private void persistEvent(CanonicalCapture capture, MutableCounts counts) {
        String payload = writeJson(capture);
        Object[] exactArguments = eventArguments(capture, payload);
        boolean existed = count(exactEventSql(), exactArguments) == 1;
        if (!existed) {
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
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                    ON CONFLICT (event_id) DO NOTHING
                    """,
                capture.eventId(),
                CaptureShadowProtocol.CAPTURE_EVENT_TYPE,
                CaptureShadowProtocol.CAPTURE_SHAPE_REF,
                capture.source().activityUid(),
                CaptureShadowProtocol.CAPTURE_SUBJECT_TYPE,
                capture.orgUnitId(),
                CaptureShadowProtocol.SYSTEM_ACTOR,
                Timestamp.from(capture.recordedAt()),
                payload
            );
        }
        requireExact(
            "capture event",
            capture.source().uid(),
            exactEventSql(),
            exactArguments
        );
        if (existed) counts.eventsExisting++; else counts.eventsCreated++;
    }

    private String exactIdentitySql() {
        return """
            SELECT count(*)
            FROM capture_identity_link
            WHERE capture_id = ?
              AND baseline_submission_uid = ?
              AND baseline_submission_id = ?
              AND baseline_serial_number = ?
            """;
    }

    private String exactEventSql() {
        return """
            SELECT count(*)
            FROM event_journal
            WHERE event_id = ?
              AND event_type = ?
              AND shape_ref = ?
              AND activity_ref IS NOT DISTINCT FROM ?
              AND subject_type = ?
              AND subject_id = ?
              AND actor_id = ?
              AND recorded_at = ?
              AND payload = CAST(? AS jsonb)
            """;
    }

    private Object[] eventArguments(CanonicalCapture capture, String payload) {
        return new Object[]{
            capture.eventId(),
            CaptureShadowProtocol.CAPTURE_EVENT_TYPE,
            CaptureShadowProtocol.CAPTURE_SHAPE_REF,
            capture.source().activityUid(),
            CaptureShadowProtocol.CAPTURE_SUBJECT_TYPE,
            capture.orgUnitId(),
            CaptureShadowProtocol.SYSTEM_ACTOR,
            Timestamp.from(capture.recordedAt()),
            payload
        };
    }

    private void requireExact(String item, String uid, String sql, Object... arguments) {
        if (count(sql, arguments) != 1) {
            throw new CaptureShadowBootstrapConflictException(
                "Conflicting immutable " + item + " for submission " + uid
            );
        }
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return Objects.requireNonNull(value, "Count query returned null");
    }

    private String writeJson(CanonicalCapture capture) {
        try {
            return objectMapper.writeValueAsString(capture.payload());
        } catch (JsonProcessingException exception) {
            throw new CaptureShadowBootstrapConflictException(
                "Canonical event payload could not be serialized for "
                    + capture.source().uid(),
                exception
            );
        }
    }

    private static final class MutableCounts {
        private long orgUnitAliasesCreated;
        private long orgUnitAliasesExisting;
        private long identitiesCreated;
        private long identitiesExisting;
        private long eventsCreated;
        private long eventsExisting;

        private CaptureBootstrapBatchResult result(long nextSerial, int rows) {
            return new CaptureBootstrapBatchResult(
                nextSerial,
                rows,
                orgUnitAliasesCreated,
                orgUnitAliasesExisting,
                identitiesCreated,
                identitiesExisting,
                eventsCreated,
                eventsExisting
            );
        }
    }
}
