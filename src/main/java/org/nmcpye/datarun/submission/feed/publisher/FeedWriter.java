package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.submission.normalized.FieldValue;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.NormalizationFlag;
import org.nmcpye.datarun.submission.normalized.NormalizedRow;
import org.nmcpye.datarun.submission.normalized.NormalizedSubmission;
import org.nmcpye.datarun.submission.normalized.StoredJson;
import org.nmcpye.datarun.submission.normalized.StoredSubmission;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;
import org.nmcpye.datarun.submission.normalized.UnmappedValue;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Writes submissions' current records from their heads (contract 2, 3, 8 and 9), inside the caller's transaction,
 * which holds the feed's lock: the caller takes {@link FeedLock} first, then this reads each head and the version the
 * feed holds, so positions taken here commit in position order.
 *
 * <p><b>The head</b> is the submission's newest revision in the journal, read joined to {@code data_submission} for
 * what a revision leaves out (the form, the entry times, the creation time) and to the tables its placement names, for
 * their ids. A head at or below the version the feed holds is skipped: versions never go backwards. A republication
 * writes the held version again, with new positions, and skips a submission whose held version is newer than its
 * head.</p>
 *
 * <p><b>The records:</b> the submission's record is rewritten with a new position; a row record only when it is new or
 * changed (or always, for a republication), keeping its last {@code changed_in_version} when only its position moves;
 * a row the head no longer has is rewritten as a deleted record. A deleted submission's rows are all deleted records,
 * keeping their last values; a restore makes the head's rows live again. Positions come from {@code feed.position_seq}.
 * Values are the submission normalizer's ({@link SubmissionNormalizer}), never failing on data; its flag counts go to
 * {@code feed_ops.normalization_flags}.</p>
 *
 * <p>A publication also removes the submission's pending change, dead or not, since the feed then holds its head, and
 * moves {@code feed.freshness} forward (last position, last published, newest change). A split partner's head
 * ({@link SplitPartners}) is published with the submission's, in the same savepoint.</p>
 *
 * <p>What makes a head unpublishable throws {@link FeedFailure} with its reason code: no head, a form or a placement
 * uid that names nothing. Database errors propagate. Either way the caller rolls back to its savepoint.</p>
 */
final class FeedWriter {

    enum Outcome {
        /** The head was written. */
        PUBLISHED,
        /** The feed already holds the head, or a newer version: nothing was written. */
        SKIPPED
    }

    /**
     * What one publication did.
     *
     * @param outcome written or skipped
     * @param version the head's version
     * @param flags   the normalizer's flag counts of the written record; empty when skipped
     */
    record Result(Outcome outcome, long version, Map<NormalizationFlag, Integer> flags) {
    }

    private static final String HEAD = """
        SELECT s.id, s.template_uid AS form_uid, r.version, r.form_data::text AS form_data,
               r.metadata ->> 'template_version_uid' AS form_version_uid,
               coalesce((r.metadata ->> 'deleted')::boolean, false) AS deleted,
               CASE WHEN r.kind = 'BASELINE'
                    THEN coalesce((r.metadata ->> 'last_modified_date')::timestamptz, r.recorded_at)
                    ELSE r.recorded_at END AS changed_at,
               s.created_date AT TIME ZONE 'UTC' AS created_at,
               s.start_entry_time AT TIME ZONE 'UTC' AS entered_start_at,
               s.finished_entry_time AT TIME ZONE 'UTC' AS entered_end_at,
               t.id AS form_id, v.id AS form_version_id, a.id AS assignment_id, tm.id AS team_id,
               o.id AS org_unit_id, ac.id AS activity_id, f.version AS held_version
        FROM data_submission s
        JOIN LATERAL (SELECT x.version, x.kind, x.recorded_at, x.form_data, x.metadata
                      FROM journal.submission_revision x
                      WHERE x.submission_id = s.id
                      ORDER BY x.version DESC
                      LIMIT 1) r ON true
        LEFT JOIN data_template t ON t.uid = s.template_uid
        LEFT JOIN data_template_version v ON v.uid = r.metadata ->> 'template_version_uid' AND v.data_template_id = t.id
        LEFT JOIN assignment a ON a.uid = r.metadata ->> 'assignment_uid'
        LEFT JOIN team tm ON tm.uid = r.metadata ->> 'team_uid'
        LEFT JOIN org_unit o ON o.uid = r.metadata ->> 'org_unit_uid'
        LEFT JOIN activity ac ON ac.uid = r.metadata ->> 'activity_uid'
        LEFT JOIN feed.submission f ON f.id = s.id
        WHERE s.id = :submissionId""";

