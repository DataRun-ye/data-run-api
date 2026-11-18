package org.nmcpye.datarun.jpa.intake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.nmcpye.datarun.jpa.intake.GoLiveRuleRepository.instant;
import static org.nmcpye.datarun.jpa.intake.GoLiveRuleRepository.utc;

/** Stores held uploads (table {@code held_submission}), one row per submission uid. */
@Repository
@RequiredArgsConstructor
// Writes are @Transactional: the pool does not auto-commit, so a write outside a
// transaction is rolled back when its connection returns to the pool.
public class HeldSubmissionRepository {

    /** The time go-live rules compare: the earlier of phone start and server receipt. */
    static final String ENTERED = "LEAST(COALESCE(h.started_at, h.received_at), h.received_at)";

    private static final String COLUMNS = """
        h.uid, h.reason, h.state, h.activity_uid, h.org_unit_uid, h.org_unit_path, h.form_uid,
        h.form_version_uid, h.team_uid, h.assignment_uid, h.submitted_by, h.started_at, h.received_at,
        h.go_live_rule_uid, h.live_from, h.decided_by, h.decided_at, h.note, h.last_release_error,
        h.last_modified_date""";

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    /**
     * Keeps a held upload, or refreshes it when the same submission is sent
     * again. A refresh keeps an admin's discard; a submission held again after
     * being released or superseded (its real copy was removed) waits again.
     */
    @Transactional
    public void hold(String uid, HoldReason reason, GoLiveRule rule, HeldContext context, JsonNode payload) {
        jdbc.update("""
                INSERT INTO held_submission AS h
                    (uid, reason, state, activity_uid, org_unit_uid, org_unit_path, form_uid, form_version_uid,
                     team_uid, assignment_uid, submitted_by, started_at, received_at, go_live_rule_uid, live_from,
                     payload, last_modified_date)
                VALUES (:uid, :reason, 'HELD', :activity, :orgUnit, :orgUnitPath, :form, :formVersion,
                        :team, :assignment, :submittedBy, :startedAt, :receivedAt, :rule, :liveFrom,
                        CAST(:payload AS jsonb), now())
                ON CONFLICT (uid) DO UPDATE SET
                    reason = EXCLUDED.reason,
                    activity_uid = EXCLUDED.activity_uid,
                    org_unit_uid = EXCLUDED.org_unit_uid,
                    org_unit_path = EXCLUDED.org_unit_path,
                    form_uid = EXCLUDED.form_uid,
                    form_version_uid = EXCLUDED.form_version_uid,
                    team_uid = EXCLUDED.team_uid,
                    assignment_uid = EXCLUDED.assignment_uid,
                    submitted_by = EXCLUDED.submitted_by,
                    started_at = EXCLUDED.started_at,
                    received_at = EXCLUDED.received_at,
                    go_live_rule_uid = EXCLUDED.go_live_rule_uid,
                    live_from = EXCLUDED.live_from,
                    payload = EXCLUDED.payload,
                    state = CASE WHEN h.state IN ('RELEASED', 'SUPERSEDED') THEN 'HELD' ELSE h.state END,
                    decided_by = CASE WHEN h.state IN ('RELEASED', 'SUPERSEDED') THEN NULL ELSE h.decided_by END,
                    decided_at = CASE WHEN h.state IN ('RELEASED', 'SUPERSEDED') THEN NULL ELSE h.decided_at END,
                    last_modified_date = now()
                """,
            new MapSqlParameterSource()
                .addValue("uid", uid)
                .addValue("reason", reason.name())
                .addValue("activity", context.activity())
                .addValue("orgUnit", context.orgUnit())
                .addValue("orgUnitPath", context.orgUnitPath())
                .addValue("form", context.form())
                .addValue("formVersion", context.formVersion())
                .addValue("team", context.team())
                .addValue("assignment", context.assignment())
                .addValue("submittedBy", context.submittedBy())
                .addValue("startedAt", utc(context.startedAt()))
                .addValue("receivedAt", utc(context.receivedAt()))
                .addValue("rule", rule == null ? null : rule.uid())
                .addValue("liveFrom", rule == null ? null : utc(rule.liveFrom()))
                .addValue("payload", json(payload)));
    }

    public Optional<HeldSubmissionRow> findByUid(String uid) {
        return jdbc.query("SELECT " + COLUMNS + ", h.payload FROM held_submission h WHERE h.uid = :uid",
                new MapSqlParameterSource("uid", uid), rows(true))
            .stream().findFirst();
    }

    /** Newest first, without payloads. */
    public List<HeldSubmissionRow> find(HeldFilter filter, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("limit", size)
            .addValue("offset", (long) page * size);
        return jdbc.query("SELECT " + COLUMNS + " FROM held_submission h" + where(filter, params)
                + " ORDER BY h.received_at DESC, h.uid LIMIT :limit OFFSET :offset",
            params, rows(false));
    }

    /** Every match, oldest first, without payloads. */
    public List<HeldSubmissionRow> findAll(HeldFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        return jdbc.query("SELECT " + COLUMNS + " FROM held_submission h" + where(filter, params)
                + " ORDER BY h.received_at, h.uid",
            params, rows(false));
    }

