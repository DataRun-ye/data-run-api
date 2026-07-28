package org.nmcpye.datarun.assignmentshadow;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcOrgUnitIdentityLink implements OrgUnitIdentityLinkPort {

    private static final String SELECT_COLUMNS = """
        org_unit_id,
        baseline_org_unit_uid
        """;

    private static final String INSERT_SQL = """
        INSERT INTO org_unit_identity_link (
            org_unit_id,
            baseline_org_unit_uid
        )
        VALUES (
            :orgUnitId,
            :baselineOrgUnitUid
        )
        RETURNING
        """ + SELECT_COLUMNS;

    private static final String FIND_BY_ORG_UNIT_ID_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM org_unit_identity_link
        WHERE org_unit_id = :orgUnitId
        """;

    private static final String FIND_BY_BASELINE_ORG_UNIT_UID_SQL = """
        SELECT
        """ + SELECT_COLUMNS + """
        FROM org_unit_identity_link
        WHERE baseline_org_unit_uid = :baselineOrgUnitUid
        """;

    private static final RowMapper<OrgUnitIdentityLink> ROW_MAPPER = JdbcOrgUnitIdentityLink::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcOrgUnitIdentityLink(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public OrgUnitIdentityLink insert(OrgUnitIdentityLink identityLink) {
        return jdbc.queryForObject(
            INSERT_SQL,
            new MapSqlParameterSource()
                .addValue("orgUnitId", identityLink.orgUnitId())
                .addValue("baselineOrgUnitUid", identityLink.baselineOrgUnitUid()),
            ROW_MAPPER
        );
    }

    @Override
    public Optional<OrgUnitIdentityLink> findByOrgUnitId(UUID orgUnitId) {
        return jdbc.query(
            FIND_BY_ORG_UNIT_ID_SQL,
            new MapSqlParameterSource("orgUnitId", orgUnitId),
            ROW_MAPPER
        ).stream().findFirst();
    }

    @Override
    public Optional<OrgUnitIdentityLink> findByBaselineOrgUnitUid(String baselineOrgUnitUid) {
        return jdbc.query(
            FIND_BY_BASELINE_ORG_UNIT_UID_SQL,
            new MapSqlParameterSource("baselineOrgUnitUid", baselineOrgUnitUid),
            ROW_MAPPER
        ).stream().findFirst();
    }

    private static OrgUnitIdentityLink mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OrgUnitIdentityLink(
            resultSet.getObject("org_unit_id", UUID.class),
            resultSet.getString("baseline_org_unit_uid")
        );
    }
}