    /** The submission's record, its gone rows and its new or changed rows, in one statement. */
    private static final String WRITE_RECORDS = """
        WITH head_rows AS (
            SELECT h.*
            FROM jsonb_to_recordset(CAST(:rows AS jsonb)) AS h(ord integer, id text, id_origin text,
                                                               parent_row_id text, section_id text,
                                                               row_index integer, field_values jsonb,
                                                               unmapped_values jsonb)
        ),
        written_submission AS (
            INSERT INTO feed.submission AS f (position, version, created_at, entered_start_at, entered_end_at,
                                              changed_at, published_at, deleted, id, form_id, form_version_id,
                                              assignment_id, team_id, org_unit_id, activity_id, field_values,
                                              unmapped_values)
            VALUES (nextval('feed.position_seq'), :version, :createdAt, :enteredStartAt, :enteredEndAt, :changedAt,
                    now(), :deleted, :submissionId, :formId, :formVersionId, :assignmentId, :teamId, :orgUnitId,
                    :activityId, CAST(:fieldValues AS jsonb), CAST(:unmappedValues AS jsonb))
            ON CONFLICT (id) DO UPDATE SET
                position = excluded.position, version = excluded.version, created_at = excluded.created_at,
                entered_start_at = excluded.entered_start_at, entered_end_at = excluded.entered_end_at,
                changed_at = excluded.changed_at, published_at = excluded.published_at, deleted = excluded.deleted,
                form_id = excluded.form_id, form_version_id = excluded.form_version_id,
                assignment_id = excluded.assignment_id, team_id = excluded.team_id,
                org_unit_id = excluded.org_unit_id, activity_id = excluded.activity_id,
                field_values = excluded.field_values, unmapped_values = excluded.unmapped_values
            RETURNING 1
        ),
        gone AS (
            UPDATE feed.submission_row f
            SET position = nextval('feed.position_seq'), published_at = now(),
                changed_in_version = CASE WHEN f.deleted THEN f.changed_in_version ELSE :version END,
                deleted = true
            WHERE f.submission_id = :submissionId
              AND (NOT f.deleted OR :republish)
              AND NOT EXISTS (SELECT 1 FROM head_rows h WHERE h.id = f.id)
            RETURNING 1
        ),
        written_rows AS (
            INSERT INTO feed.submission_row AS f (position, changed_in_version, published_at, row_index, deleted,
                                                  submission_id, id, id_origin, parent_row_id, section_id,
                                                  field_values, unmapped_values)
            SELECT nextval('feed.position_seq'), :version, now(), c.row_index, :deleted, :submissionId, c.id,
                   c.id_origin, c.parent_row_id, c.section_id, c.field_values, c.unmapped_values
            FROM (SELECT h.*
                  FROM head_rows h
                  LEFT JOIN feed.submission_row x ON x.submission_id = :submissionId AND x.id = h.id
                  WHERE x.id IS NULL OR :republish
                     OR (x.deleted, x.id_origin, x.parent_row_id, x.section_id, x.row_index, x.field_values::text,
                         x.unmapped_values::text)
                        IS DISTINCT FROM
                        (:deleted, h.id_origin, h.parent_row_id, h.section_id, h.row_index, h.field_values::text,
                         h.unmapped_values::text)
                  ORDER BY h.ord) c
            ON CONFLICT (submission_id, id) DO UPDATE SET
                position = excluded.position, published_at = excluded.published_at,
                changed_in_version = CASE
                    WHEN (f.deleted, f.id_origin, f.parent_row_id, f.section_id, f.row_index, f.field_values::text,
                          f.unmapped_values::text)
                         IS NOT DISTINCT FROM
                         (excluded.deleted, excluded.id_origin, excluded.parent_row_id, excluded.section_id,
                          excluded.row_index, excluded.field_values::text, excluded.unmapped_values::text)
                    THEN f.changed_in_version ELSE excluded.changed_in_version END,
                row_index = excluded.row_index, deleted = excluded.deleted, id_origin = excluded.id_origin,
                parent_row_id = excluded.parent_row_id, section_id = excluded.section_id,
                field_values = excluded.field_values, unmapped_values = excluded.unmapped_values
            RETURNING 1
        )
        SELECT (SELECT count(*) FROM written_submission) + (SELECT count(*) FROM gone)
               + (SELECT count(*) FROM written_rows)""";

