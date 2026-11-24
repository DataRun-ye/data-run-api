package org.nmcpye.datarun.etl;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.FastCheckService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The changeset that drops the five unused indexes of analytics.tall_canonical, on its own against PostgreSQL: it
 * drops exactly those five, keeps the primary key, the upsert's unique key and the two used indexes, runs once,
 * rolls back to the same definitions, and fails after its lock timeout instead of waiting behind a long read.
 */
class TallUnusedIndexesMigrationIT {

    private static final String CHANGELOG = "config/liquibase/changelog/etl/20261125-drop-unused-tall-indexes.xml";
    private static final String CHANGESET = "20261125-01-drop-unused-tall-indexes";

    private static final List<String> KEPT = List.of(
        "idx_tall_canonical_submission_uid", "idx_tall_canonical_template_element", "pk_tall_canonical",
        "uq_tall_instance_canonical");

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        db.execute("CREATE SCHEMA analytics");
        db.execute("""
            CREATE TABLE analytics.tall_canonical (
              tall_id bigserial CONSTRAINT pk_tall_canonical PRIMARY KEY,
              submission_uid varchar(11) NOT NULL,
              template_uid varchar(11) NOT NULL,
              template_version_uid varchar(11),
              instance_key text NOT NULL,
              canonical_element_id uuid NOT NULL,
              repeat_instance_id varchar(128),
              parent_instance_id varchar(128),
              value_ref_uid varchar(11),
              updated_at timestamp,
              CONSTRAINT uq_tall_instance_canonical UNIQUE (instance_key, canonical_element_id))""");
        db.update("""
            INSERT INTO analytics.tall_canonical (submission_uid, template_uid, template_version_uid, instance_key,
                                                  canonical_element_id, repeat_instance_id, updated_at)
            SELECT 's' || i, 'tmpl0000001', 'version0001', 's' || i, gen_random_uuid(), 'r' || i, now()
            FROM generate_series(1, 1000) AS i""");
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    /** The indexes as production has them (changeset 2025-11-15-05-indexes), and no history. */
    @BeforeEach
    void startFromProductionsIndexesAndNoHistory() {
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
        for (String index : List.of(
            "CREATE INDEX IF NOT EXISTS idx_tall_canonical_template_version_element "
                + "ON analytics.tall_canonical (template_version_uid, canonical_element_id)",
            "CREATE INDEX IF NOT EXISTS idx_tall_canonical_template_element "
                + "ON analytics.tall_canonical (template_uid, canonical_element_id)",
            "CREATE INDEX IF NOT EXISTS idx_tall_canonical_submission_uid ON analytics.tall_canonical (submission_uid)",
            "CREATE INDEX IF NOT EXISTS idx_tall_canonical_repeat_instance ON analytics.tall_canonical (repeat_instance_id)",
            "CREATE INDEX IF NOT EXISTS idx_tall_canonical_parent_instance ON analytics.tall_canonical (parent_instance_id)",
            "CREATE INDEX IF NOT EXISTS idx_tall_canonical_updated_at ON analytics.tall_canonical (updated_at)",
            "CREATE INDEX IF NOT EXISTS idx_tall_value_ref_uid ON analytics.tall_canonical (value_ref_uid)")) {
            db.execute(index);
        }
    }

    @Test
    void itDropsTheFiveUnusedIndexesOnceAndRollsBackToTheirDefinitions() throws Exception {
        Map<String, String> before = indexes();
        assertThat(before).hasSize(9);

        liquibase(false);
        assertThat(indexes()).containsOnlyKeys(KEPT);
        assertThat(indexes()).allSatisfy((name, definition) -> assertThat(definition).isEqualTo(before.get(name)));

        liquibase(false);
        assertThat(runs()).as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(indexes()).as("the rollback restores each definition").isEqualTo(before);
        assertThat(runs()).isEmpty();
    }

    /** A long read holds the table (Airbyte, a dump): the migration fails visibly instead of waiting behind it. */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void aLongReadMakesTheMigrationFailAfterItsLockTimeoutInsteadOfWaiting() throws Exception {
        try (Connection reader = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            reader.setAutoCommit(false);
            try (Statement statement = reader.createStatement()) {
                statement.execute("LOCK TABLE analytics.tall_canonical IN ACCESS SHARE MODE");
            }
            long start = System.nanoTime();
            assertThatThrownBy(() -> liquibase(false, "2s")).hasStackTraceContaining("lock timeout");
            assertThat((System.nanoTime() - start) / 1e9).isBetween(1.5, 20.0);
            reader.rollback();
        }

        assertThat(indexes()).as("nothing dropped").hasSize(9);
        assertThat(runs()).isEmpty();
    }

    private static void liquibase(boolean rollback) throws Exception {
        liquibase(rollback, null);
    }

    /** One Liquibase run, as a fresh process would make it (see OutboxUnfinishedIndexMigrationIT). */
    private static void liquibase(boolean rollback, String lockTimeout) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
            if (lockTimeout != null) {
                liquibase.setChangeLogParameter("tallIndexLockTimeout", lockTimeout);
            }
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    /** Each index of the table, by name, with its definition. */
    private static Map<String, String> indexes() {
        Map<String, String> indexes = new TreeMap<>();
        db.queryForList("""
                SELECT c.relname AS name, pg_get_indexdef(c.oid) AS definition
                FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
                WHERE i.indrelid = 'analytics.tall_canonical'::regclass""")
            .forEach(row -> indexes.put((String) row.get("name"), (String) row.get("definition")));
        return indexes;
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }
}
