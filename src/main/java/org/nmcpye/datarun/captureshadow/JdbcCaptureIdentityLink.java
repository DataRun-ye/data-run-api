package org.nmcpye.datarun.captureshadow;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCaptureIdentityLink implements CaptureIdentityLinkPort {

    private static final String SELECT_COLUMNS = """
        capture_id,
        baseline_submission_uid,
        baseline_submission_id,
        baseline_serial_number
        """;

    private static final RowMapper<CaptureIdentityLink> ROW_MAPPER =
        JdbcCaptureIdentityLink::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCaptureIdentityLink(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CaptureIdentityResolution resolveOrInsert(
        CaptureIdentityLink identity
    ) {
        int inserted = jdbc.update(
            """
                INSERT INTO capture_identity_link (
                    capture_id,
                    baseline_submission_uid,
                    baseline_submission_id,
                    baseline_serial_number
                ) VALUES (
                    :captureId,
                    :submissionUid,
                    :submissionId,
                    :serialNumber
                )
                ON CONFLICT DO NOTHING
                """,
            parameters(identity)
        );
        return new CaptureIdentityResolution(
            requireExact(identity),
            inserted == 1
        );
    }

    @Override
    public CaptureIdentityLink requireExact(CaptureIdentityLink expected) {
        List<CaptureIdentityLink> candidates = jdbc.query(
            """
                SELECT
                """ + SELECT_COLUMNS + """
                FROM capture_identity_link
                WHERE capture_id = :captureId
                   OR baseline_submission_uid = :submissionUid
                   OR baseline_submission_id = :submissionId
                   OR baseline_serial_number = :serialNumber
                """,
            parameters(expected),
            ROW_MAPPER
        );
        if (candidates.size() != 1 || !candidates.get(0).equals(expected)) {
            throw new CaptureIdentityConflictException(
                "Conflicting capture identity for submission "
                    + expected.baselineSubmissionUid()
            );
        }
        return candidates.get(0);
    }

    @Override
    public Optional<CaptureIdentityLink> findByCaptureId(UUID captureId) {
        return find(
            "capture_id = :value",
            new MapSqlParameterSource("value", captureId)
        );
    }

    @Override
    public Optional<CaptureIdentityLink> findBySubmissionUid(
        String submissionUid
    ) {
        return find(
            "baseline_submission_uid = :value",
            new MapSqlParameterSource("value", submissionUid)
        );
    }

    private Optional<CaptureIdentityLink> find(
        String predicate,
        MapSqlParameterSource parameters
    ) {
        List<CaptureIdentityLink> values = jdbc.query(
            "SELECT " + SELECT_COLUMNS
                + " FROM capture_identity_link WHERE " + predicate,
            parameters,
            ROW_MAPPER
        );
        if (values.size() > 1) {
            throw new CaptureIdentityConflictException(
                "Capture identity lookup returned more than one row"
            );
        }
        return values.stream().findFirst();
    }

    private static MapSqlParameterSource parameters(
        CaptureIdentityLink identity
    ) {
        return new MapSqlParameterSource()
            .addValue("captureId", identity.captureId())
            .addValue("submissionUid", identity.baselineSubmissionUid())
            .addValue("submissionId", identity.baselineSubmissionId())
            .addValue("serialNumber", identity.baselineSerialNumber());
    }

    private static CaptureIdentityLink mapRow(
        ResultSet resultSet,
        int rowNumber
    ) throws SQLException {
        return new CaptureIdentityLink(
            resultSet.getObject("capture_id", UUID.class),
            resultSet.getString("baseline_submission_uid"),
            resultSet.getString("baseline_submission_id"),
            resultSet.getLong("baseline_serial_number")
        );
    }
}
