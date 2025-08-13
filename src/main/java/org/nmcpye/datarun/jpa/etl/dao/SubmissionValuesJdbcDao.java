package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */


@Repository
public class SubmissionValuesJdbcDao implements ISubmissionValuesDao {

    private final NamedParameterJdbcTemplate jdbc;

    public SubmissionValuesJdbcDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * ON CONFLICT target: choose an appropriate unique constraint. Common choice:
     * UNIQUE (submission_id, element_id, repeat_instance_id)
     */
    private static final String UPSERT_SQL = ""
        + "INSERT INTO element_data_value (submission_id, repeat_instance_id, element_id, value_text, value_num, value_bool, assignment_id, template_id, category_id, deleted_at, created_date, last_modified_date) "
        + "VALUES (:submission, :repeatInstance, :element, :valueText, :valueNum, :valueBool, :assignment, :template, :category, :deletedAt, :createdDate, :lastModifiedDate) "
        + "ON CONFLICT (submission_id, element_id, repeat_instance_id) DO UPDATE SET "
        + "  value_text = EXCLUDED.value_text, "
        + "  value_num = EXCLUDED.value_num, "
        + "  value_bool = EXCLUDED.value_bool, "
        + "  assignment_id = EXCLUDED.assignment_id, "
        + "  template_id = EXCLUDED.template_id, "
        + "  category_id = EXCLUDED.category_id, "
        + "  deleted_at = EXCLUDED.deleted_at, "
        + "  last_modified_date = now();";

    @Override
    public void upsertSubmissionValue(SubmissionValueRow r) {
        Objects.requireNonNull(r, "SubmissionValueRow");

        // ensure timestamps
        if (r.getCreatedDate() == null) r.setCreatedDate(new Date());
        if (r.getLastModifiedDate() == null) r.setLastModifiedDate(new Date());

        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(r);
        jdbc.update(UPSERT_SQL, params);
    }

    @Override
    public void upsertSubmissionValuesBatch(List<SubmissionValueRow> rows) {
        if (rows == null || rows.isEmpty()) return;
        rows.forEach(r -> {
            if (r.getCreatedDate() == null) r.setCreatedDate(new Date());
            if (r.getLastModifiedDate() == null) r.setLastModifiedDate(new Date());
        });

        jdbc.batchUpdate(UPSERT_SQL, SqlParameterSourceUtils.createBatch(rows.toArray()));
    }

    private static final String MARK_VALUES_DELETED_SQL = ""
        + "UPDATE element_data_value SET deleted_at = now(), last_modified_date = now() "
        + "WHERE submission_id = :submission AND repeat_instance_id IN (:uids)";

    @Override
    public void markValuesDeletedForRepeatUids(String submissionId, List<String> repeatUids) {
        if (repeatUids == null || repeatUids.isEmpty()) return;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submissionId)
            .addValue("uids", repeatUids);
        jdbc.update(MARK_VALUES_DELETED_SQL, params);
    }
}

