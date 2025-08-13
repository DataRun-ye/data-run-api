package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Named-parameter JDBC DAO for element_data_value.
 * <p>
 * Important: this class expects the following partial unique indexes exist:
 * - ux_element_single_value  (option_id IS NULL)
 * - ux_element_multi_value   (option_id IS NOT NULL)
 * <p>
 * and the table element_data_value with columns including option_id, value_text, deleted_at etc.
 *
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Repository
public class SubmissionValuesJdbcDao implements ISubmissionValuesDao {

    private final NamedParameterJdbcTemplate jdbc;

    public SubmissionValuesJdbcDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // UPDATE SQL for single-value rows
    private static final String UPDATE_SINGLE_SQL = """
        UPDATE element_data_value
        SET
          value_text = :valueText,
          value_num = :valueNum,
          value_bool = :valueBool,
          category_id = :category,
          deleted_at = NULL,
          last_modified_date = now()
        WHERE submission_id = :submission
          AND repeat_instance_id IS NOT DISTINCT FROM :repeatInstance
          AND element_id = :element
          AND option_id IS NULL;
        """;

    // INSERT SQL for single-value rows
    private static final String INSERT_SINGLE_SQL = """
        INSERT INTO element_data_value (
          submission_id, repeat_instance_id, element_id,
          value_text, value_num, value_bool,
          assignment_id, template_id, category_id,
          deleted_at, created_date, last_modified_date
        ) VALUES (
          :submission, :repeatInstance, :element,
          :valueText, :valueNum, :valueBool,
          :assignment, :template, :category,
          :deletedAt, :createdDate, :lastModifiedDate
        );
        """;

    // UPDATE SQL for multi-value rows
    private static final String UPDATE_MULTI_SQL = """
        UPDATE element_data_value
        SET
          value_text = :valueText,
          category_id = :category,
          deleted_at = NULL,
          last_modified_date = now()
        WHERE submission_id = :submission
          AND repeat_instance_id IS NOT DISTINCT FROM :repeatInstance
          AND element_id = :element
          AND option_id = :optionId;
        """;

    // INSERT SQL for multi-value rows
    private static final String INSERT_MULTI_SQL = """
        INSERT INTO element_data_value (
          submission_id, repeat_instance_id, element_id,
          option_id, value_text,
          assignment_id, template_id, category_id,
          deleted_at, created_date, last_modified_date
        ) VALUES (
          :submission, :repeatInstance, :element,
          :optionId, :valueText,
          :assignment, :template, :category,
          :deletedAt, :createdDate, :lastModifiedDate
        );
        """;

    @Override
    public void upsertSubmissionValue(SubmissionValueRow r) {
        // default single-row upsert by delegating to batch logic for simplicity
        upsertSubmissionValuesBatch(Collections.singletonList(r));
    }

