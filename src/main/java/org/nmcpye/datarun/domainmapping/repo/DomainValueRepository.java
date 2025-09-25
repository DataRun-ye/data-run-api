package org.nmcpye.datarun.domainmapping.repo;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.domainmapping.dto.DomainDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
@Repository
@RequiredArgsConstructor
public class DomainValueRepository {
    private final JdbcTemplate jdbc;

    /**
     * Upsert domain value by idempotency_key.
     * Adjust columns as needed.
     */
    public void upsertDomainValue(DomainDto dto) {
        String sql = """
          INSERT INTO domain_value (
            id, domain_concept_id, submission_uid, repeat_instance_id,
            value_text, value_num, value_bool, value_ts, value_array,
            source_element_data_value_id, etl_run_id, mapping_uid,
            idempotency_key, created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
          ON CONFLICT (idempotency_key) DO UPDATE
            SET value_text = EXCLUDED.value_text,
                value_num = EXCLUDED.value_num,
                value_bool = EXCLUDED.value_bool,
                value_ts = EXCLUDED.value_ts,
                value_array = EXCLUDED.value_array,
                mapping_uid = EXCLUDED.mapping_uid,
                created_at = EXCLUDED.created_at
        """;
        jdbc.update(sql,
            UUID.randomUUID(),
            dto.domainConceptId,
            dto.submissionUid,
            dto.repeatInstanceId,
            dto.valueText,
            dto.valueNum,
            dto.valueBool,
            dto.valueTs == null ? null : Timestamp.from(dto.valueTs),
            dto.valueArrayJson,
            dto.sourceElementDataValueId,
            dto.etlRunId,
            dto.mappingUid,
            dto.idempotencyKey,
            Timestamp.from(java.time.Instant.now())
        );
    }
}
