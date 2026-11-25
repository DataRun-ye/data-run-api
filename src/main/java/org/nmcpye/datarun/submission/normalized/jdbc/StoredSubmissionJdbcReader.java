package org.nmcpye.datarun.submission.normalized.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.submission.normalized.StoredSubmission;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads stored submissions at their head (their newest revision in the journal), by id or in batches, for the feed's
 * publisher, its reconcile and its agreement harness: the one read seam over {@code data_submission}, the journal and
 * the configuration tables they name, beside {@link FormDefinitionJdbcReader}. Read-only; it opens no transaction of
 * its own, so it reads in the caller's, through the template it is given.
 *
 * <p>A head is the revision's state ({@code form_data} as text, its version, its deleted flag, its pinned form version
 * and placement, the time of its change), with what a revision leaves out read from {@code data_submission}: the form,
 * the entry times and the creation time. Its placement is resolved to ids by {@link PlacementJdbcResolver}.</p>
 */
@Component
public class StoredSubmissionJdbcReader {

    /** Each submission's newest revision, with the stored columns the revision leaves out and the form's ids. */
    private static final String HEADS = """
        SELECT s.id, s.template_uid AS form_uid, r.version, r.form_data::text AS form_data,
               r.metadata ->> 'template_version_uid' AS form_version_uid,
               coalesce((r.metadata ->> 'deleted')::boolean, false) AS deleted,
               CASE WHEN r.kind = 'BASELINE'
                    THEN coalesce((r.metadata ->> 'last_modified_date')::timestamptz, r.recorded_at)
                    ELSE r.recorded_at END AS changed_at,
               s.created_date AT TIME ZONE 'UTC' AS created_at,
               s.start_entry_time AT TIME ZONE 'UTC' AS entered_start_at,
               s.finished_entry_time AT TIME ZONE 'UTC' AS entered_end_at,
               t.id AS form_id, v.id AS form_version_id, r.metadata::text AS metadata
        FROM data_submission s
        JOIN LATERAL (SELECT x.version, x.kind, x.recorded_at, x.form_data, x.metadata
                      FROM journal.submission_revision x
                      WHERE x.submission_id = s.id
                      ORDER BY x.version DESC
                      LIMIT 1) r ON true
        LEFT JOIN data_template t ON t.uid = s.template_uid
        LEFT JOIN data_template_version v ON v.uid = r.metadata ->> 'template_version_uid' AND v.data_template_id = t.id
        WHERE s.id IN (:ids)""";

    /** The stored submissions after a cursor, in id order, each with its form and its head's version. */
    private static final String HEAD_VERSIONS = """
        SELECT s.id, s.template_uid AS form_uid,
               (SELECT max(r.version) FROM journal.submission_revision r WHERE r.submission_id = s.id) AS version
        FROM data_submission s
        WHERE s.id > :after
        ORDER BY s.id
        LIMIT :limit""";

    /**
     * A stored submission at its head.
     *
     * @param submission     what the submission normalizer reads: id, form uid, pinned version uid, document text
     * @param version        the head's version
     * @param deleted        whether the head is deleted
     * @param formId         the form's id, or {@code null} when its uid names no stored form
     * @param formVersionId  the pinned form version's id, or {@code null} when it is not among the form's versions
     * @param placement      what the head is tied to, as ids
     * @param createdAt      when the submission was stored
     * @param enteredStartAt the phone's start of filling the form, or {@code null}
     * @param enteredEndAt   the phone's end of filling the form, or {@code null}
     * @param changedAt      when the head's change was recorded: for a baseline, when its state was reached
     */
    public record Head(
        StoredSubmission submission,
        long version,
        boolean deleted,
        String formId,
        String formVersionId,
        Placement placement,
        OffsetDateTime createdAt,
        OffsetDateTime enteredStartAt,
        OffsetDateTime enteredEndAt,
        OffsetDateTime changedAt) {
    }

    /**
     * A stored submission's id and form, and its head's version.
     *
     * @param submissionId the submission's id
     * @param formUid      its form's uid, as stored
     * @param version      its head's version, or {@code null} when the journal holds no revision of it
     */
    public record HeadVersion(String submissionId, String formUid, Long version) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public StoredSubmissionJdbcReader(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /** The submission at its head, or empty when it is not stored or the journal holds no revision of it. */
    public Optional<Head> head(String submissionId) {
        List<Head> heads = heads(Set.of(submissionId));
        return heads.isEmpty() ? Optional.empty() : Optional.of(heads.get(0));
    }

    /** These submissions at their heads, in no particular order; one not stored, or without a revision, is left out. */
    public List<Head> heads(Collection<String> submissionIds) {
        if (submissionIds.isEmpty()) {
            return List.of();
        }
        List<Read> reads = jdbc.query(HEADS, new MapSqlParameterSource("ids", Set.copyOf(submissionIds)),
            (rs, n) -> read(rs));
        // placements resolve once the heads' statement is done: one connection may serve both
        PlacementJdbcResolver placements = new PlacementJdbcResolver(jdbc);
        List<Head> heads = new ArrayList<>(reads.size());
        for (Read read : reads) {
            heads.add(new Head(read.submission(), read.version(), read.deleted(), read.formId(), read.formVersionId(),
                placements.ofRevision(read.metadata()), read.createdAt(), read.enteredStartAt(), read.enteredEndAt(),
                read.changedAt()));
        }
        return heads;
    }

    /** A head as read, its placement still stored as uids in its metadata. */
    private record Read(
        StoredSubmission submission,
        long version,
        boolean deleted,
        String formId,
        String formVersionId,
        JsonNode metadata,
        OffsetDateTime createdAt,
        OffsetDateTime enteredStartAt,
        OffsetDateTime enteredEndAt,
        OffsetDateTime changedAt) {
    }

    /**
     * The stored submissions after {@code after} in id order, at most {@code limit}, with their heads' versions: a
     * pass over every submission in pages, as reconcile and the agreement harness make it.
     */
    public List<HeadVersion> headVersionsAfter(String after, int limit) {
        return jdbc.query(HEAD_VERSIONS, new MapSqlParameterSource().addValue("after", after).addValue("limit", limit),
            (rs, n) -> new HeadVersion(rs.getString("id"), rs.getString("form_uid"),
                rs.getObject("version", Long.class)));
    }

    private Read read(ResultSet rs) throws SQLException {
        JsonNode metadata;
        try {
            metadata = objectMapper.readTree(rs.getString("metadata"));
        } catch (JsonProcessingException e) {
            // the column is jsonb, which always prints as JSON
            throw new IllegalStateException("a revision's metadata is not JSON", e);
        }
        return new Read(
            new StoredSubmission(rs.getString("id"), rs.getString("form_uid"), rs.getString("form_version_uid"),
                rs.getString("form_data")),
            rs.getLong("version"),
            rs.getBoolean("deleted"),
            rs.getString("form_id"),
            rs.getString("form_version_id"),
            metadata,
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("entered_start_at", OffsetDateTime.class),
            rs.getObject("entered_end_at", OffsetDateTime.class),
            rs.getObject("changed_at", OffsetDateTime.class));
    }
}