    /**
     * partitions and executes two batchUpdate(...) calls so each set uses the correct
     * ON CONFLICT constraint.
     *
     * @param rows to batch rows
     */
    @Override
    public void upsertSubmissionValuesBatch(List<SubmissionValueRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        final Instant now = Instant.now();

        List<SqlParameterSource> singleBatch = new ArrayList<>();
        List<SqlParameterSource> multiBatch = new ArrayList<>();

        for (SubmissionValueRow r : rows) {
            if (r.getCreatedDate() == null) r.setCreatedDate(now);
            if (r.getLastModifiedDate() == null) r.setLastModifiedDate(now);

            MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            // ... (populate parameterSource with values as before)
            parameterSource.addValue("submission", r.getSubmission());
            parameterSource.addValue("repeatInstance", r.getRepeatInstance());
            parameterSource.addValue("element", r.getElement());
            parameterSource.addValue("valueText", r.getValueText());
            parameterSource.addValue("valueNum", r.getValueNum());
            parameterSource.addValue("valueBool", r.getValueBool());
            parameterSource.addValue("assignment", r.getAssignment());
            parameterSource.addValue("template", r.getTemplate());
            parameterSource.addValue("category", r.getCategory());
            parameterSource.addValue("deletedAt",
                r.getDeletedAt() != null ? Timestamp.from(r.getDeletedAt()) : null);
            parameterSource.addValue("createdDate", Timestamp.from(r.getCreatedDate()));
            parameterSource.addValue("lastModifiedDate", Timestamp.from(r.getLastModifiedDate()));

            if (r.getOption() != null) {
                parameterSource.addValue("optionId", r.getOption());
                multiBatch.add(parameterSource);
            } else {
                singleBatch.add(parameterSource);
            }
        }

        // Step 1: Execute batch updates for both single and multi-select rows
        if (!singleBatch.isEmpty()) {
            jdbc.batchUpdate(UPDATE_SINGLE_SQL, singleBatch.toArray(new SqlParameterSource[0]));
        }
        if (!multiBatch.isEmpty()) {
            jdbc.batchUpdate(UPDATE_MULTI_SQL, multiBatch.toArray(new SqlParameterSource[0]));
        }

        // Step 2: Now, execute batch inserts. The database will handle the rest.
        // NOTE: This approach assumes that an insert will not fail if an update succeeded.
        // If you need more complex logic, you might have to first query for which rows exist.
        if (!singleBatch.isEmpty()) {
            jdbc.batchUpdate(INSERT_SINGLE_SQL, singleBatch.toArray(new SqlParameterSource[0]));
        }
        if (!multiBatch.isEmpty()) {
            jdbc.batchUpdate(INSERT_MULTI_SQL, multiBatch.toArray(new SqlParameterSource[0]));
        }
    }
//    @Override
//    public void upsertSubmissionValuesBatch(List<SubmissionValueRow> rows) {
//        if (rows == null || rows.isEmpty()) return;
//
//        final Instant now = Instant.now();
//        final Timestamp nowTs = Timestamp.from(now);
//
//        // Create lists of SqlParameterSource for each partition
//        List<SqlParameterSource> singleBatch = new ArrayList<>();
//        List<SqlParameterSource> multiBatch = new ArrayList<>();
//
//        for (SubmissionValueRow r : rows) {
//            // Ensure created_date / last_modified_date defaults
//            if (r.getCreatedDate() == null) r.setCreatedDate(now);
//            if (r.getLastModifiedDate() == null) r.setLastModifiedDate(now);
//
//            // Create a MapSqlParameterSource for the current row
//            MapSqlParameterSource parameterSource = new MapSqlParameterSource();
//
//            // Put all parameters, but manually convert Instant to Timestamp
//            parameterSource.addValue("submission", r.getSubmission());
//            parameterSource.addValue("repeatInstance", r.getRepeatInstance());
//            parameterSource.addValue("element", r.getElement());
//            parameterSource.addValue("valueText", r.getValueText());
//            parameterSource.addValue("valueNum", r.getValueNum());
//            parameterSource.addValue("valueBool", r.getValueBool());
//            parameterSource.addValue("assignment", r.getAssignment());
//            parameterSource.addValue("template", r.getTemplate());
//            parameterSource.addValue("category", r.getCategory());
//            parameterSource.addValue("deletedAt",
//                r.getDeletedAt() != null ? Timestamp.from(r.getDeletedAt()) : null);
//            parameterSource.addValue("createdDate", Timestamp.from(r.getCreatedDate()));
//            parameterSource.addValue("lastModifiedDate", Timestamp.from(r.getLastModifiedDate()));
//
//            // Partition the SqlParameterSource objects
//            if (r.getOption() != null) {
//                parameterSource.addValue("optionId", r.getOption()); // Assuming 'optionId' is the named parameter
//                multiBatch.add(parameterSource);
//            } else {
//                singleBatch.add(parameterSource);
//            }
//        }
//
//        // Batch upsert singles
//        if (!singleBatch.isEmpty()) {
//            jdbc.batchUpdate(UPSERT_SINGLE_SQL, singleBatch.toArray(new SqlParameterSource[0]));
//        }
//
//        // Batch upsert multis
//        if (!multiBatch.isEmpty()) {
//            jdbc.batchUpdate(UPSERT_MULTI_SQL, multiBatch.toArray(new SqlParameterSource[0]));
//        }
//    }

