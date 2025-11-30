package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DimAssignmentJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public List<String> templatesByActivity(String activityUid) {
        if (activityUid == null) return Collections.emptyList();
        String sql = "SELECT Distinct form_uid FROM analytics.dim_assignment WHERE activity_uid = :uid";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("uid", activityUid);
        return jdbc.queryForList(sql, p, String.class);
    }
}

