package org.nmcpye.datarun.submission.normalized.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads form definitions for {@link org.nmcpye.datarun.submission.normalized.SubmissionNormalizer}: every version of
 * each form, as stored, with its fields' data element ids. Two read-only statements per call, whatever the number of
 * forms; it writes nothing and opens no transaction of its own, so it joins the caller's when there is one.
 *
 * <p>A {@link FormDefinition} is immutable: a caller may keep it until the form gets a new version.</p>
 */
@Component
public class FormDefinitionJdbcReader {

    private static final String VERSIONS = """
        SELECT t.uid AS form_uid, t.id AS form_id, v.id AS version_id, v.uid AS version_uid, v.version_number,
               v.sections::text AS sections, v.fields::text AS fields
        FROM data_template t
        JOIN data_template_version v ON v.data_template_id = t.id
        WHERE t.uid IN (:formUids)""";

    private static final String FIELD_IDS = """
        SELECT DISTINCT de.uid, de.id
        FROM data_template t
        JOIN data_template_version v ON v.data_template_id = t.id
        CROSS JOIN LATERAL jsonb_array_elements(v.fields) AS f(field)
        JOIN data_element de ON de.uid = f.field ->> 'id'
        WHERE t.uid IN (:formUids)""";

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public FormDefinitionJdbcReader(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /** The definition of the form with this uid (stored submissions name their form by uid), if it exists. */
    public Optional<FormDefinition> read(String formUid) {
        return Optional.ofNullable(read(Set.of(formUid)).get(formUid));
    }

    /**
     * The definitions of the forms with these uids, by form uid. A uid that names no form has no entry: the caller
     * then normalizes with no definition ({@link org.nmcpye.datarun.submission.normalized.NormalizationFlag#FORM_MISSING}).
     * A stored field whose data element no longer exists is kept without an id; nothing in the stored definitions
     * makes the read fail.
     */
    public Map<String, FormDefinition> read(Collection<String> formUids) {
        if (formUids.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> parameters = Map.of("formUids", Set.copyOf(formUids));
        Map<String, String> fieldIdByUid = new HashMap<>();
        jdbc.query(FIELD_IDS, parameters, rs -> {
            fieldIdByUid.put(rs.getString("uid"), rs.getString("id"));
        });
        Map<String, String> formIds = new HashMap<>();
        Map<String, List<FormVersionDefinition>> versions = new HashMap<>();
        jdbc.query(VERSIONS, parameters, rs -> {
            String formUid = rs.getString("form_uid");
            String formId = rs.getString("form_id");
            formIds.put(formUid, formId);
            try {
                versions.computeIfAbsent(formUid, uid -> new ArrayList<>()).add(FormVersionDefinition.parse(
                    formId,
                    rs.getString("version_id"),
                    rs.getString("version_uid"),
                    rs.getInt("version_number"),
                    objectMapper.readTree(rs.getString("sections")),
                    objectMapper.readTree(rs.getString("fields")),
                    fieldIdByUid::get));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("form version " + rs.getString("version_id")
                    + " holds a definition that is not JSON", e);
            }
        });
        Map<String, FormDefinition> forms = new HashMap<>();
        versions.forEach((formUid, list) -> forms.put(formUid, FormDefinition.of(formIds.get(formUid), list)));
        return Map.copyOf(forms);
    }
}
