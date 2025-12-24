package org.nmcpye.datarun.jpa.bronze.dao;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.bronze.dto.BronzeNodeDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;


/**
 * @author Hamza Assada
 * @since 28/09/2025
 */
@RequiredArgsConstructor
@Repository
public class BronzeNodeDao {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Idempotent upsert for a single bronze node.
     * Key uniqueness based on (ingestion_id, node_path, COALESCE(sequence_index,-1)).
     */
    public void upsertBronzeNode(BronzeNodeDto node) {
        final String sql = """
            INSERT INTO bronze_nodes (
              ingestion_id,
              submission_id,
              template_id,
              template_version,
              node_path,
              node_kind,
              value_type,
              role_suggestion,
              cardinality,
              repeat_instance_id,
              sequence_index,
              value_string,
              value_num,
              value_bool,
              value_ts,
              value_array,
              classifiers,
              profiling,
              registry_version,
              parser_version,
              created_at
            ) VALUES (
              :ingestionId,
              :submissionId,
              :templateId,
              :templateVersion,
              :nodePath,
              :nodeKind,
              :valueType,
              :roleSuggestion,
              :cardinality,
              :repeatInstanceId,
              :sequenceIndex,
              :valueString,
              :valueNum,
              :valueBool,
              :valueTs,
              CAST(:valueArray AS jsonb),
              CAST(:classifiers AS jsonb),
              CAST(:profiling AS jsonb),
              :registryVersion,
              :parserVersion,
              now()
            )
            ON CONFLICT (ingestion_id, node_path, COALESCE(sequence_index, -1))
            DO UPDATE SET
              value_string   = EXCLUDED.value_string,
              value_num      = EXCLUDED.value_num,
              value_bool     = EXCLUDED.value_bool,
              value_ts       = EXCLUDED.value_ts,
              value_array    = EXCLUDED.value_array,
              classifiers    = EXCLUDED.classifiers,
              profiling      = EXCLUDED.profiling,
              registry_version = EXCLUDED.registry_version,
              parser_version = EXCLUDED.parser_version,
              created_at     = now()
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("ingestionId", node.getIngestionId())
            .addValue("submissionId", node.getSubmissionId())
            .addValue("templateId", node.getTemplateId())
            .addValue("templateVersion", node.getTemplateVersion())
            .addValue("nodePath", node.getNodePath())
            .addValue("nodeKind", node.getNodeKind())
            .addValue("dataType", node.getValueType())
            .addValue("roleSuggestion", node.getRoleSuggestion())
            .addValue("cardinality", node.getCardinality())
            .addValue("repeatInstanceId", node.getRepeatInstanceId())
            .addValue("sequenceIndex", node.getSequenceIndex())
            .addValue("valueString", node.getValueString())
            .addValue("valueNum", node.getValueNum())
            .addValue("valueBool", node.getValueBool())
            .addValue("valueTs", node.getValueTs())
            // JSON fields: pass a JSON string (or JsonNode.toString()). The CAST(:... AS jsonb) in SQL stores it as jsonb.
            .addValue("valueArray", node.getValueArray() == null ? null : node.getValueArray().toString())
            .addValue("classifiers", node.getClassifiers() == null ? null : node.getClassifiers().toString())
            .addValue("profiling", node.getProfiling() == null ? null : node.getProfiling().toString())
            .addValue("registryVersion", node.getRegistryVersion())
            .addValue("parserVersion", node.getParserVersion());

        jdbc.update(sql, params);
    }
}
