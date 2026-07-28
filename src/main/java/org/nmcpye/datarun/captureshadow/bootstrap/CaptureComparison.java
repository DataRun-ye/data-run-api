package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
final class CaptureComparison {

    static final int MAX_DIAGNOSTIC_SAMPLES = 10;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final ObjectMapper objectMapper;
    private final CaptureSourceReader sourceReader;
    private final CaptureCanonicalizer canonicalizer;

    CaptureComparison(
        JdbcTemplate jdbc,
        NamedParameterJdbcTemplate namedJdbc,
        ObjectMapper objectMapper,
        CaptureSourceReader sourceReader,
        CaptureCanonicalizer canonicalizer
    ) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
        this.objectMapper = objectMapper;
        this.sourceReader = sourceReader;
        this.canonicalizer = canonicalizer;
    }

    CaptureComparisonResult compare(CaptureSourceBoundary boundary) {
        MutableComparison comparison = new MutableComparison();
        long lastSerial = CaptureShadowBootstrap.INITIAL_SERIAL_CURSOR;
        long comparedRows = 0;
        while (true) {
            List<CaptureSourceRow> rows = sourceReader.readPage(
                lastSerial,
                boundary.sourceMaxSerial(),
                CaptureShadowBootstrap.BATCH_SIZE
            );
            if (rows.isEmpty()) {
                break;
            }
            comparePage(rows, comparison);
            comparedRows += rows.size();
            lastSerial = rows.get(rows.size() - 1).serialNumber();
        }
        if (comparedRows != boundary.sourceCount()) {
            comparison.identityDifferenceCount++;
            comparison.sample(
                "comparison source count expected=" + boundary.sourceCount()
                    + " actual=" + comparedRows
            );
        }

        long identityTotal = count("SELECT count(*) FROM capture_identity_link");
        comparison.extraIdentityCount = Math.max(0, identityTotal - boundary.sourceCount());
        if (comparison.extraIdentityCount > 0) {
            comparison.sample("extra capture identities=" + comparison.extraIdentityCount);
        }

        long eventTotal = count(
            """
                SELECT count(*)
                FROM event_journal
                WHERE event_type = ? AND shape_ref = ?
                """,
            CaptureShadowProtocol.CAPTURE_EVENT_TYPE,
            CaptureShadowProtocol.CAPTURE_SHAPE_REF
        );
        comparison.extraEventCount = Math.max(0, eventTotal - boundary.sourceCount());
        if (comparison.extraEventCount > 0) {
            comparison.sample("extra bootstrap capture events=" + comparison.extraEventCount);
        }
        return comparison.result();
    }

    private void comparePage(
        List<CaptureSourceRow> rows,
        MutableComparison comparison
    ) {
        List<CanonicalCapture> expected = rows.stream()
            .map(canonicalizer::canonicalize)
            .toList();
        List<String> submissionUids = rows.stream().map(CaptureSourceRow::uid).toList();
        List<UUID> captureIds = expected.stream().map(CanonicalCapture::captureId).toList();
        List<String> orgUnitUids = rows.stream().map(CaptureSourceRow::orgUnitUid).distinct().toList();
        List<UUID> orgUnitIds = expected.stream().map(CanonicalCapture::orgUnitId).distinct().toList();
        List<UUID> eventIds = expected.stream().map(CanonicalCapture::eventId).toList();

        List<ActualIdentity> identities = identities(submissionUids, captureIds);
        List<ActualAlias> aliases = aliases(orgUnitUids, orgUnitIds);
        List<ActualEvent> events = events(eventIds);

        for (CanonicalCapture capture : expected) {
            compareIdentity(capture, identities, comparison);
            compareAlias(capture, aliases, comparison);
            compareEvent(capture, events, comparison);
        }
    }

    private void compareIdentity(
        CanonicalCapture expected,
        List<ActualIdentity> actual,
        MutableComparison comparison
    ) {
        List<ActualIdentity> candidates = actual.stream()
            .filter(value -> value.captureId().equals(expected.captureId())
                || value.submissionUid().equals(expected.source().uid()))
            .toList();
        if (candidates.isEmpty()) {
            comparison.missingIdentityCount++;
            comparison.sample("missing capture identity uid=" + expected.source().uid());
            return;
        }
        boolean exact = candidates.stream().anyMatch(value ->
            value.captureId().equals(expected.captureId())
                && value.submissionUid().equals(expected.source().uid())
                && value.submissionId().equals(expected.source().submissionId())
                && value.serialNumber() == expected.source().serialNumber()
        );
        if (!exact || candidates.size() != 1) {
            comparison.identityDifferenceCount++;
            comparison.sample("capture identity mismatch uid=" + expected.source().uid());
        }
    }

    private void compareAlias(
        CanonicalCapture expected,
        List<ActualAlias> actual,
        MutableComparison comparison
    ) {
        List<ActualAlias> candidates = actual.stream()
            .filter(value -> value.orgUnitId().equals(expected.orgUnitId())
                || value.orgUnitUid().equals(expected.source().orgUnitUid()))
            .toList();
        if (candidates.isEmpty()) {
            comparison.missingOrgUnitAliasCount++;
            comparison.sample("missing org-unit alias uid=" + expected.source().orgUnitUid());
            return;
        }
        boolean exact = candidates.stream().anyMatch(value ->
            value.orgUnitId().equals(expected.orgUnitId())
                && value.orgUnitUid().equals(expected.source().orgUnitUid())
        );
        if (!exact || candidates.size() != 1) {
            comparison.orgUnitAliasDifferenceCount++;
            comparison.sample("org-unit alias mismatch uid=" + expected.source().orgUnitUid());
        }
    }

    private void compareEvent(
        CanonicalCapture expected,
        List<ActualEvent> actual,
        MutableComparison comparison
    ) {
        ActualEvent event = actual.stream()
            .filter(value -> value.eventId().equals(expected.eventId()))
            .findFirst()
            .orElse(null);
        if (event == null) {
            comparison.missingEventCount++;
            comparison.sample("missing capture event uid=" + expected.source().uid());
            return;
        }
        boolean exact = event.eventType().equals(CaptureShadowProtocol.CAPTURE_EVENT_TYPE)
            && event.shapeRef().equals(CaptureShadowProtocol.CAPTURE_SHAPE_REF)
            && Objects.equals(event.activityRef(), expected.source().activityUid())
            && event.subjectType().equals(CaptureShadowProtocol.CAPTURE_SUBJECT_TYPE)
            && event.subjectId().equals(expected.orgUnitId())
            && event.actorId().equals(CaptureShadowProtocol.SYSTEM_ACTOR)
            && event.recordedAt().equals(expected.recordedAt())
            && event.payload().equals(expected.payload());
        if (!exact) {
            comparison.eventDifferenceCount++;
            comparison.sample("capture event mismatch uid=" + expected.source().uid());
        }
    }

    private List<ActualIdentity> identities(List<String> uids, List<UUID> ids) {
        return namedJdbc.query(
            """
                SELECT capture_id, baseline_submission_uid,
                       baseline_submission_id, baseline_serial_number
                FROM capture_identity_link
                WHERE baseline_submission_uid IN (:uids) OR capture_id IN (:ids)
                """,
            new MapSqlParameterSource().addValue("uids", uids).addValue("ids", ids),
            (resultSet, rowNumber) -> new ActualIdentity(
                resultSet.getObject("capture_id", UUID.class),
                resultSet.getString("baseline_submission_uid"),
                resultSet.getString("baseline_submission_id"),
                resultSet.getLong("baseline_serial_number")
            )
        );
    }

    private List<ActualAlias> aliases(List<String> uids, List<UUID> ids) {
        return namedJdbc.query(
            """
                SELECT org_unit_id, baseline_org_unit_uid
                FROM org_unit_identity_link
                WHERE baseline_org_unit_uid IN (:uids) OR org_unit_id IN (:ids)
                """,
            new MapSqlParameterSource().addValue("uids", uids).addValue("ids", ids),
            (resultSet, rowNumber) -> new ActualAlias(
                resultSet.getObject("org_unit_id", UUID.class),
                resultSet.getString("baseline_org_unit_uid")
            )
        );
    }

    private List<ActualEvent> events(List<UUID> ids) {
        return namedJdbc.query(
            """
                SELECT event_id, event_type, shape_ref, activity_ref,
                       subject_type, subject_id, actor_id, recorded_at,
                       payload::text AS payload
                FROM event_journal
                WHERE event_id IN (:ids)
                """,
            new MapSqlParameterSource("ids", ids),
            (resultSet, rowNumber) -> new ActualEvent(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getString("shape_ref"),
                resultSet.getString("activity_ref"),
                resultSet.getString("subject_type"),
                resultSet.getObject("subject_id", UUID.class),
                resultSet.getString("actor_id"),
                resultSet.getTimestamp("recorded_at").toInstant(),
                readJson(resultSet.getString("payload"))
            )
        );
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new CaptureShadowBootstrapConflictException(
                "Stored capture event payload is invalid JSON",
                exception
            );
        }
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return Objects.requireNonNull(value, "Count query returned null");
    }

    private record ActualIdentity(
        UUID captureId,
        String submissionUid,
        String submissionId,
        long serialNumber
    ) {
    }

    private record ActualAlias(UUID orgUnitId, String orgUnitUid) {
    }

    private record ActualEvent(
        UUID eventId,
        String eventType,
        String shapeRef,
        String activityRef,
        String subjectType,
        UUID subjectId,
        String actorId,
        Instant recordedAt,
        JsonNode payload
    ) {
    }

    private static final class MutableComparison {
        private long missingIdentityCount;
        private long identityDifferenceCount;
        private long extraIdentityCount;
        private long missingOrgUnitAliasCount;
        private long orgUnitAliasDifferenceCount;
        private long missingEventCount;
        private long eventDifferenceCount;
        private long extraEventCount;
        private final List<String> diagnosticSamples = new ArrayList<>();

        private void sample(String value) {
            if (diagnosticSamples.size() < MAX_DIAGNOSTIC_SAMPLES) {
                diagnosticSamples.add(value);
            }
        }

        private CaptureComparisonResult result() {
            return new CaptureComparisonResult(
                missingIdentityCount,
                identityDifferenceCount,
                extraIdentityCount,
                missingOrgUnitAliasCount,
                orgUnitAliasDifferenceCount,
                missingEventCount,
                eventDifferenceCount,
                extraEventCount,
                diagnosticSamples
            );
        }
    }
}
