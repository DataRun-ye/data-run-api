package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.EventDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("ConcatenationWithEmptyString")
@Repository
@RequiredArgsConstructor
public class EventEntityJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public void upsertEventEntity(EventDto eventDto) {

        //
        String sql = ""
            + "INSERT INTO analytics.events (event_id, instance_key, event_type, submission_uid, submission_id, assignment_uid, activity_uid, org_unit_uid, team_uid, template_uid, submission_creation_time, start_time, last_seen, anchor_ce_id, anchor_ref_uid, anchor_value_text, anchor_confidence, anchor_resolved_at, created_at, updated_at) "
            + "VALUES (:eventUid,:instanceKey,:eventType,:submissionUid,:submissionId,:assignmentUid,:activityUid,:orgUnitUid,:teamUid,:templateUid,:submissionCreationTime,:startTime,:lastSeen,:anchorCeId,:anchorRefUid,:anchorValueText,:anchorConfidence,:anchorResolvedAt,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) "
            + "ON CONFLICT (instance_key) DO UPDATE SET "
            + "event_id = events.event_id, "
            + "event_type = COALESCE(EXCLUDED.event_type, events.event_type), "
            + "submission_uid = COALESCE(EXCLUDED.submission_uid, events.submission_uid), "
            + "submission_id = COALESCE(EXCLUDED.submission_id, events.submission_id), "
            + "assignment_uid = COALESCE(EXCLUDED.assignment_uid, events.assignment_uid), "
            + "activity_uid = COALESCE(EXCLUDED.activity_uid, events.activity_uid), "
            + "org_unit_uid = COALESCE(EXCLUDED.org_unit_uid, events.org_unit_uid), "
            + "team_uid = COALESCE(EXCLUDED.team_uid, events.team_uid), "
            + "template_uid = COALESCE(EXCLUDED.template_uid, events.template_uid), "
            + "submission_creation_time = COALESCE(EXCLUDED.submission_creation_time, events.submission_creation_time), "
            + "start_time = COALESCE(EXCLUDED.start_time, events.start_time), "
            + "last_seen = GREATEST(COALESCE(events.last_seen, TIMESTAMP '1970-01-01'), COALESCE(EXCLUDED.last_seen, TIMESTAMP '1970-01-01')), "
            // anchor update: only replace when new anchor is non-null and better by confidence OR newer resolved_at
            + "anchor_ref_uid = CASE WHEN EXCLUDED.anchor_ref_uid IS NOT NULL AND (events.anchor_ref_uid IS NULL OR COALESCE(EXCLUDED.anchor_confidence,0) > COALESCE(events.anchor_confidence,0) OR (EXCLUDED.anchor_resolved_at IS NOT NULL AND (events.anchor_resolved_at IS NULL OR EXCLUDED.anchor_resolved_at > events.anchor_resolved_at))) THEN EXCLUDED.anchor_ref_uid ELSE events.anchor_ref_uid END, "
            + "anchor_ce_id = CASE WHEN EXCLUDED.anchor_ref_uid IS NOT NULL AND (events.anchor_ref_uid IS NULL OR COALESCE(EXCLUDED.anchor_confidence,0) > COALESCE(events.anchor_confidence,0) OR (EXCLUDED.anchor_resolved_at IS NOT NULL AND (events.anchor_resolved_at IS NULL OR EXCLUDED.anchor_resolved_at > events.anchor_resolved_at))) THEN EXCLUDED.anchor_ce_id ELSE events.anchor_ce_id END, "
            + "anchor_value_text = CASE WHEN EXCLUDED.anchor_ref_uid IS NOT NULL AND (events.anchor_ref_uid IS NULL OR COALESCE(EXCLUDED.anchor_confidence,0) > COALESCE(events.anchor_confidence,0) OR (EXCLUDED.anchor_resolved_at IS NOT NULL AND (events.anchor_resolved_at IS NULL OR EXCLUDED.anchor_resolved_at > events.anchor_resolved_at))) THEN EXCLUDED.anchor_value_text ELSE events.anchor_value_text END, "
            + "anchor_confidence = CASE WHEN EXCLUDED.anchor_ref_uid IS NOT NULL AND (events.anchor_ref_uid IS NULL OR COALESCE(EXCLUDED.anchor_confidence,0) > COALESCE(events.anchor_confidence,0) OR (EXCLUDED.anchor_resolved_at IS NOT NULL AND (events.anchor_resolved_at IS NULL OR EXCLUDED.anchor_resolved_at > events.anchor_resolved_at))) THEN EXCLUDED.anchor_confidence ELSE events.anchor_confidence END, "
            + "anchor_resolved_at = CASE WHEN EXCLUDED.anchor_ref_uid IS NOT NULL AND (events.anchor_ref_uid IS NULL OR COALESCE(EXCLUDED.anchor_confidence,0) > COALESCE(events.anchor_confidence,0) OR (EXCLUDED.anchor_resolved_at IS NOT NULL AND (events.anchor_resolved_at IS NULL OR EXCLUDED.anchor_resolved_at > events.anchor_resolved_at))) THEN EXCLUDED.anchor_resolved_at ELSE events.anchor_resolved_at END, "
            + "updated_at = CURRENT_TIMESTAMP";

        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("eventUid", eventDto.eventUid())
            .addValue("instanceKey", eventDto.instanceKey())
            .addValue("eventType", eventDto.eventType())
            .addValue("submissionUid", eventDto.submissionUid())
            .addValue("submissionId", eventDto.submissionId())
            .addValue("assignmentUid", eventDto.assignmentUid())
            .addValue("activityUid", eventDto.activityUid())
            .addValue("orgUnitUid", eventDto.orgUnitUid())
            .addValue("teamUid", eventDto.teamUid())
            .addValue("templateUid", eventDto.templateUid())
            .addValue("submissionCreationTime", getTimestamp(eventDto.submissionCreationTime()))
            .addValue("startTime", getTimestamp(eventDto.startTime()))
            .addValue("lastSeen", getTimestamp(eventDto.lastSeen()))
            .addValue("anchorCeId", eventDto.anchorCeId())
            .addValue("anchorRefUid", eventDto.anchorRefUid())
            .addValue("anchorValueText", eventDto.anchorValueText())
            .addValue("anchorConfidence", eventDto.anchorConfidence())
            .addValue("anchorResolvedAt", getTimestamp(eventDto.anchorResolvedAt()));

        jdbc.update(sql, p);
    }

    public Optional<EventDto> findByInstanceKey(String instanceKey) {
        if (instanceKey == null) return Optional.empty();
        String sql = "SELECT * FROM analytics.events WHERE instance_key = :instanceKey LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("instanceKey", instanceKey);
        List<EventDto> rows = jdbc.query(sql, p, EventDto.ROW_MAPPER);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }

    Timestamp getTimestamp(Instant instant) {
        if (instant != null) return Timestamp.from(instant);
        return Timestamp.from(Instant.now());
    }
}
