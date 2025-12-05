package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.RefTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;

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
}
