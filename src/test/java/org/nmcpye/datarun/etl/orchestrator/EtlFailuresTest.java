package org.nmcpye.datarun.etl.orchestrator;

import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    void aFailureOutsideTheDatabaseIsNamedByItsRootCauseTypeAndWhereItWasThrown() {
        IllegalArgumentException root = new IllegalArgumentException("No enum constant SemanticType.NotASemanticType");

        assertThat(EtlFailures.describe(new InvalidDataAccessApiUsageException("could not read", root)))
            .startsWith("IllegalArgumentException at " + root.getStackTrace()[0])
            .doesNotContain("NotASemanticType");
    }

    @Test
    void aMessageThatQuotesAnInputIsNeverRepeated() {
        NumberFormatException root = new NumberFormatException("For input string: \"a person's name\"");
        root.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("java.lang.Integer", "parseInt", "Integer.java", 668),
            new StackTraceElement("org.nmcpye.datarun.etl.SomeStep", "read", "SomeStep.java", 42)});

        assertThat(EtlFailures.describe(new IllegalStateException("wrapped", root)))
            .isEqualTo("NumberFormatException at java.lang.Integer.parseInt(Integer.java:668), "
                + "from org.nmcpye.datarun.etl.SomeStep.read(SomeStep.java:42)");
    }

    @Test
    void aLongDatabaseErrorLineIsCut() {
        assertThat(EtlFailures.describe(new SQLException("x".repeat(5000)))).hasSize(EtlFailures.MAX_LENGTH);
    }

    @Test
    void withoutMessagesKeepsTheTypesAndFramesOfTheWholeChainAndNoMessage() {
        SQLException server = new SQLException("ERROR: x\n  Detail: Failing row contains (a person's name).", "23514");
        BatchUpdateException batch = new BatchUpdateException(STATEMENT_WITH_VALUES, "23514", new int[0], server);
        UncategorizedSQLException failure = new UncategorizedSQLException("task", "sql", batch);

        Throwable copy = EtlFailures.withoutMessages(failure);

        List<String> types = new ArrayList<>();
        for (Throwable t = copy, original = failure; t != null; t = t.getCause(), original = original.getCause()) {
            assertThat(t.getMessage()).isEqualTo(original.getClass().getName() + " (message left out)");
            assertThat(t.getStackTrace()).isEqualTo(original.getStackTrace());
            types.add(t.getMessage());
        }
        assertThat(types).hasSize(3);
        assertThat(String.join(" ", types)).doesNotContain("a person");
    }
}