    /** The pending change resolved, the flag counts of the written record, and freshness moved forward. */
    private static final String BOOKKEEPING = """
        WITH resolved AS (
            DELETE FROM feed_ops.pending WHERE submission_id = :submissionId RETURNING 1
        ),
        unflagged AS (
            DELETE FROM feed_ops.normalization_flags
            WHERE submission_id = :submissionId AND CAST(:flags AS jsonb) IS NULL
            RETURNING 1
        ),
        flagged AS (
            INSERT INTO feed_ops.normalization_flags (submission_id, version, flags)
            SELECT :submissionId, :version, CAST(:flags AS jsonb) WHERE CAST(:flags AS jsonb) IS NOT NULL
            ON CONFLICT (submission_id) DO UPDATE SET version = excluded.version, flags = excluded.flags
            RETURNING 1
        )
        INSERT INTO feed.freshness AS f (feed, last_position, last_published_at, last_changed_at, backlog, pending,
                                         dead, checked_at)
        VALUES ('submissions', currval('feed.position_seq'), now(), :changedAt, 0, 0, 0, now())
        ON CONFLICT (feed) DO UPDATE SET
            last_position = greatest(f.last_position, excluded.last_position),
            last_published_at = excluded.last_published_at,
            last_changed_at = greatest(f.last_changed_at, excluded.last_changed_at),
            checked_at = excluded.checked_at""";

    private static final String RESOLVE = "DELETE FROM feed_ops.pending WHERE submission_id = :submissionId";

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private record Head(
        String submissionId,
        String formUid,
        long version,
        String formData,
        String formVersionUid,
        boolean deleted,
        OffsetDateTime changedAt,
        OffsetDateTime createdAt,
        OffsetDateTime enteredStartAt,
        OffsetDateTime enteredEndAt,
        String formId,
        String formVersionId,
        String assignmentId,
        String teamId,
        String orgUnitId,
        String activityId,
        Long heldVersion) {
    }

    private final FormDefinitions definitions;
    private final ObjectMapper objectMapper;
    private final SplitPartners partners;

    FeedWriter(FormDefinitions definitions, ObjectMapper objectMapper, SplitPartners partners) {
        this.definitions = definitions;
        this.objectMapper = objectMapper;
        this.partners = partners;
    }

    /**
     * Publishes the submission's head, then its split partners' heads.
     *
     * @param republish write the held version again with new positions (a republication)
     * @return what was done for the submission itself
     * @throws FeedFailure when a head cannot be published
     */
    Result publish(FeedStatements statements, String submissionId, boolean republish) {
        Result result = publishOne(statements, submissionId, republish);
        for (String partner : partners.of(submissionId, result.version())) {
            if (!partner.equals(submissionId)) {
                publishOne(statements, partner, republish);
            }
        }
        return result;
    }

    private Result publishOne(FeedStatements statements, String submissionId, boolean republish) {
        NamedParameterJdbcTemplate jdbc = statements.named();
        Head head = readHead(jdbc, submissionId);
        if (head.heldVersion() != null
            && (head.heldVersion() > head.version() || (head.heldVersion() == head.version() && !republish))) {
            jdbc.update(RESOLVE, Map.of("submissionId", submissionId));
            return new Result(Outcome.SKIPPED, head.version(), Map.of());
        }
        if (head.formId() == null) {
            throw new FeedFailure(FeedFailure.FORM_UNRESOLVED);
        }
        if (head.assignmentId() == null || head.teamId() == null || head.orgUnitId() == null
            || head.activityId() == null) {
            throw new FeedFailure(FeedFailure.PLACEMENT_UNRESOLVED);
        }
        FormDefinition form = definitions.forHead(new FormDefinitionJdbcReader(jdbc, objectMapper), head.formUid(),
            head.formVersionUid());
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission(submissionId, head.formUid(), head.formVersionUid(), head.formData()), form);

