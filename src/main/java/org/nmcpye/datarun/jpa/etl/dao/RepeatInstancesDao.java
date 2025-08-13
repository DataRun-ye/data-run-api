//package org.nmcpye.datarun.jpa.etl.dao;
//
//import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
///**
// * @author Hamza Assada 10/08/2025 (7amza.it@gmail.com)
// */
//@Repository
//public class RepeatInstancesDao {
//
//    private final NamedParameterJdbcTemplate jdbc;
//
//    public RepeatInstancesDao(NamedParameterJdbcTemplate jdbc) {
//        this.jdbc = jdbc;
//    }
//
//    public void upsertRepeatInstance(RepeatInstance repeatInstance) {
//        String sql = """
//            INSERT INTO repeat_instance (id, submission_id, repeat_path, repeat_index, client_updated_at, deleted_at, created_date, last_modified_date, created_by, last_modified_by)
//            VALUES (:id, :submissionId, :repeatPath, :repeatIndex, :clientUpdatedAt, NULL, now(), now(), :createdBy, :lastModifiedBy)
//            ON CONFLICT (id) DO UPDATE
//              SET client_updated_at = EXCLUDED.client_updated_at,
//                  repeat_path = EXCLUDED.repeat_path,
//                  repeat_index = EXCLUDED.repeat_index,
//                  last_modified_by = EXCLUDED.last_modified_by,
//                  deleted_at = NULL,
//                  last_modified_date = now();
//            """;
//
//        MapSqlParameterSource params = new MapSqlParameterSource()
//            .addValue("id", repeatInstance.getId())
//            .addValue("submissionId", repeatInstance.getSubmission())
//            .addValue("repeatPath", repeatInstance.getRepeatPath())
//            .addValue("repeatIndex", repeatInstance.getRepeatIndex())
//            .addValue("clientUpdatedAt", repeatInstance.getClientUpdatedAt())
//            .addValue("createdBy", repeatInstance.getCreatedBy())
//            .addValue("lastModifiedBy", repeatInstance.getLastModifiedBy());
//
//        jdbc.update(sql, params);
//    }
//
//    public List<String> findActiveRepeatUids(String submissionId, String repeatPath) {
//        String sql = "SELECT repeat_instance_uid " +
//            "FROM repeat_instance " +
//            "WHERE submission_id = :submissionId " +
//            "AND repeat_path = :repeatPath " +
//            "AND deleted_at IS NULL";
//        return jdbc.query(sql, java.util.Map.of("submissionId", submissionId, "repeatPath", repeatPath),
//            (rs, rowNum) -> rs.getString("repeat_instance_uid"));
//    }
//
//    public void markRepeatInstancesDeleted(String submissionId, String repeatPath, List<String> ids) {
//        String sql = "UPDATE repeat_instance " +
//            "SET deleted_at = now(), updated_at = now() " +
//            "WHERE submission_id = :submissionId " +
//            "AND repeat_path = :repeatPath " +
//            "AND id = ANY(:ids::text[]) " +
//            "AND deleted_at IS NULL";
//        MapSqlParameterSource params = new MapSqlParameterSource()
//            .addValue("submissionId", submissionId)
//            .addValue("repeatPath", repeatPath)
//            .addValue("ids", ids.toArray(new String[0]));
//        jdbc.update(sql, params);
//    }
//}
