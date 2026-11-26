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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads stored submissions at their head (their newest revision in the journal), by id or in batches, for the feed's
 * publisher, its reconcile and its agreement harness: the one read seam over {@code data_submission} and the journal.
 * The configuration tables they name are read by their own seams: the form by {@link FormDefinitionJdbcReader}, the
 * placement by {@link PlacementJdbcResolver}. Read-only; it opens no transaction of its own, so it reads in the
 * caller's, through the template it is given.
 *
 * <p>A head is the revision's state ({@code form_data} as text, its version, its deleted flag, its pinned form version
 * and placement, the time of its change), with what a revision leaves out read from {@code data_submission}: the form,
 * the entry times and the creation time.</p>
 *
 * <p><b>Ids, not uids.</b> A submission names its form ({@code data_submission.template_uid}), its pinned version
 * (the revision's {@code template_version_uid}) and its placement by uid, as phones send them. This reader is the edge
 * where they turn into ids, once: the form and the pinned version through {@link FormDefinitionJdbcReader#ids}, the
 * placement through {@link PlacementJdbcResolver}. Nothing it hands on carries a uid.</p>
 */
@Component
public class StoredSubmissionJdbcReader {

    /** Each submission's newest revision, with the stored columns the revision leaves out. */
    private static final String HEADS = """
        SELECT s.id, s.template_uid, r.version, r.form_data::text AS form_data,
               r.metadata ->> 'template_version_uid' AS template_version_uid,
               coalesce((r.metadata ->> 'deleted')::boolean, false) AS deleted,
               CASE WHEN r.kind = 'BASELINE'
                    THEN coalesce((r.metadata ->> 'last_modified_date')::timestamptz, r.recorded_at)
                    ELSE r.recorded_at END AS changed_at,
               s.created_date AT TIME ZONE 'UTC' AS created_at,
               s.start_entry_time AT TIME ZONE 'UTC' AS entered_start_at,
               s.finished_entry_time AT TIME ZONE 'UTC' AS entered_end_at,
               r.metadata::text AS metadata
        FROM data_submission s
        JOIN LATERAL (SELECT x.version, x.kind, x.recorded_at, x.form_data, x.metadata
                      FROM journal.submission_revision x
                      WHERE x.submission_id = s.id
                      ORDER BY x.version DESC
                      LIMIT 1) r ON true
        WHERE s.id IN (:ids)""";

    /** The stored submissions after a cursor, in id order, each with its form and its head's version. */
    private static final String HEAD_VERSIONS = """
        SELECT s.id, s.template_uid,
               (SELECT max(r.version) FROM journal.submission_revision r WHERE r.submission_id = s.id) AS version
        FROM data_submission s
        WHERE s.id > :after
        ORDER BY s.id
        LIMIT :limit""";

    /**
     * A stored submission at its head.
     *
     * @param submission     what the submission normalizer reads: id, form id, pinned version id, document text
     * @param version        the head's version
     * @param deleted        whether the head is deleted
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
     * @param formId       its form's id, or {@code null} when the form it names is not stored
     * @param version      its head's version, or {@code null} when the journal holds no revision of it
     */
    public record HeadVersion(String submissionId, String formId, Long version) {
    }

    /**
     * The form and pinned version a submission names, as stored: uids, until {@link #resolve} turns them into ids.
     *
     * @param templateUid        {@code data_submission.template_uid}, or {@code null}
     * @param templateVersionUid the revision's {@code template_version_uid}, or {@code null} (and always for a head
     *                           version, which needs the form only)
     */
    private record StoredForm(String templateUid, String templateVersionUid) {
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
        // forms and placements resolve once the heads' statement is done: one connection may serve all three
        Map<StoredForm, FormDefinitionJdbcReader.FormIds> forms = resolve(reads.stream().map(Read::form).toList());
        PlacementJdbcResolver placements = new PlacementJdbcResolver(jdbc);
        List<Head> heads = new ArrayList<>(reads.size());
        for (Read read : reads) {
            FormDefinitionJdbcReader.FormIds ids = forms.get(read.form());
            heads.add(new Head(new StoredSubmission(read.submissionId(), ids.formId(), ids.versionId(), read.formData()),
                read.version(), read.deleted(), placements.ofRevision(read.metadata()), read.createdAt(),
                read.enteredStartAt(), read.enteredEndAt(), read.changedAt()));
        }
        return heads;
    }

    /** A head as read, its form, pinned version and placement still stored as uids. */
    private record Read(
        String submissionId,
        StoredForm form,
        String formData,
        long version,
        boolean deleted,
        JsonNode metadata,
        OffsetDateTime createdAt,
        OffsetDateTime enteredStartAt,
        OffsetDateTime enteredEndAt,
        OffsetDateTime changedAt) {
    }

    /** A head version as read, its form still stored as a uid. */
    private record VersionRead(String submissionId, StoredForm form, Long version) {
    }

    /**
     * The stored submissions after {@code after} in id order, at most {@code limit}, with their forms and their heads'
     * versions: a pass over every submission in pages, as reconcile and the agreement harness make it.
     */
    public List<HeadVersion> headVersionsAfter(String after, int limit) {
        List<VersionRead> page = jdbc.query(HEAD_VERSIONS,
            new MapSqlParameterSource().addValue("after", after).addValue("limit", limit),
            (rs, n) -> new VersionRead(rs.getString("id"), new StoredForm(rs.getString("template_uid"), null),
                rs.getObject("version", Long.class)));
        Map<StoredForm, FormDefinitionJdbcReader.FormIds> forms = resolve(page.stream().map(VersionRead::form).toList());
        return page.stream()
            .map(read -> new HeadVersion(read.submissionId(), forms.get(read.form()).formId(), read.version()))
            .toList();
    }

    /** The ids of the stored forms and pins, each turned into ids once, through the form definitions' seam. */
    private Map<StoredForm, FormDefinitionJdbcReader.FormIds> resolve(Collection<StoredForm> stored) {
        FormDefinitionJdbcReader definitions = new FormDefinitionJdbcReader(jdbc, objectMapper);
        Map<StoredForm, FormDefinitionJdbcReader.FormIds> ids = new HashMap<>();
        for (StoredForm form : stored) {
            ids.computeIfAbsent(form, f -> f.templateUid() == null ? new FormDefinitionJdbcReader.FormIds(null, null)
                : definitions.ids(f.templateUid(), f.templateVersionUid()));
        }
        return ids;
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
            rs.getString("id"),
            new StoredForm(rs.getString("template_uid"), rs.getString("template_version_uid")),
            rs.getString("form_data"),
            rs.getLong("version"),
            rs.getBoolean("deleted"),
            metadata,
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("entered_start_at", OffsetDateTime.class),
            rs.getObject("entered_end_at", OffsetDateTime.class),
            rs.getObject("changed_at", OffsetDateTime.class));
    }
}
