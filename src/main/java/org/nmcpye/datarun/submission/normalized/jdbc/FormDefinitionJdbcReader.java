package org.nmcpye.datarun.submission.normalized.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads form definitions for {@link org.nmcpye.datarun.submission.normalized.SubmissionNormalizer}: every version of
 * each form as stored, with the stored ids of the data elements its fields name, in one read-only statement, so one
 * snapshot. It writes nothing and opens no transaction of its own, so it joins the caller's.
 *
 * <p><b>Change tokens.</b> Each definition comes with a token that changes whenever anything it was built from
 * changes: a version added, rewritten or deleted, or a data element its fields name changed, deleted or created.
 * {@link #changeTokens} computes the current tokens without reading the definitions, so a caller keeping
 * definitions can tell when to read them again. The token is a SHA-256, over each version's id and row version
 * ({@code xmin}) and those of its data elements; its text has no other meaning.</p>
 */
@Component
public class FormDefinitionJdbcReader {

    /** Per version: its id and row version, then each data element its fields name, with their row versions. */
    private static final String VERSION_MARK = """
        v.id || ':' || v.xmin::text || ':' || coalesce((
            SELECT string_agg(de.id || ':' || de.xmin::text, ',' ORDER BY de.id)
            FROM jsonb_array_elements(v.fields) AS f(field)
            JOIN data_element de ON de.uid = f.field ->> 'id'), '')""";

    private static final String DEFINITIONS = """
        SELECT t.uid AS form_uid, t.id AS form_id, v.id AS version_id, v.uid AS version_uid, v.version_number,
               v.sections::text AS sections, v.fields::text AS fields,
               (SELECT coalesce(jsonb_object_agg(de.uid, de.id), '{}'::jsonb)::text
                FROM jsonb_array_elements(v.fields) AS f(field)
                JOIN data_element de ON de.uid = f.field ->> 'id') AS field_ids,
               """ + VERSION_MARK + """
         AS version_mark
        FROM data_template t
        JOIN data_template_version v ON v.data_template_id = t.id
        WHERE t.uid IN (:formUids)""";

    private static final String TOKENS = """
        SELECT t.uid AS form_uid,
               """ + VERSION_MARK + """
         AS version_mark
        FROM data_template t
        JOIN data_template_version v ON v.data_template_id = t.id
        WHERE t.uid IN (:formUids)""";

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public FormDefinitionJdbcReader(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * A form's definition as read, and the change token of what it was built from.
     *
     * @param definition  the definition
     * @param changeToken changes whenever a version or a data element it was built from changes
     */
    public record Snapshot(FormDefinition definition, String changeToken) {
    }

    /** The definition of the form with this uid (stored submissions name their form by uid), if it exists. */
    public Optional<Snapshot> read(String formUid) {
        return Optional.ofNullable(read(Set.of(formUid)).get(formUid));
    }

    /**
     * The definitions of the forms with these uids, by form uid. A uid that names no form has no entry: the caller
     * then normalizes with no definition ({@link org.nmcpye.datarun.submission.normalized.NormalizationFlag#FORM_MISSING}).
     * A stored field whose data element no longer exists is kept without an id; nothing in the stored definitions
     * makes the read fail.
     */
    public Map<String, Snapshot> read(Collection<String> formUids) {
        if (formUids.isEmpty()) {
            return Map.of();
        }
        Map<String, String> formIds = new HashMap<>();
        Map<String, List<FormVersionDefinition>> versions = new HashMap<>();
        Map<String, List<String>> marks = new HashMap<>();
        jdbc.query(DEFINITIONS, Map.of("formUids", Set.copyOf(formUids)), rs -> {
            String formUid = rs.getString("form_uid");
            String formId = rs.getString("form_id");
            formIds.put(formUid, formId);
            marks.computeIfAbsent(formUid, uid -> new ArrayList<>()).add(rs.getString("version_mark"));
            try {
                JsonNode fieldIds = objectMapper.readTree(rs.getString("field_ids"));
                versions.computeIfAbsent(formUid, uid -> new ArrayList<>()).add(FormVersionDefinition.parse(
                    formId,
                    rs.getString("version_id"),
                    rs.getString("version_uid"),
                    rs.getInt("version_number"),
                    objectMapper.readTree(rs.getString("sections")),
                    objectMapper.readTree(rs.getString("fields")),
                    uid -> fieldIds.hasNonNull(uid) ? fieldIds.get(uid).asText() : null));
            } catch (JsonProcessingException e) {
                // the columns are jsonb, which always prints as JSON
                throw new IllegalStateException("form version " + rs.getString("version_id")
                    + " holds a definition that is not JSON", e);
            }
        });
        Map<String, Snapshot> forms = new HashMap<>();
        versions.forEach((formUid, list) -> forms.put(formUid,
            new Snapshot(FormDefinition.of(formIds.get(formUid), list), token(marks.get(formUid)))));
        return Map.copyOf(forms);
    }

    /** The current change token of each form with these uids, without reading the definitions. */
    public Map<String, String> changeTokens(Collection<String> formUids) {
        if (formUids.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> marks = new HashMap<>();
        jdbc.query(TOKENS, Map.of("formUids", Set.copyOf(formUids)), rs -> {
            marks.computeIfAbsent(rs.getString("form_uid"), uid -> new ArrayList<>()).add(rs.getString("version_mark"));
        });
        Map<String, String> tokens = new HashMap<>();
        marks.forEach((formUid, list) -> tokens.put(formUid, token(list)));
        return Map.copyOf(tokens);
    }

    private static String token(List<String> versionMarks) {
        List<String> sorted = new ArrayList<>(versionMarks);
        sorted.sort(null);
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(String.join("|", sorted).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}
