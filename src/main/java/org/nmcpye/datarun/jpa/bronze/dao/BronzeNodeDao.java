//package org.nmcpye.datarun.jpa.bronze.dao;
//
//import org.nmcpye.datarun.jpa.bronze.dto.BronzeNodeDto;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author Hamza Assada
// * @since 28/09/2025
// */
//@Repository
//public class BronzeNodeDao {
//    private final NamedParameterJdbcTemplate jdbc;
//
//    public BronzeNodeDao(NamedParameterJdbcTemplate jdbc) {
//        this.jdbc = jdbc;
//    }
//
//    /**
//     * Idempotent upsert keyed by ingestion_id + node_path + coalesce(sequence_index, -1)
//     */
//    public void upsertBronzeNode(BronzeNodeDto node) {
//        String sql = ""
//            + "INSERT INTO bronze_nodes (ingestion_id, submission_id, template_id, template_version, node_path, node_kind, value_type, role_suggestion, cardinality, repeat_instance_id, sequence_index, value_string, value_num, value_bool, value_ts, value_array, classifiers, profiling, registry_version, parser_version, created_at) "
//            + "VALUES (:ingestionId, :submissionId, :templateId, :templateVersion, :nodePath, :nodeKind, :valueType, :roleSuggestion, :cardinality, :repeatInstanceId, :sequenceIndex, :valueString, :valueNum, :valueBool, :valueTs, cast(:valueArray as jsonb), cast(:classifiers as jsonb), cast(:profiling as jsonb), :registryVersion, :parserVersion, now()) "
//            + "ON CONFLICT (ingestion_id, node_path, coalesce(sequence_index, -1)) DO UPDATE SET "
//            + "value_string = EXCLUDED.value_string, value_num = EXCLUDED.value_num, value_bool = EXCLUDED.value_bool, value_ts = EXCLUDED.value_ts, value_array = EXCLUDED.value_array, classifiers = EXCLUDED.classifiers, profiling = EXCLUDED.profiling, registry_version = EXCLUDED.registry_version, parser_version = EXCLUDED.parser_version, created_at = now()";
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("ingestionId", node.getIngestionId());
//        params.put("submissionId", node.getSubmissionId());
//        params.put("templateId", node.getTemplateId());
//        params.put("templateVersion", node.getTemplateVersion());
//        params.put("nodePath", node.getNodePath());
//        params.put("nodeKind", node.getNodeKind());
//        params.put("valueType", node.getValueType());
//        params.put("roleSuggestion", node.getRoleSuggestion());
//        params.put("cardinality", node.getCardinality());
//        params.put("repeatInstanceId", node.getRepeatInstanceId());
//        params.put("sequenceIndex", node.getSequenceIndex());
//        params.put("valueString", node.getValueString());
//        params.put("valueNum", node.getValueNum());
//        params.put("valueBool", node.getValueBool());
//        params.put("valueTs", node.getValueTs());
//        params.put("valueArray", node.getValueArray() == null ? "null" : node.getValueArray().toString());
//        params.put("classifiers", node.getClassifiers() == null ? "null" : node.getClassifiers().toString());
//        params.put("profiling", node.getProfiling() == null ? "null" : node.getProfiling().toString());
//        params.put("registryVersion", node.getRegistryVersion());
//        params.put("parserVersion", node.getParserVersion());
//
//        jdbc.update(sql, params);
//    }
//}
