package org.nmcpye.datarun.jpa.intake;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores {@link GoLiveRule}s, one per scope (table {@code go_live_rule}). */
@Repository
@RequiredArgsConstructor
// Writes are @Transactional: the pool does not auto-commit, so a write outside a
// transaction is rolled back when its connection returns to the pool.
public class GoLiveRuleRepository {

    private static final RowMapper<GoLiveRule> ROW = (rs, i) -> new GoLiveRule(
        rs.getString("uid"),
        rs.getString("activity_uid"),
        rs.getString("org_unit_uid"),
        rs.getString("form_uid"),
        instant(rs, "live_from"),
        rs.getString("note"),
        rs.getString("last_modified_by"),
        instant(rs, "last_modified_date"));

    private final NamedParameterJdbcTemplate jdbc;

    public List<GoLiveRule> findByActivity(String activity) {
        return jdbc.query("""
                SELECT * FROM go_live_rule WHERE activity_uid = :activity
                ORDER BY org_unit_uid NULLS FIRST, form_uid NULLS FIRST
                """,
            Map.of("activity", activity), ROW);
    }

    public Optional<GoLiveRule> findByUid(String uid) {
        return jdbc.query("SELECT * FROM go_live_rule WHERE uid = :uid", Map.of("uid", uid), ROW)
            .stream().findFirst();
    }

    /** Creates the rule for its scope, or moves that scope's go-live and replaces its note. */
    @Transactional
    public GoLiveRule put(
        String activity, String orgUnit, String form, Instant liveFrom, String note, String modifiedBy) {
        return jdbc.queryForObject("""
                INSERT INTO go_live_rule
                    (uid, activity_uid, org_unit_uid, form_uid, live_from, note, last_modified_by, last_modified_date)
                VALUES (:uid, :activity, :orgUnit, :form, :liveFrom, :note, :modifiedBy, now())
                ON CONFLICT (activity_uid, (COALESCE(org_unit_uid, '')), (COALESCE(form_uid, '')))
                DO UPDATE SET live_from = EXCLUDED.live_from,
                              note = EXCLUDED.note,
                              last_modified_by = EXCLUDED.last_modified_by,
                              last_modified_date = EXCLUDED.last_modified_date
                RETURNING *
                """,
            new MapSqlParameterSource()
                .addValue("uid", CodeGenerator.generateUid())
                .addValue("activity", activity)
                .addValue("orgUnit", orgUnit)
                .addValue("form", form)
                .addValue("liveFrom", utc(liveFrom))
                .addValue("note", note)
                .addValue("modifiedBy", modifiedBy),
            ROW);
    }

    @Transactional
    public boolean delete(String uid) {
        return jdbc.update("DELETE FROM go_live_rule WHERE uid = :uid", Map.of("uid", uid)) > 0;
    }

    /** The forms the campaign's assignments carry. */
    public List<String> formsOfActivity(String activity) {
        return jdbc.queryForList("""
                SELECT DISTINCT form.uid
                FROM assignment a
                JOIN activity act ON act.id = a.activity_id
                CROSS JOIN LATERAL jsonb_array_elements_text(COALESCE(a.forms, '[]'::jsonb)) AS form(uid)
                WHERE act.uid = :activity
                ORDER BY form.uid
                """,
            Map.of("activity", activity), String.class);
    }

    static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