//@Repository
//public class SubmissionValuesJdbcDao implements ISubmissionValuesDao {
//
//    private final JdbcTemplate jdbc;
//
//    public SubmissionValuesJdbcDao(JdbcTemplate jdbc) {
//        this.jdbc = jdbc;
//    }
//
//    // Adjust conflict target if you use a different unique key
//    private static final String UPSERT_SQL =
//        "INSERT INTO submission_values (submission_id, repeat_instance_id, element_id, value_text, value_num, value_bool, assignment_id, template_id, category_id, deleted_at, created_date, last_modified_date) " +
//            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
//            "ON CONFLICT (submission_id, element_id, repeat_instance_id) DO UPDATE SET " +
//            " value_text = EXCLUDED.value_text, " +
//            " value_num = EXCLUDED.value_num, " +
//            " value_bool = EXCLUDED.value_bool, " +
//            " assignment_id = EXCLUDED.assignment_id, " +
//            " template_id = EXCLUDED.template_id, " +
//            " category_id = EXCLUDED.category_id, " +
//            " deleted_at = EXCLUDED.deleted_at, " +
//            " last_modified_date = now();";
//
//    @Override
//    public void upsertSubmissionValue(SubmissionValueRow r) {
//        Objects.requireNonNull(r, "SubmissionValueRow");
//
//        Instant created = r.getCreatedDate() == null ? Instant.now() : r.getCreatedDate();
//        Instant lastModified = r.getLastModifiedDate() == null ? Instant.now() : r.getLastModifiedDate();
//
//        jdbc.update(
//            UPSERT_SQL,
//            r.getSubmission(),
//            r.getRepeatInstance(),
//            r.getElement(),
//            r.getValueText(),
//            r.getValueNum(),
//            r.getValueBool(),
//            r.getAssignment(),
//            r.getTemplate(),
//            r.getCategory(),
//            toTimestamp(r.getDeletedAt()),
//            toTimestamp(created),
//            toTimestamp(lastModified)
//        );
//    }
//
//    @Override
//    public void upsertSubmissionValuesBatch(List<SubmissionValueRow> rows) {
//        if (rows == null || rows.isEmpty()) return;
//
//        Instant now = Instant.now();
//        rows.forEach(r -> {
//            if (r.getCreatedDate() == null) r.setCreatedDate(now);
//            if (r.getLastModifiedDate() == null) r.setLastModifiedDate(now);
//        });
//
//        jdbc.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
//            @Override
//            public void setValues(PreparedStatement ps, int i) throws SQLException {
//                SubmissionValueRow r = rows.get(i);
//                ps.setString(1, r.getSubmission());
//                ps.setString(2, r.getRepeatInstance()); // nullable
//                ps.setString(3, r.getElement());
//                ps.setString(4, r.getValueText());
//                if (r.getValueNum() != null) {
//                    ps.setBigDecimal(5, r.getValueNum());
//                } else {
//                    ps.setNull(5, java.sql.Types.NUMERIC);
//                }
//
//                if (r.getValueBool() != null) {
//                    ps.setBoolean(6, r.getValueBool());
//                } else {
//                    ps.setNull(6, java.sql.Types.BOOLEAN);
//                }
//
//                ps.setString(7, r.getAssignment());
//                ps.setString(8, r.getTemplate());
//                ps.setString(9, r.getCategory());
//                setTimestampOrNull(ps, 10, r.getDeletedAt());
//                setTimestampOrNull(ps, 11, r.getCreatedDate());
//                setTimestampOrNull(ps, 12, r.getLastModifiedDate());
//            }
//
//            @Override
//            public int getBatchSize() {
//                return rows.size();
//            }
//        });
//    }
//
//    private static void setTimestampOrNull(PreparedStatement ps, int index, Instant instant) throws SQLException {
//        if (instant != null) {
//            ps.setTimestamp(index, Timestamp.from(instant));
//        } else {
//            ps.setNull(index, java.sql.Types.TIMESTAMP);
//        }
//    }
//
//    private static Timestamp toTimestamp(Instant i) {
//        return i == null ? null : Timestamp.from(i);
//    }
//
//    // Mark values deleted for repeat uids (for given submission)
//    private static final String MARK_VALUES_DELETED_SQL =
//        "UPDATE submission_values SET deleted_at = now(), last_modified_date = now() WHERE submission_id = ? AND repeat_instance_id = ?";
//
//    @Override
//    public void markValuesDeletedForRepeatUids(String submissionId, List<String> repeatUids) {
//        if (repeatUids == null || repeatUids.isEmpty()) return;
//
//        jdbc.batchUpdate(MARK_VALUES_DELETED_SQL, new BatchPreparedStatementSetter() {
//            @Override
//            public void setValues(PreparedStatement ps, int i) throws SQLException {
//                ps.setString(1, submissionId);
//                ps.setString(2, repeatUids.get(i));
//            }
//
//            @Override
//            public int getBatchSize() {
//                return repeatUids.size();
//            }
//        });
//    }
//}