    /** With payloads, for a release. */
    public List<HeldSubmissionRow> findWithPayload(HeldFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        return jdbc.query("SELECT " + COLUMNS + ", h.payload FROM held_submission h" + where(filter, params)
                + " ORDER BY h.received_at, h.uid",
            params, rows(true));
    }

    /** With payloads, for a release, chosen by uid. */
    public List<HeldSubmissionRow> findWithPayload(Collection<String> uids, Collection<HeldState> states) {
        if (uids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("SELECT " + COLUMNS + ", h.payload FROM held_submission h"
                + " WHERE h.uid IN (:uids) AND h.state IN (:states) ORDER BY h.received_at, h.uid",
            new MapSqlParameterSource()
                .addValue("uids", uids)
                .addValue("states", states.stream().map(Enum::name).toList()),
            rows(true));
    }

    public List<HeldSummaryRow> summary(HeldFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        return jdbc.query("""
                SELECT h.org_unit_uid, ou.name AS org_unit_name, h.form_uid, h.reason, h.state,
                       count(*) AS held, min(%1$s) AS first_entered, max(%1$s) AS last_entered
                FROM held_submission h
                LEFT JOIN org_unit ou ON ou.uid = h.org_unit_uid
                """.formatted(ENTERED) + where(filter, params) + """

                GROUP BY h.org_unit_uid, ou.name, h.form_uid, h.reason, h.state
                ORDER BY ou.name, h.form_uid, h.reason, h.state
                """,
            params,
            (rs, i) -> new HeldSummaryRow(
                rs.getString("org_unit_uid"),
                rs.getString("org_unit_name"),
                rs.getString("form_uid"),
                HoldReason.valueOf(rs.getString("reason")),
                HeldState.valueOf(rs.getString("state")),
                rs.getLong("held"),
                instant(rs, "first_entered"),
                instant(rs, "last_entered")));
    }

    /** Records an admin's decision on one held upload. */
    @Transactional
    public void markDecided(String uid, HeldState state, String decidedBy, String note) {
        jdbc.update("""
                UPDATE held_submission SET state = :state, decided_by = :decidedBy, decided_at = now(),
                    note = COALESCE(:note, note), last_release_error = NULL, last_modified_date = now()
                WHERE uid = :uid
                """,
            new MapSqlParameterSource()
                .addValue("uid", uid)
                .addValue("state", state.name())
                .addValue("decidedBy", decidedBy)
                .addValue("note", note));
    }

    /** Records why a release failed; the upload stays where it was. */
    @Transactional
    public void recordReleaseError(String uid, String error) {
        jdbc.update("""
                UPDATE held_submission SET last_release_error = :error, last_modified_date = now()
                WHERE uid = :uid
                """,
            new MapSqlParameterSource().addValue("uid", uid).addValue("error", error));
    }

    private static String where(HeldFilter filter, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        if (filter.activity() != null) {
            conditions.add("h.activity_uid = :activity");
            params.addValue("activity", filter.activity());
        }
        if (filter.orgUnit() != null) {
            conditions.add("(h.org_unit_path || ',') LIKE :orgUnitInPath");
            params.addValue("orgUnitInPath", "%," + filter.orgUnit() + ",%");
        }
        if (filter.form() != null) {
            conditions.add("h.form_uid = :form");
            params.addValue("form", filter.form());
        }
        if (filter.reason() != null) {
            conditions.add("h.reason = :reason");
            params.addValue("reason", filter.reason().name());
        }
        if (filter.states() != null && !filter.states().isEmpty()) {
            conditions.add("h.state IN (:states)");
            params.addValue("states", filter.states().stream().map(Enum::name).toList());
        }
        if (filter.enteredFrom() != null) {
            conditions.add(ENTERED + " >= :enteredFrom");
            params.addValue("enteredFrom", utc(filter.enteredFrom()));
        }
        if (filter.enteredTo() != null) {
            conditions.add(ENTERED + " < :enteredTo");
            params.addValue("enteredTo", utc(filter.enteredTo()));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private RowMapper<HeldSubmissionRow> rows(boolean withPayload) {
        return (rs, i) -> new HeldSubmissionRow(
            rs.getString("uid"),
            HoldReason.valueOf(rs.getString("reason")),
            HeldState.valueOf(rs.getString("state")),
            rs.getString("activity_uid"),
            rs.getString("org_unit_uid"),
            rs.getString("org_unit_path"),
            rs.getString("form_uid"),
            rs.getString("form_version_uid"),
            rs.getString("team_uid"),
            rs.getString("assignment_uid"),
            rs.getString("submitted_by"),
            instant(rs, "started_at"),
            instant(rs, "received_at"),
            rs.getString("go_live_rule_uid"),
            instant(rs, "live_from"),
            rs.getString("decided_by"),
            instant(rs, "decided_at"),
            rs.getString("note"),
            rs.getString("last_release_error"),
            instant(rs, "last_modified_date"),
            withPayload ? tree(rs.getString("payload")) : null);
    }

    private String json(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot store a held upload", e);
        }
    }

    private JsonNode tree(String payload) {
        try {
            return payload == null ? null : objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read a held upload", e);
        }
    }
}
