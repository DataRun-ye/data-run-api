package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.RefResolutionDto;
import org.nmcpye.datarun.etl.dto.RefTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefTypeValueRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String UPSERT_SQL =
        "INSERT INTO analytics.ref_type_value (" +
            "template_uid, submission_uid, instance_key, ce_id, ref_type, raw_value, value_ref_uid, option_set_uid, created_at, updated_at" +
            ") VALUES (" +
            ":templateUid, :submissionUid, :instanceKey, :ceId, :refType, :rawValue, :valueRefUid, :optionSetUid, :createdAt, now()" +
            ") ON CONFLICT (instance_key, ce_id) DO UPDATE SET " +
            "ref_type = EXCLUDED.ref_type, " +
            "raw_value = EXCLUDED.raw_value, " +
            "value_ref_uid = EXCLUDED.value_ref_uid, " +
            "option_set_uid = EXCLUDED.option_set_uid, " +
            "created_at = COALESCE(ref_type_value.created_at, EXCLUDED.created_at), " +
            "updated_at = now()";

    public int upsert(RefTypeValue r) {
        return jdbc.update(UPSERT_SQL, param(r));
    }

    public int[] batchUpsert(Collection<RefTypeValue> rows) {
        return jdbc.batchUpdate(UPSERT_SQL, rows.stream().map(this::param).toArray(MapSqlParameterSource[]::new));
    }

    private MapSqlParameterSource param(RefTypeValue r) {
        return new MapSqlParameterSource()
            .addValue("templateUid", r.getTemplateUid())
            .addValue("submissionUid", r.getSubmissionUid())
            .addValue("instanceKey", r.getInstanceKey())
            .addValue("ceId", r.getCeId())
            .addValue("refType", r.getRefType())
            .addValue("rawValue", r.getRawValue())
            .addValue("valueRefUid", r.getValueRefUid())
            .addValue("createdAt", Timestamp.from(r.getCreatedAt()))
            .addValue("optionSetUid", r.getOptionSetUid());
    }

    /**
     * Optional: fetch by instanceKey for audit/compare
     */
    public List<RefTypeValue> findByInstanceKey(String instanceKey) {
        String sql = "SELECT * FROM analytics.ref_type_value WHERE instance_key = :instanceKey";
        MapSqlParameterSource p = new MapSqlParameterSource("instanceKey", instanceKey);
        return jdbc.query(sql, p, (rs, rowNum) -> {
            RefTypeValue r = new RefTypeValue();
            r.setTemplateUid(rs.getString("template_uid"));
            r.setSubmissionUid(rs.getString("submission_uid"));
            r.setInstanceKey(rs.getString("instance_key"));
            UUID ce = rs.getObject("ce_id", UUID.class);
            if (ce != null) r.setCeId(ce);
            r.setRefType(rs.getString("ref_type"));
            r.setRawValue(rs.getString("raw_value"));
            r.setValueRefUid(rs.getString("value_ref_uid"));
            r.setOptionSetUid(rs.getString("option_set_uid"));
            java.sql.Timestamp created = rs.getTimestamp("created_at");
            if (created != null) r.setCreatedAt(created.toInstant());
            java.sql.Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) r.setUpdatedAt(updated.toInstant());
            return r;
        });
    }

    public Optional<RefResolutionDto> findLatestByRawAndType(String instanceKey, String rawValue, String refType) {
        if (rawValue == null || refType == null) return Optional.empty();
        String sql = "SELECT ref_resolution_uid, raw_value, raw_source, ref_type, resolved_uid, confidence, resolved_at, replaced_by, notes, created_at, updated_at "
            + "FROM analytics.ref_resolution "
            + "WHERE raw_value = :rawValue AND ref_type = :refType "
            + "ORDER BY resolved_at DESC NULLS LAST LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("rawValue", rawValue)
            .addValue("refType", refType);
        List<RefResolutionDto> rows = jdbc.query(sql, params, RefResolutionDto.ROW_MAPPER);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }
}