    /**
     * Return selection identities for the given submission/repeat/element.
     * Identity = COALESCE(option_id, value_text)
     * Only returns active rows (deleted_at IS NULL).
     * <p>
     * repeatInstanceId may be null to match top-level rows (repeat_instance_id IS NULL).
     * <p>
     * uses a predicate that handles NULL repeat_instance_id by
     * (repeat_instance_id IS NULL AND :repeatInstance IS NULL) OR repeat_instance_id = :repeatInstance.
     * This avoids COALESCE quirks and is safe.
     *
     * @param submissionId     submission id
     * @param repeatInstanceId repeat id
     * @param elementId        element id
     * @return list of active rows options
     */
    @Override
    public List<String> findSelectionIdentitiesForElementRepeat(String submissionId,
                                                                String repeatInstanceId,
                                                                String elementId) {
        String sql;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submissionId)
            .addValue("element", elementId);

        if (repeatInstanceId == null) {
            sql = """
                SELECT COALESCE(option_id, value_text) AS identity
                FROM element_data_value
                WHERE submission_id = :submission
                  AND element_id = :element
                  AND deleted_at IS NULL
                  AND repeat_instance_id IS NULL
                """;
        } else {
            sql = """
                SELECT COALESCE(option_id, value_text) AS identity
                FROM element_data_value
                WHERE submission_id = :submission
                  AND element_id = :element
                  AND deleted_at IS NULL
                  AND repeat_instance_id = :repeatInstance
                """;
            params.addValue("repeatInstance", repeatInstanceId);
        }

        List<String> list = jdbc.queryForList(sql, params, String.class);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * Mark selection rows deleted by their identity (COALESCE(option_id, value_text)).
     * identities must be non-empty.
     *
     * @param submissionId     submission id
     * @param repeatInstanceId repeat id
     * @param elementId        element id
     * @param identities       options identities
     */
    @Override
    public void markSelectionValuesDeletedByIdentity(String submissionId, String repeatInstanceId,
                                                     String elementId, List<String> identities) {
        if (identities == null || identities.isEmpty()) return;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submissionId)
            .addValue("element", elementId)
            .addValue("idents", identities);

        String sql;
        if (repeatInstanceId == null) {
            sql = """
                UPDATE element_data_value
                SET deleted_at = now(), last_modified_date = now()
                WHERE submission_id = :submission
                AND element_id = :element
                AND COALESCE(option_id, value_text) IN (:idents)
                AND repeat_instance_id is NULL
                """;
        } else {
            sql = """
                UPDATE element_data_value
                SET deleted_at = now(), last_modified_date = now()
                WHERE submission_id = :submission
                AND element_id = :element
                AND COALESCE(option_id, value_text) IN (:idents)
                AND repeat_instance_id = :repeatInstance
                """;
            params.addValue("repeatInstance", repeatInstanceId);
        }
//        sql = """
//            UPDATE element_data_value
//            SET deleted_at = now(), last_modified_date = now()
//            WHERE submission_id = :submission
//              AND element_id = :element
//              AND option_id IN (:idents)
//              AND ( (repeat_instance_id IS NULL AND :repeatInstance IS NULL)
//                        OR (repeat_instance_id = :repeatInstance) )
//            """;


        jdbc.update(sql, params);
    }

    @Override
    public void markValuesDeletedForRepeatUids(String submissionId, List<String> repeatUids) {
        if (repeatUids == null || repeatUids.isEmpty()) return;
        String sql = "UPDATE element_data_value SET deleted_at = now(), last_modified_date = now()" +
            " WHERE submission_id = :submission AND repeat_instance_id IN (:uids)";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submission", submissionId)
            .addValue("uids", repeatUids);
        jdbc.update(sql, params);
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
