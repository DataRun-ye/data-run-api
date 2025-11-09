package org.nmcpye.datarun.jpa.datatemplate.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataUpsertService {
    private final NamedParameterJdbcTemplate npJdbc;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 800; // tune for your env

    // -------- canonical_element upsert ----------
    private static final String CANONICAL_UPSERT_SQL = """
        INSERT INTO canonical_element
          (canonical_element_uid, template_uid, preferred_name, data_type, semantic_type, display_label,
           cardinality, option_set_id, canonical_candidates, notes, created_date, last_modified_date)
        VALUES
          (:canonicalElementUid, :templateUid, :preferredName, :dataType, :semanticType, CAST(:displayLabel AS jsonb),
           :cardinality, :optionSetId, :notes, now(), now())
        ON CONFLICT (canonical_element_uid) DO UPDATE
          SET preferred_name = EXCLUDED.preferred_name,
              data_type = EXCLUDED.data_type,
              semantic_type = EXCLUDED.semantic_type,
              canonical_path = COALESCE(EXCLUDED.canonical_path, canonical_element.canonical_path),
              cardinality = COALESCE(EXCLUDED.cardinality, canonical_element.cardinality),
              option_set_id = COALESCE(EXCLUDED.option_set_id, canonical_element.option_set_id),
              canonical_candidates = canonical_element.canonical_candidates || EXCLUDED.canonical_candidates,
              notes = COALESCE(EXCLUDED.notes, canonical_element.notes)
        """;

    @Transactional
    public void upsertCanonicalElements(List<CanonicalElement> elems) {
        if (elems == null || elems.isEmpty()) return;
        List<SqlParameterSource> batch = new ArrayList<>(elems.size());
        for (CanonicalElement e : elems) {
            MapSqlParameterSource p = new MapSqlParameterSource();
            p.addValue("canonicalElementUid", e.getCanonicalElementUid());
            p.addValue("template_uid", e.getTemplateUid());
            p.addValue("preferredName", e.getPreferredName());
            p.addValue("dataType", e.getDataType() == null ? null : e.getDataType().name());
            p.addValue("semanticType", e.getSemanticType() == null ? null : e.getSemanticType().name());
            try {
                p.addValue("displayLabel", objectMapper.writeValueAsString(e.getDisplayLabel()));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
            p.addValue("cardinality", e.getCardinality());
            p.addValue("optionSetId", e.getOptionSetId());
            p.addValue("notes", e.getNotes());
            batch.add(p);
        }
        // chunked execution
        for (int i = 0; i < batch.size(); i += BATCH_SIZE) {
            int end = Math.min(batch.size(), i + BATCH_SIZE);
            List<SqlParameterSource> slice = batch.subList(i, end);
            npJdbc.batchUpdate(CANONICAL_UPSERT_SQL, slice.toArray(new SqlParameterSource[0]));
        }
    }

    // -------- template_element upsert ----------
    // Ensure you have unique index on (template_uid, template_version_uid, id_path)
    private static final String TEMPLATE_UPSERT_SQL = """
        INSERT INTO template_element
          (uid, template_uid, template_version_uid, template_version_no, element_kind, canonical_element_uid,
           schema_fingerprint, id_path, json_data_path, canonical_path, name, data_element_uid,
           data_type, semantic_type, cardinality, option_set_uid, parent_repeat_json_data_path,
           parent_repeat_canonical_path, display_label, sort_order, value_type, created_date)
        VALUES
          (:uid, :templateUid, :templateVersionUid, :versionNo, :elementKind, :canonicalElementUid,
           :schemaFingerprint, :idPath, :jsonDataPath, :canonicalPath, :name, :dataElementUid,
           :dataType, :semanticType, :cardinality, :optionSetUid, :parentRepeatJsonDataPath,
           :parentRepeatCanonicalPath, CAST(:displayLabel AS jsonb), :sortOrder, :valueType, now())
        ON CONFLICT (template_uid, template_version_uid, id_path) DO UPDATE
          SET schema_fingerprint = EXCLUDED.schema_fingerprint,
              canonical_element_uid = EXCLUDED.canonical_element_uid,
              name = EXCLUDED.name,
              data_element_uid = EXCLUDED.data_element_uid,
              data_type = EXCLUDED.data_type,
              semantic_type = EXCLUDED.semantic_type,
              cardinality = EXCLUDED.cardinality,
              option_set_uid = EXCLUDED.option_set_uid,
              parent_repeat_json_data_path = EXCLUDED.parent_repeat_json_data_path,
              parent_repeat_canonical_path = EXCLUDED.parent_repeat_canonical_path,
              display_label = EXCLUDED.display_label,
              sort_order = EXCLUDED.sort_order,
              value_type = EXCLUDED.value_type
        """;

    @Transactional
    public void upsertTemplateElements(List<TemplateElement> elems) {
        if (elems == null || elems.isEmpty()) return;
        List<SqlParameterSource> batch = new ArrayList<>(elems.size());
        for (TemplateElement e : elems) {
            MapSqlParameterSource p = new MapSqlParameterSource();
            p.addValue("uid", e.getUid());
            p.addValue("templateUid", e.getTemplateUid());
            p.addValue("templateVersionUid", e.getTemplateVersionUid());
            p.addValue("versionNo", e.getVersionNo());
            p.addValue("elementKind", e.getElementKind() == null ? null : e.getElementKind().name());
            p.addValue("canonicalElementUid", e.getCanonicalElementUid());
            p.addValue("schemaFingerprint", e.getSchemaFingerprint());
            p.addValue("idPath", e.getIdPath());
            p.addValue("jsonDataPath", e.getJsonDataPath());
            p.addValue("canonicalPath", e.getCanonicalPath());
            p.addValue("name", e.getName());
            p.addValue("dataElementUid", e.getDataElementUid());
            p.addValue("dataType", e.getDataType() == null ? null : e.getDataType().name());
            p.addValue("semanticType", e.getSemanticType() == null ? null : e.getSemanticType().name());
            p.addValue("cardinality", e.getCardinality());
            p.addValue("optionSetUid", e.getOptionSetUid());
            p.addValue("parentRepeatJsonDataPath", e.getParentRepeatJsonDataPath());
            p.addValue("parentRepeatCanonicalPath", e.getParentRepeatCanonicalPath());
            try {
                p.addValue("displayLabel", objectMapper.writeValueAsString(e.getDisplayLabel()));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
            p.addValue("sortOrder", e.getSortOrder());
            p.addValue("valueType", e.getValueType() == null ? null : e.getValueType().name());
            batch.add(p);
        }

        for (int i = 0; i < batch.size(); i += BATCH_SIZE) {
            int end = Math.min(batch.size(), i + BATCH_SIZE);
            List<SqlParameterSource> slice = batch.subList(i, end);
            npJdbc.batchUpdate(TEMPLATE_UPSERT_SQL, slice.toArray(new SqlParameterSource[0]));
        }
    }
}
