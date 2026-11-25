package org.nmcpye.datarun.submission.feed;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The feed's schema changeset on its own, against PostgreSQL 16: the five streams exactly as contract version 1 types
 * them (names, types, nullability, keys), the shared position sequence with CACHE 1, the BRIN cursor index and
 * fillfactor headroom on both submission streams, an empty feed_ops, a rollback, and a rollback that refuses to drop
 * what someone else put in the schemas.
 */
class FeedSchemaMigrationIT {

    static final String CHANGELOG = "config/liquibase/changelog/feed/20261125-feed-schema.xml";
    static final String CHANGESET = "20261125-d01-feed-schema";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void startFromNothing() {
        db.execute("DROP SCHEMA IF EXISTS feed CASCADE");
        db.execute("DROP SCHEMA IF EXISTS feed_ops CASCADE");
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
    }

    /** One Liquibase run, as a fresh process would make it. */
    private static void liquibase(boolean rollback) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    /** Each column of a table as "name type nullable", sorted by name: the order of columns is not the contract. */
    private static List<String> columns(String table) {
        return db.queryForList("""
            SELECT column_name || ' ' || data_type || ' ' || is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'feed' AND table_name = ?
            ORDER BY column_name""", String.class, table);
    }

    private static String constraint(String name) {
        return db.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?",
            String.class, name);
    }

    @Test
    void theFiveStreamsAreTypedAsTheContractTypesThem() throws Exception {
        liquibase(false);

        assertThat(db.queryForList("""
                SELECT table_name FROM information_schema.tables WHERE table_schema = 'feed' ORDER BY table_name""",
            String.class))
            .containsExactly("form_field", "form_section", "freshness", "submission", "submission_row");
        assertThat(columns("submission")).containsExactly(
            "activity_id text NO",
            "assignment_id text NO",
            "changed_at timestamp with time zone NO",
            "created_at timestamp with time zone NO",
            "deleted boolean NO",
            "entered_end_at timestamp with time zone YES",
            "entered_start_at timestamp with time zone YES",
            "field_values jsonb NO",
            "form_id text NO",
            "form_version_id text YES",
            "id text NO",
            "org_unit_id text NO",
            "position bigint NO",
            "published_at timestamp with time zone NO",
            "team_id text NO",
            "unmapped_values jsonb YES",
            "version bigint NO");
        assertThat(columns("submission_row")).containsExactly(
            "changed_in_version bigint NO",
            "deleted boolean NO",
            "field_values jsonb NO",
            "id text NO",
            "id_origin text NO",
            "parent_row_id text YES",
            "position bigint NO",
            "published_at timestamp with time zone NO",
            "row_index integer NO",
            "section_id text NO",
            "submission_id text NO",
            "unmapped_values jsonb YES");
        assertThat(columns("form_field")).containsExactly(
            "field_id text NO",
            "form_id text NO",
            "form_version_id text NO",
            "form_version_number integer NO",
            "label jsonb YES",
            "mandatory boolean YES",
            "name text NO",
            "option_set_id text YES",
            "published_at timestamp with time zone NO",
            "repeat_section_id text YES",
            "section_id text YES",
            "sort_order integer YES",
            "value_type text YES");
        assertThat(columns("form_section")).containsExactly(
            "form_id text NO",
            "form_version_id text NO",
            "form_version_number integer NO",
            "label jsonb YES",
            "name text YES",
            "parent_section_id text YES",
            "published_at timestamp with time zone NO",
            "repeatable boolean NO",
            "section_id text NO",
            "sort_order integer YES");
        assertThat(columns("freshness")).containsExactly(
            "backlog bigint NO",
            "checked_at timestamp with time zone NO",
            "dead bigint NO",
            "feed text NO",
            "first_publication_completed_at timestamp with time zone YES",
            "last_changed_at timestamp with time zone YES",
            "last_position bigint YES",
            "last_published_at timestamp with time zone YES",
            "last_reconciled_at timestamp with time zone YES",
            "oldest_pending_at timestamp with time zone YES",
            "oldest_unpublished_at timestamp with time zone YES",
            "pending bigint NO",
            "unpublished bigint YES");
    }

    @Test
    void theKeysAreTheContracts() throws Exception {
        liquibase(false);

        assertThat(constraint("pk_feed_submission")).isEqualTo("PRIMARY KEY (id)");
        assertThat(constraint("pk_feed_submission_row")).isEqualTo("PRIMARY KEY (submission_id, id)");
        assertThat(constraint("uk_feed_form_field"))
            .isEqualTo("UNIQUE NULLS NOT DISTINCT (form_version_id, section_id, field_id)");
        assertThat(constraint("pk_feed_form_section")).isEqualTo("PRIMARY KEY (form_version_id, section_id)");
        assertThat(constraint("pk_feed_freshness")).isEqualTo("PRIMARY KEY (feed)");
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
                WHERE n.nspname IN ('feed', 'feed_ops')""", Long.class))
            .as("no other constraint, and no foreign key out of the schema").isEqualTo(5);

        String field = """
            INSERT INTO feed.form_field (form_id, form_version_id, form_version_number, field_id, section_id, name,
                                         published_at)
            VALUES ('f', 'v', 1, 'e', %s, 'n', now())""";
        db.update(field.formatted("NULL"));
        db.update(field.formatted("'s'"));
        assertThatThrownBy(() -> db.update(field.formatted("NULL")))
            .as("a field outside every section is placed once per version: nulls are not distinct")
            .hasStackTraceContaining("uk_feed_form_field");
    }

    @Test
    void bothSubmissionStreamsHaveABrinCursorAndFillfactorHeadroom() throws Exception {
        liquibase(false);

        for (String table : List.of("submission", "submission_row")) {
            assertThat(db.queryForList("""
                    SELECT pg_get_indexdef(i.indexrelid) FROM pg_index i
                    WHERE i.indrelid = ('feed.' || ?)::regclass ORDER BY 1""", String.class, table))
                .as("the key and the BRIN cursor, and no btree on position")
                .containsExactlyInAnyOrder(
                    table.equals("submission")
                        ? "CREATE UNIQUE INDEX pk_feed_submission ON feed.submission USING btree (id)"
                        : "CREATE UNIQUE INDEX pk_feed_submission_row ON feed.submission_row USING btree (submission_id, id)",
                    "CREATE INDEX idx_feed_" + table + "_position ON feed." + table
                        + " USING brin (\"position\") WITH (pages_per_range='4', autosummarize='on')");
            assertThat(db.queryForObject("SELECT reloptions::text FROM pg_class WHERE oid = ('feed.' || ?)::regclass",
                String.class, table)).isEqualTo("{fillfactor=80}");
        }
        for (String table : List.of("form_field", "form_section", "freshness")) {
            assertThat(db.queryForObject("SELECT reloptions FROM pg_class WHERE oid = ('feed.' || ?)::regclass",
                Object.class, table)).as(table).isNull();
        }
    }

    /**
     * The point of the BRIN cursor: rewriting a record with a new position touches no btree column, so the update is
     * HOT (heap-only) when its page has room.
     */
    @Test
    void aRewriteWithANewPositionIsAHotUpdate() throws Exception {
        liquibase(false);
        db.update("""
            INSERT INTO feed.submission (position, version, created_at, changed_at, published_at, deleted, id, form_id,
                                         assignment_id, team_id, org_unit_id, activity_id, field_values)
            VALUES (nextval('feed.position_seq'), 0, now(), now(), now(), false, 's1', 'f', 'a', 't', 'o', 'c',
                    '{}')""");
        db.update("""
            UPDATE feed.submission SET position = nextval('feed.position_seq'), version = 1, published_at = now(),
                                       field_values = '{"e": 1}'
            WHERE id = 's1'""");

        long hot = 0;
        for (int attempt = 0; attempt < 50 && hot == 0; attempt++) {
            // statistics reach the shared view after the writing session flushes them
            Long read = db.queryForObject("""
                SELECT n_tup_hot_upd FROM pg_stat_user_tables WHERE relid = 'feed.submission'::regclass""", Long.class);
            hot = read == null ? 0 : read;
            if (hot == 0) {
                Thread.sleep(100);
            }
        }
        assertThat(hot).isEqualTo(1);
        assertThat(db.queryForObject("SELECT position FROM feed.submission WHERE id = 's1'", Long.class)).isEqualTo(2);
    }

    @Test
    void thePositionSequenceIsSharedBigintAndCachesOneValue() throws Exception {
        liquibase(false);

        Map<String, Object> sequence = db.queryForMap("""
            SELECT data_type::text AS data_type, start_value, increment_by, cycle, cache_size
            FROM pg_sequences WHERE schemaname = 'feed' AND sequencename = 'position_seq'""");
        assertThat(sequence).containsEntry("data_type", "bigint").containsEntry("start_value", 1L)
            .containsEntry("increment_by", 1L).containsEntry("cycle", false).containsEntry("cache_size", 1L);
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'feed' AND c.relkind = 'S'""", Long.class))
            .as("one sequence for both submission streams").isEqualTo(1);
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'feed_ops'""", Long.class))
            .as("feed_ops starts empty: the publisher's bookkeeping comes with the publisher").isZero();
    }

    @Test
    void itRunsOnceAndRollsBack() throws Exception {
        liquibase(false);
        liquibase(false);
        assertThat(db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET))
            .as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_namespace WHERE nspname IN ('feed', 'feed_ops')""", Long.class)).isZero();
        assertThat(db.queryForList("SELECT id FROM databasechangelog", String.class)).isEmpty();

        liquibase(false);
        assertThat(db.queryForObject("SELECT to_regclass('feed.submission') IS NOT NULL", Boolean.class)).isTrue();
    }

    /** DROP SCHEMA without CASCADE: a rollback never takes anything else that was put in the schemas. */
    @Test
    void theRollbackRefusesWhenASchemaHoldsSomethingElse() throws Exception {
        liquibase(false);
        db.execute("CREATE TABLE feed_ops.someone_elses (id int)");

        assertThatThrownBy(() -> liquibase(true)).hasStackTraceContaining("depend");

        assertThat(db.queryForObject("SELECT to_regclass('feed.submission') IS NOT NULL", Boolean.class))
            .as("the refused rollback undid its own drops").isTrue();
        assertThat(db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET))
            .containsExactly("EXECUTED");
    }
}
