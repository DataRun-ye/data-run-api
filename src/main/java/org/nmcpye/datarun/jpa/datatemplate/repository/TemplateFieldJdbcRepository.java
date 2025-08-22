package org.nmcpye.datarun.jpa.datatemplate.repository;

import org.nmcpye.datarun.jpa.datatemplate.ElementTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for template_field table using NamedParameterJdbcTemplate.
 *
 * @author Hamza Assada 19/08/2025 (7amza.it@gmail.com)
 */
@Repository
public class TemplateFieldJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TemplateFieldJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String DELETE_BY_TEMPLATE_VERSION =
        "DELETE FROM template_field WHERE template_id = :templateId AND version_id = :versionId";

    public void deleteByTemplateAndVersion(String templateId, String versionId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("templateId", templateId)
            .addValue("versionId", versionId);
        jdbc.update(DELETE_BY_TEMPLATE_VERSION, params);
    }

    private static final String INSERT_SQL =
        "INSERT INTO template_field (" +
            "template_id, version_id, version_no, element_id, path, name, value_type, is_reference, reference_table, option_set_id," +
            "is_repeatable, repeat_path, is_multi, is_measure, default_aggregation, category_for_repeat, display_label, definition, created_at" +
            ") VALUES (" +
            ":templateId, :versionId, :versionNo, :elementId, :path, :name, :valueType, :isReference, :referenceTable, :optionSetId," +
            ":isRepeatable, :repeatPath, :isMulti, :isMeasure, :defaultAggregation, :categoryForRepeat, :displayLabel, :definition, :createdAt" +
            ")";

    /**
     * Bulk insert using NamedParameterJdbcTemplate.batchUpdate
     */
    public void bulkInsert(List<ElementTemplate> rows) {
        if (rows == null || rows.isEmpty()) return;

        List<MapSqlParameterSource> paramsList = new ArrayList<>(rows.size());
        Instant now = Instant.now();

        for (ElementTemplate r : rows) {
            MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("templateId", r.getTemplateId())
                .addValue("versionId", r.getVersionId())
                .addValue("versionNo", r.getVersionNo())
                .addValue("elementId", r.getElementId())
                .addValue("path", r.getPath())
                .addValue("name", r.getName())
                .addValue("valueType", r.getValueType())
                .addValue("isReference", r.getIsReference())
                .addValue("referenceTable", r.getReferenceTable())
                .addValue("optionSetId", r.getOptionSetId())
                .addValue("isRepeatable", r.getIsRepeatable())
                .addValue("repeatPath", r.getRepeatPath())
                .addValue("isMulti", r.getIsMulti())
                .addValue("isMeasure", r.getIsMeasure())
                .addValue("defaultAggregation", r.getAggregationType())
                .addValue("categoryForRepeat", r.getCategoryForRepeat())
                // jsonb fields: pass JSON string (Postgres driver accepts it)
                .addValue("displayLabel", r.getDisplayLabelJson())
                .addValue("definition", r.getDefinitionJson())
                .addValue("createdAt", Timestamp.from(now));
            paramsList.add(p);
        }

        SqlParameterSource[] batch = paramsList.toArray(new SqlParameterSource[0]);
        jdbc.batchUpdate(INSERT_SQL, batch);
    }

    private static final String SELECT_BY_TEMPLATE_VERSION =
        "SELECT id, template_id, version_id, version_no, element_id, path, name, value_type, is_reference, reference_table, option_set_id," +
            "is_repeatable, repeat_path, is_multi, is_measure, default_aggregation, category_for_repeat, display_label::text as display_label, definition::text as definition, created_at " +
            "FROM template_field WHERE template_id = :templateId AND version_id = :versionId ORDER BY id";

    public List<ElementTemplate> findByTemplateAndVersion(String templateId, String versionId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("templateId", templateId)
            .addValue("versionId", versionId);

        return jdbc.query(SELECT_BY_TEMPLATE_VERSION, params, new TemplateFieldRowMapper());
    }

    static class TemplateFieldRowMapper implements RowMapper<ElementTemplate> {
        @Override
        public ElementTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
            ElementTemplate f = new ElementTemplate();
            f.setId(rs.getLong("id"));
            f.setTemplateId(rs.getString("template_id"));
            f.setVersionId(rs.getString("version_id"));
            f.setVersionNo(rs.getInt("version_no"));
            f.setElementId(rs.getString("element_id"));
            f.setPath(rs.getString("path"));
            f.setName(rs.getString("name"));
            f.setValueType(rs.getString("value_type"));
            f.setIsReference(rs.getBoolean("is_reference"));
            f.setReferenceTable(rs.getString("reference_table"));
            f.setOptionSetId(rs.getString("option_set_id"));
            f.setIsRepeatable(rs.getBoolean("is_repeatable"));
            f.setRepeatPath(rs.getString("repeat_path"));
            f.setIsMulti(rs.getBoolean("is_multi"));
            f.setIsMeasure(rs.getBoolean("is_measure"));
            f.setAggregationType(rs.getString("default_aggregation"));
            f.setCategoryForRepeat(rs.getString("category_for_repeat"));
            f.setDisplayLabelJson(rs.getString("display_label"));
            f.setDefinitionJson(rs.getString("definition"));
            Timestamp t = rs.getTimestamp("created_at");
            if (t != null) f.setCreatedAt(t.toInstant());
            return f;
        }
    }
}

