package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.EventDto;
import org.nmcpye.datarun.utils.UuidUtils;
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
            + "INSERT INTO analytics.events ("
            + "event_id, event_type, submission_uid, submission_id, submission_serial, "
            + "parent_event_id, event_ce_id, assignment_uid, activity_uid, org_unit_uid, team_uid, template_uid, "
            + "submission_creation_time, start_time, last_seen, created_by, last_modified_by, "
            + "anchor_ce_id, anchor_ref_uid, anchor_value_text, anchor_value_ref_type, anchor_confidence, "
            + "anchor_resolved_at, created_at, updated_at, deleted_at) "
            + "VALUES ("
            + ":eventId, :eventType, :submissionUid, :submissionId, :submissionSerial, "
            + ":parentEventId, :eventCeId, :assignmentUid, :activityUid, :orgUnitUid, :teamUid, :templateUid, "
            + ":submissionCreationTime, :startTime, :lastSeen,  :createdBy, :lastModifiedBy, "
            + ":anchorCeId, :anchorRefUid, :anchorValueText, :anchorValueRefType, :anchorConfidence," +
            " :anchorResolvedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :deletedAt) "
            + "ON CONFLICT (event_id) DO UPDATE SET "
            + "event_id = events.event_id, "
            + "deleted_at = EXCLUDED.deleted_at, "
            + "event_type = COALESCE(EXCLUDED.event_type, events.event_type), "
            + "submission_uid = COALESCE(EXCLUDED.submission_uid, events.submission_uid), "
            + "submission_id = COALESCE(EXCLUDED.submission_id, events.submission_id), "
            + "submission_serial = COALESCE(EXCLUDED.submission_serial, events.submission_serial), "
            + "parent_event_id = COALESCE(EXCLUDED.parent_event_id, events.parent_event_id), "
            + "event_ce_id = COALESCE(EXCLUDED.event_ce_id, events.event_ce_id), "
            + "assignment_uid = COALESCE(EXCLUDED.assignment_uid, events.assignment_uid), "
            + "activity_uid = COALESCE(EXCLUDED.activity_uid, events.activity_uid), "
            + "org_unit_uid = COALESCE(EXCLUDED.org_unit_uid, events.org_unit_uid), "
            + "team_uid = COALESCE(EXCLUDED.team_uid, events.team_uid), "
            + "template_uid = COALESCE(EXCLUDED.template_uid, events.template_uid), "
            + "submission_creation_time = COALESCE(events.submission_creation_time, EXCLUDED.submission_creation_time), "
            + "start_time = COALESCE(events.start_time, EXCLUDED.start_time), "
            + "created_by = COALESCE(events.created_by, EXCLUDED.created_by), "
            + "last_modified_by = COALESCE(events.last_modified_by, EXCLUDED.last_modified_by), "
            + "last_seen = GREATEST(COALESCE(events.last_seen, TIMESTAMP '1970-01-01'), COALESCE(EXCLUDED.last_seen, TIMESTAMP '1970-01-01')), "
            // anchor update: replace
            + "anchor_ref_uid = EXCLUDED.anchor_ref_uid, "
            + "anchor_ce_id = EXCLUDED.anchor_ce_id, "
            + "anchor_value_text = EXCLUDED.anchor_value_text, "
            + "anchor_value_ref_type = EXCLUDED.anchor_value_ref_type, "
            + "anchor_confidence = EXCLUDED.anchor_confidence, "
            + "anchor_resolved_at = EXCLUDED.anchor_resolved_at, "
            + "updated_at = CURRENT_TIMESTAMP";

        var ceId = UuidUtils.toUuidOrNull(eventDto.anchorCeId());
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("eventId", eventDto.eventId())
            .addValue("eventType", eventDto.eventType())
            .addValue("submissionUid", eventDto.submissionUid())
            .addValue("submissionId", eventDto.submissionId())
            .addValue("assignmentUid", eventDto.assignmentUid())
            .addValue("submissionSerial", eventDto.submissionSerial())
            .addValue("parentEventId", eventDto.parentEventId())
            .addValue("eventCeId", eventDto.eventCeId())
            .addValue("activityUid", eventDto.activityUid())
            .addValue("orgUnitUid", eventDto.orgUnitUid())
            .addValue("teamUid", eventDto.teamUid())
            .addValue("templateUid", eventDto.templateUid())
            .addValue("submissionCreationTime", getTimestamp(eventDto.submissionCreationTime()))
            .addValue("startTime", getTimestamp(eventDto.startTime()))
            .addValue("lastSeen", getTimestamp(eventDto.lastSeen()))
            .addValue("createdBy", eventDto.createdBy())
            .addValue("lastModifiedBy", eventDto.lastModifiedBy())
            .addValue("anchorCeId", ceId)
            .addValue("anchorRefUid", eventDto.anchorRefUid())
            .addValue("anchorValueText", eventDto.anchorValueText())
            .addValue("anchorValueRefType", eventDto.anchorValueRefType())
            .addValue("anchorConfidence", eventDto.anchorConfidence())
            .addValue("anchorResolvedAt", getTimestamp(eventDto.anchorResolvedAt()))
            .addValue("deletedAt", getTimestamp(eventDto.deletedAt()));

        jdbc.update(sql, p);
    }

    public Optional<EventDto> findByInstanceKey(String eventId) {
        if (eventId == null) return Optional.empty();
        String sql = "SELECT * FROM analytics.events WHERE event_id = :eventId LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("eventId", eventId);
        List<EventDto> rows = jdbc.query(sql, p, EventDto.ROW_MAPPER);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }

    public void markAllAsDeletedForSubmission(String submissionUid) {
        String sql = "UPDATE analytics.events SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
            "WHERE submission_uid = :submissionUid AND deleted_at IS NULL";
        jdbc.update(sql, new MapSqlParameterSource("submissionUid", submissionUid));
    }

    Timestamp getTimestamp(Instant instant) {
        if (instant != null) return Timestamp.from(instant);
        return Timestamp.from(Instant.now());
    }

    public void patchUpsertEvents(List<EventDto> events) {
        if (events == null || events.isEmpty()) return;

        final String sql = ""
            + "INSERT INTO analytics.events ("
            + "event_id, event_type, submission_uid, submission_id, submission_serial, "
            + "parent_event_id, event_ce_id, assignment_uid, activity_uid, org_unit_uid, team_uid, template_uid, "
            + "submission_creation_time, start_time, last_seen, created_by, last_modified_by, "
            + "anchor_ce_id, anchor_ref_uid, anchor_value_text, anchor_value_ref_type, anchor_confidence, "
            + "anchor_resolved_at, created_at, updated_at, deleted_at) "
            + "VALUES ("
            + ":eventId, :eventType, :submissionUid, :submissionId, :submissionSerial, "
            + ":parentEventId, :eventCeId, :assignmentUid, :activityUid, :orgUnitUid, :teamUid, :templateUid, "
            + ":submissionCreationTime, :startTime, :lastSeen, :createdBy, :lastModifiedBy, "
            + ":anchorCeId, :anchorRefUid, :anchorValueText, :anchorValueRefType, :anchorConfidence, "
            + ":anchorResolvedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :deletedAt) "
            + "ON CONFLICT (event_id) DO UPDATE SET "
            + "deleted_at = EXCLUDED.deleted_at, "
            + "event_type = COALESCE(EXCLUDED.event_type, events.event_type), "
            + "submission_uid = COALESCE(EXCLUDED.submission_uid, events.submission_uid), "
            + "submission_id = COALESCE(EXCLUDED.submission_id, events.submission_id), "
            + "submission_serial = COALESCE(EXCLUDED.submission_serial, events.submission_serial), "
            + "parent_event_id = COALESCE(EXCLUDED.parent_event_id, events.parent_event_id), "
            + "event_ce_id = COALESCE(EXCLUDED.event_ce_id, events.event_ce_id), "
            + "assignment_uid = COALESCE(EXCLUDED.assignment_uid, events.assignment_uid), "
            + "activity_uid = COALESCE(EXCLUDED.activity_uid, events.activity_uid), "
            + "org_unit_uid = COALESCE(EXCLUDED.org_unit_uid, events.org_unit_uid), "
            + "team_uid = COALESCE(EXCLUDED.team_uid, events.team_uid), "
            + "template_uid = COALESCE(EXCLUDED.template_uid, events.template_uid), "
            + "created_by = COALESCE(events.created_by, EXCLUDED.created_by), "
            + "last_modified_by = COALESCE(events.last_modified_by, EXCLUDED.last_modified_by), "
            + "submission_creation_time = COALESCE(events.submission_creation_time, EXCLUDED.submission_creation_time), "
            + "start_time = COALESCE(events.start_time, EXCLUDED.start_time), "
            + "last_seen = GREATEST(COALESCE(events.last_seen, TIMESTAMP '1970-01-01'), COALESCE(EXCLUDED.last_seen, TIMESTAMP '1970-01-01')), "
            // anchor update: only replace anchor fields when the incoming anchor is "better" or existing is null
//            + "anchor_confidence = CASE "
//            + "  WHEN EXCLUDED.anchor_confidence IS NULL AND events.anchor_confidence IS NOT NULL THEN events.anchor_confidence "
//            + "  WHEN events.anchor_confidence IS NULL THEN EXCLUDED.anchor_confidence "
//            + "  WHEN EXCLUDED.anchor_confidence >= events.anchor_confidence THEN EXCLUDED.anchor_confidence "
//            + "  ELSE events.anchor_confidence END, "
//            + "anchor_resolved_at = CASE "
//            + "  WHEN EXCLUDED.anchor_resolved_at IS NULL AND events.anchor_resolved_at IS NOT NULL THEN events.anchor_resolved_at "
//            + "  WHEN events.anchor_resolved_at IS NULL THEN EXCLUDED.anchor_resolved_at "
//            + "  WHEN EXCLUDED.anchor_resolved_at >= events.anchor_resolved_at THEN EXCLUDED.anchor_resolved_at "
//            + "  ELSE events.anchor_resolved_at END, "
//            + "anchor_ref_uid = CASE "
//            + "  WHEN EXCLUDED.anchor_confidence IS NULL AND events.anchor_confidence IS NOT NULL THEN events.anchor_ref_uid "
//            + "  WHEN events.anchor_confidence IS NULL THEN EXCLUDED.anchor_ref_uid "
//            + "  WHEN EXCLUDED.anchor_confidence >= events.anchor_confidence THEN EXCLUDED.anchor_ref_uid "
//            + "  ELSE events.anchor_ref_uid END, "
//            + "anchor_ce_id = CASE "
//            + "  WHEN EXCLUDED.anchor_confidence IS NULL AND events.anchor_confidence IS NOT NULL THEN events.anchor_ce_id "
//            + "  WHEN events.anchor_confidence IS NULL THEN EXCLUDED.anchor_ce_id "
//            + "  WHEN EXCLUDED.anchor_confidence >= events.anchor_confidence THEN EXCLUDED.anchor_ce_id "
//            + "  ELSE events.anchor_ce_id END, "
//            + "anchor_value_text = CASE "
//            + "  WHEN EXCLUDED.anchor_confidence IS NULL AND events.anchor_confidence IS NOT NULL THEN events.anchor_value_text "
//            + "  WHEN events.anchor_confidence IS NULL THEN EXCLUDED.anchor_value_text "
//            + "  WHEN EXCLUDED.anchor_confidence >= events.anchor_confidence THEN EXCLUDED.anchor_value_text "
//            + "  ELSE events.anchor_value_text END, "
//            + "anchor_value_ref_type = CASE "
//            + "  WHEN EXCLUDED.anchor_confidence IS NULL AND events.anchor_confidence IS NOT NULL THEN events.anchor_value_ref_type "
//            + "  WHEN events.anchor_confidence IS NULL THEN EXCLUDED.anchor_value_ref_type "
//            + "  WHEN EXCLUDED.anchor_confidence >= events.anchor_confidence THEN EXCLUDED.anchor_value_ref_type "
//            + "  ELSE events.anchor_value_ref_type END, "
            + "updated_at = CURRENT_TIMESTAMP";

        // prepare batch params
        MapSqlParameterSource[] batch = new MapSqlParameterSource[events.size()];
        int i = 0;
        for (EventDto e : events) {
            MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("eventId", e.eventId())
                .addValue("eventType", e.eventType())
                .addValue("submissionUid", e.submissionUid())
                .addValue("submissionId", e.submissionId())
                .addValue("assignmentUid", e.assignmentUid())
                .addValue("submissionSerial", e.submissionSerial())
                .addValue("parentEventId", e.parentEventId())
                .addValue("eventCeId", UuidUtils.toUuidOrNull(e.eventCeId()))
                .addValue("activityUid", e.activityUid())
                .addValue("orgUnitUid", e.orgUnitUid())
                .addValue("teamUid", e.teamUid())
                .addValue("templateUid", e.templateUid())
                .addValue("createdBy", e.createdBy())
                .addValue("lastModifiedBy", e.lastModifiedBy())
                .addValue("submissionCreationTime", getTimestamp(e.submissionCreationTime()))
                .addValue("startTime", getTimestamp(e.startTime()))
                .addValue("lastSeen", getTimestamp(e.lastSeen()))
                .addValue("anchorCeId", UuidUtils.toUuidOrNull(e.anchorCeId()))
                .addValue("anchorRefUid", e.anchorRefUid())
                .addValue("anchorValueText", e.anchorValueText())
                .addValue("anchorValueRefType", e.anchorValueRefType())
                .addValue("anchorConfidence", e.anchorConfidence())
                .addValue("anchorResolvedAt", getTimestamp(e.anchorResolvedAt()))
                .addValue("deletedAt", getTimestamp(e.deletedAt()));
            batch[i++] = p;
        }

        jdbc.batchUpdate(sql, batch);
    }
}
