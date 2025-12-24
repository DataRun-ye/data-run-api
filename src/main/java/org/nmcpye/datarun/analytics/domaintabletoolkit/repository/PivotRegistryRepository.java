package org.nmcpye.datarun.analytics.domaintabletoolkit.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PivotRegistryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Map<String,Object>> findByHash(String pivotHash) {
        String sql = "SELECT * FROM pivot_registry WHERE pivot_hash = :h LIMIT 1";
        var p = new MapSqlParameterSource().addValue("h", pivotHash);
        var rows = jdbc.queryForList(sql, p);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public int insertClaim(String pivotHash, String specJson, Instant expiresAt, String createdBy) {
        String sql = ""
            + "INSERT INTO pivot_registry (pivot_hash, spec, status, created_at, updated_at, expires_at, created_by) "
            + "VALUES (:h, cast(:spec as jsonb), 'running', now(), now(), :exp, :cb) "
            + "ON CONFLICT (pivot_hash) DO UPDATE SET status = 'running', updated_at = now() "
            + "RETURNING 1";
        var p = new MapSqlParameterSource()
            .addValue("h", pivotHash)
            .addValue("spec", specJson)
            .addValue("exp", Timestamp.from(expiresAt))
            .addValue("cb", createdBy);
        try {
            return jdbc.queryForObject(sql, p, Integer.class);
        } catch (Exception ex) {
            return 0;
        }
    }

    public void markReady(String pivotHash, String tableName, Instant expiresAt) {
        String sql = "UPDATE pivot_registry SET status='ready', table_name=:t, updated_at=now(), expires_at=:exp, error=null WHERE pivot_hash=:h";
        var p = new MapSqlParameterSource().addValue("t", tableName).addValue("h", pivotHash).addValue("exp", Timestamp.from(expiresAt));
        jdbc.update(sql, p);
    }

    public void markFailed(String pivotHash, String error) {
        String sql = "UPDATE pivot_registry SET status='failed', error=:e, updated_at=now() WHERE pivot_hash=:h";
        var p = new MapSqlParameterSource().addValue("e", error).addValue("h", pivotHash);
        jdbc.update(sql, p);
    }
}

