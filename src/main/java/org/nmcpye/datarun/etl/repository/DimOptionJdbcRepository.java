package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DimOptionJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Find option by any variant: code, option_uid, option_name, option_name_en, option_name_ar.
     * Return the canonical option row as a map. Keep exact matches first.
     */
    public Optional<Map<String,Object>> findByCodeOrUidOrNameVariants(String token) {
        if (token == null) return Optional.empty();
        String sql = ""
            + "SELECT * FROM analytics.dim_option "
            + "WHERE option_code = :t OR option_uid = :t OR option_name = :t OR option_name_en = :t OR option_name_ar = :t "
            + "ORDER BY CASE WHEN option_code = :t THEN 1 WHEN option_uid = :t THEN 2 WHEN option_name = :t THEN 3 WHEN option_name_en = :t THEN 4 WHEN option_name_ar = :t THEN 5 ELSE 6 END "
            + "LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("t", token);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }

    public Optional<Map<String,Object>> findByOptionSetAndToken(String optionSetUid, String token) {
        if (optionSetUid == null || token == null) return Optional.empty();
        String sql = ""
            + "SELECT * FROM analytics.dim_option "
            + "WHERE option_set_uid = :optSetUid AND (option_code = :t OR option_uid = :t OR option_name = :t OR option_name_en = :t OR option_name_ar = :t) "
            + "ORDER BY CASE WHEN option_code = :t THEN 1 WHEN option_uid = :t THEN 2 WHEN option_name = :t THEN 3 WHEN option_name_en = :t THEN 4 WHEN option_name_ar = :t THEN 5 ELSE 6 END "
            + "LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("optSetUid", optionSetUid).addValue("t", token);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }

    public List<Map<String,Object>> findByOptionSetUid(String optionSetUid) {
        if (optionSetUid == null) return Collections.emptyList();
        String sql = "SELECT * FROM analytics.dim_option WHERE option_set_uid = :optSetUid ORDER BY option_code NULLS LAST";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("optSetUid", optionSetUid);
        return jdbc.queryForList(sql, p);
    }
}
