package org.nmcpye.datarun.etl.orchestrator;

import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.BatchUpdateException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class EtlFailuresTest {

    private static final String STATEMENT_WITH_VALUES =
        "Batch entry 0 INSERT INTO analytics.tall_canonical (value_text) VALUES ('a person''s name') was aborted";

    @Test
    void aBatchErrorKeepsTheDatabaseErrorLineAndDropsTheStatementAndItsValues() {
        SQLException server = new SQLException(
            "ERROR: value too long for type character varying(26)\n  Detail: Failing row contains (a person's name).",
            "22001");
        BatchUpdateException batch = new BatchUpdateException(STATEMENT_WITH_VALUES, "22001", new int[0]);
        batch.setNextException(server);
        UncategorizedSQLException failure = new UncategorizedSQLException("PreparedStatementCallback", "INSERT ...", batch);

        assertThat(EtlFailures.describe(failure))
            .isEqualTo("ERROR: value too long for type character varying(26) [SQLState 22001]");
    }

    @Test
    void theDeepestDatabaseErrorInTheCausesIsUsed() {
        SQLException server = new SQLException("ERROR: injected projection failure\n  Where: PL/pgSQL function", "P0001");
        BatchUpdateException batch = new BatchUpdateException(STATEMENT_WITH_VALUES, "P0001", new int[0], server);

        assertThat(EtlFailures.describe(new UncategorizedSQLException("task", "sql", batch)))
            .isEqualTo("ERROR: injected projection failure [SQLState P0001]");
    }

    @Test
    void aFailureOutsideTheDatabaseIsNamedByItsRootCause() {
        IllegalArgumentException root = new IllegalArgumentException("No enum constant SemanticType.NotASemanticType");

        assertThat(EtlFailures.describe(new InvalidDataAccessApiUsageException("could not read", root)))
            .isEqualTo("IllegalArgumentException: No enum constant SemanticType.NotASemanticType");
        assertThat(EtlFailures.describe(new IllegalStateException())).isEqualTo("IllegalStateException: (no message)");
    }

    @Test
    void aLongDescriptionIsCut() {
        assertThat(EtlFailures.describe(new IllegalStateException("x".repeat(5000)))).hasSize(EtlFailures.MAX_LENGTH);
    }
}