        jdbc.queryForObject(WRITE_RECORDS, new MapSqlParameterSource()
            .addValue("submissionId", submissionId)
            .addValue("version", head.version())
            .addValue("republish", republish)
            .addValue("deleted", head.deleted())
            .addValue("createdAt", head.createdAt(), Types.TIMESTAMP_WITH_TIMEZONE)
            .addValue("enteredStartAt", head.enteredStartAt(), Types.TIMESTAMP_WITH_TIMEZONE)
            .addValue("enteredEndAt", head.enteredEndAt(), Types.TIMESTAMP_WITH_TIMEZONE)
            .addValue("changedAt", head.changedAt(), Types.TIMESTAMP_WITH_TIMEZONE)
            .addValue("formId", head.formId())
            .addValue("formVersionId", head.formVersionId(), Types.VARCHAR)
            .addValue("assignmentId", head.assignmentId())
            .addValue("teamId", head.teamId())
            .addValue("orgUnitId", head.orgUnitId())
            .addValue("activityId", head.activityId())
            .addValue("fieldValues", StoredJson.write(fieldValues(normalized.values())))
            .addValue("unmappedValues", text(unmapped(normalized.unmapped(), UnaryOperator.identity())),
                Types.VARCHAR)
            .addValue("rows", StoredJson.write(rows(normalized.rows()))), Long.class);
        jdbc.update(BOOKKEEPING, new MapSqlParameterSource()
            .addValue("submissionId", submissionId)
            .addValue("version", head.version())
            .addValue("flags", flags(normalized.flags()), Types.VARCHAR)
            .addValue("changedAt", head.changedAt(), Types.TIMESTAMP_WITH_TIMEZONE));
        return new Result(Outcome.PUBLISHED, head.version(), normalized.flags());
    }

    private static Head readHead(NamedParameterJdbcTemplate jdbc, String submissionId) {
        List<Head> heads = jdbc.query(HEAD, Map.of("submissionId", submissionId), (rs, n) -> new Head(
            rs.getString("id"),
            rs.getString("form_uid"),
            rs.getLong("version"),
            rs.getString("form_data"),
            rs.getString("form_version_uid"),
            rs.getBoolean("deleted"),
            rs.getObject("changed_at", OffsetDateTime.class),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("entered_start_at", OffsetDateTime.class),
            rs.getObject("entered_end_at", OffsetDateTime.class),
            rs.getString("form_id"),
            rs.getString("form_version_id"),
            rs.getString("assignment_id"),
            rs.getString("team_id"),
            rs.getString("org_unit_id"),
            rs.getString("activity_id"),
            rs.getObject("held_version", Long.class)));
        if (heads.isEmpty()) {
            throw new FeedFailure(FeedFailure.HEAD_MISSING);
        }
        return heads.get(0);
    }

    /** One container's values, keyed by field id: the normalizer gives each field at most one value per container. */
    private static ObjectNode fieldValues(List<FieldValue> values) {
        ObjectNode object = NODES.objectNode();
        for (FieldValue value : values) {
            if (object.has(value.fieldId())) {
                throw new IllegalStateException("two values for one field in one container");
            }
            object.set(value.fieldId(), value.value());
        }
        return object;
    }

    /** One container's unmapped values, keyed by their path in the container; {@code null} when it has none. */
    private static ObjectNode unmapped(List<UnmappedValue> values, UnaryOperator<String> pathInContainer) {
        if (values.isEmpty()) {
            return null;
        }
        ObjectNode object = NODES.objectNode();
        for (UnmappedValue value : values) {
            String path = pathInContainer.apply(value.path());
            if (object.has(path)) {
                throw new IllegalStateException("two unmapped values at one path in one container");
            }
            object.set(path, value.value());
        }
        return object;
    }

    private static String text(ObjectNode object) {
        return object == null ? null : StoredJson.write(object);
    }

    private static ArrayNode rows(List<NormalizedRow> rows) {
        ArrayNode array = NODES.arrayNode();
        int ord = 0;
        for (NormalizedRow row : rows) {
            ObjectNode node = array.addObject();
            node.put("ord", ++ord);
            node.put("id", row.id());
            node.put("id_origin", row.idOrigin().name());
            node.put("parent_row_id", row.parentRowId());
            node.put("section_id", row.sectionId());
            node.put("row_index", row.index());
            node.set("field_values", fieldValues(row.values()));
            ObjectNode unmapped = unmapped(row.unmapped(), row::pathInRow);
            node.set("unmapped_values", unmapped == null ? NODES.nullNode() : unmapped);
        }
        return array;
    }

    /** The flag counts as JSON text, by flag name; {@code null} when nothing was flagged. */
    private static String flags(Map<NormalizationFlag, Integer> flags) {
        if (flags.isEmpty()) {
            return null;
        }
        ObjectNode object = NODES.objectNode();
        flags.forEach((flag, count) -> object.put(flag.name(), count));
        return StoredJson.write(object);
    }
}
