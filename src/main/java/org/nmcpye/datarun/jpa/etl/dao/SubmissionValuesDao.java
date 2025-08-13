//package org.nmcpye.datarun.jpa.etl.dao;
//
//import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
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
//public class SubmissionValuesDao {
//
//    private final NamedParameterJdbcTemplate jdbc;
//
//    public SubmissionValuesDao(NamedParameterJdbcTemplate jdbc) {
//        this.jdbc = jdbc;
//    }
//
//    public void upsertSubmissionValue(SubmissionValueRow row) {
//        String sql = """
//            INSERT INTO element_data_value (submission_id, element_id, repeat_instance_id, template_id, assignment_id,
//                                            value_text, value_num, value_bool, category_id, created_date, deleted_at)
//            VALUES (:submissionId, :elementId, :repeatId, :templateId, :assignmentId,
//                     :valueText, :valueNum, :valueBool, :categoryId, now(), NULL)
//            ON CONFLICT ON CONSTRAINT ux_element_data_value_elem
//            DO UPDATE SET value_text = EXCLUDED.value_text,
//                          value_num = EXCLUDED.value_num,
//                          value_bool = EXCLUDED.value_bool,
//                          category_id = EXCLUDED.category_id,
//                          assignment_id = EXCLUDED.assignment_id,
//                          deleted_at = NULL;
//            """;
//
//        MapSqlParameterSource params = new MapSqlParameterSource()
//            .addValue("submissionId", row.getSubmission())
//            .addValue("elementId", row.getElement())
//            .addValue("repeatId", row.getRepeatInstance())
//            .addValue("templateId", row.getTemplate())
//            .addValue("assignmentId", row.getAssignment())
//            .addValue("valueText", row.getValueText())
//            .addValue("valueNum", row.getValueNum())
//            .addValue("valueBool", row.getValueBool())
//            .addValue("categoryId", row.getCategory());
//        jdbc.update(sql, params);
//    }
//
//    public void markValuesDeletedForRepeatUids(String submissionId, List<String> ids) {
//        String sql = "UPDATE element_data_value " +
//            "SET deleted_at = now(), updated_at = now() " +
//            "WHERE submission_id = :submissionId " +
//            "AND repeat_instance_id = ANY(:ids::text[]) " +
//            "AND deleted_at IS NULL";
//        MapSqlParameterSource params = new MapSqlParameterSource()
//            .addValue("submissionId", submissionId)
//            .addValue("ids", ids.toArray(new String[0]));
//        jdbc.update(sql, params);
//    }
//}
