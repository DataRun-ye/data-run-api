//package org.nmcpye.datarun.jpa.bronze.dao;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.nmcpye.datarun.jpa.bronze.dto.RawSubmissionDto;
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
//public class RawSubmissionDao {
//    private final NamedParameterJdbcTemplate jdbc;
//    private final ObjectMapper objectMapper;
//
//    public RawSubmissionDao(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
//        this.jdbc = jdbc;
//        this.objectMapper = objectMapper;
//    }
//
//    public void insertRaw(RawSubmissionDto raw) {
//        String sql = "INSERT INTO raw_submissions (ingestion_id, received_at, source_system, template_id, template_version, submission_id, user_id, org_unit, submission_json, created_at) " +
//            "VALUES (:ingestionId, :receivedAt, :sourceSystem, :templateId, :templateVersion, :submissionId, :userId, :orgUnit, :submissionJson::jsonb, now())";
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("ingestionId", raw.getIngestionId());
//        params.put("receivedAt", raw.getReceivedAt());
//        params.put("sourceSystem", raw.getSourceSystem());
//        params.put("templateId", raw.getTemplateId());
//        params.put("templateVersion", raw.getTemplateVersion());
//        params.put("submissionId", raw.getSubmissionId());
//        params.put("userId", raw.getUserId());
//        params.put("orgUnit", raw.getOrgUnit());
//        try {
//            String json = objectMapper.writeValueAsString(raw.getSubmissionJson());
//            params.put("submissionJson", json);
//            jdbc.update(sql, params);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to serialize raw JSON", e);
//        }
//    }
//}
