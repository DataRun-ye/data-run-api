package org.nmcpye.datarun.jpa.datasubmission.revision;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.FastCheckService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The submission change record's migrations against PostgreSQL, as production meets them: a fresh database is
 * migrated up to step C's first changeset, given submissions and outbox rows the way 6.5.4 leaves them, and then
 * migrated through step C. The tests then read what the migrations built and what the database refuses.
 */
class SubmissionRevisionMigrationIT {

    private static final String MASTER = "config/liquibase/master.xml";
    /** Every changeset of step C starts with this id. */
    static final String STEP_C = "20261124-c";
    static final String GAP_REPAIR = "20261124-c08-submission-revision-gap-repair";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;
    private static NamedParameterJdbcTemplate named;

    /** Submissions stored before step C, the way 6.5.4 leaves them. */
    private static String live;
    private static String changedThrice;
    private static String deleted;
    private static String neverSent;
    private static String withoutData;
    private static List<String> seeded;
    private static long outboxBeforeStepC;
    private static long outboxAfterStepC;
    private static long revisionsAfterStepC;
    private static Map<String, Long> integrityBeforeStepC;
    private static Map<String, Long> integrityAfterStepC;

    @BeforeAll
    static void migrateThroughStepC() throws Exception {
        PG.start();
        DriverManagerDataSource dataSource =
            new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        db = new JdbcTemplate(dataSource);
        named = new NamedParameterJdbcTemplate(dataSource);
        migrate(true);
        assertThat(db.queryForObject("SELECT to_regclass('journal.submission_revision') IS NULL", Boolean.class))
            .as("the first phase stops before step C").isTrue();

        live = submission(0, false);
        changedThrice = submission(3, false);
        deleted = submission(1, true);
        neverSent = submission(0, false);
        withoutData = submission(0, false);
        db.update("UPDATE data_submission SET form_data = NULL WHERE id = ?", withoutData);
        seeded = List.of(live, changedThrice, deleted, neverSent, withoutData);
        for (String id : List.of(live, changedThrice, deleted, withoutData)) {
            legacyOutboxRow(id, "SAVE");
        }
        outboxBeforeStepC = count("SELECT count(*) FROM outbox");
        integrityBeforeStepC = integrityCheck("01-outbox.sql", "11-revisions.sql");

        migrate(false);
        outboxAfterStepC = count("SELECT count(*) FROM outbox");
        revisionsAfterStepC = count("SELECT count(*) FROM journal.submission_revision");
        integrityAfterStepC = integrityCheck("01-outbox.sql", "11-revisions.sql");
    }

    /**
     * Runs files of {@code scripts/integrity/sql} with psql in the database's container, the way the integrity check
     * runs them: in a read-only transaction as {@code pg_read_all_data}. Returns each line's count by key.
     */
    static Map<String, Long> integrityCheck(String... files) throws Exception {
        StringBuilder script = new StringBuilder("""
            \\set ON_ERROR_STOP 1
            BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
            SET LOCAL work_mem = '8MB';
            SET LOCAL max_parallel_workers_per_gather = 0;
            SET LOCAL ROLE pg_read_all_data;
            """);
        for (String file : files) {
            PG.copyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(
                java.nio.file.Path.of("scripts", "integrity", "sql", file)), "/tmp/" + file);
            script.append("\\i /tmp/").append(file).append('\n');
        }
        script.append("ROLLBACK;\n");
        PG.copyFileToContainer(org.testcontainers.images.builder.Transferable.of(script.toString()), "/tmp/integrity.sql");
        var result = PG.execInContainer("psql", "-X", "-q", "-A", "-t", "-v", "ON_ERROR_STOP=1",
            "-U", PG.getUsername(), "-d", PG.getDatabaseName(), "-f", "/tmp/integrity.sql");
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        Map<String, Long> counts = new java.util.TreeMap<>();
        for (String line : result.getStdout().split("\n")) {
            String[] cells = line.split("\\|");
            if (cells.length >= 3 && cells[0].matches("\\d+[a-z]")) {
                counts.put(cells[0], Long.parseLong(cells[1]));
            }
        }
        return counts;
    }

    private static long count(String sql, Object... args) {
        Long n = db.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    /** An outbox row as 6.5.4 writes it: no version, the JVM's clock. */
    static void legacyOutboxRow(String submissionId, String eventType) {
        db.update("""
            INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload, event_type,
                                status, attempt, created_at)
            SELECT id, serial_number, uid, template_version_uid, form_data, ?, 'success', 0, localtimestamp
            FROM data_submission WHERE id = ?""", eventType, submissionId);
    }

    /** Submissions among {@code ids} whose head revision is not their live row: version, data and metadata. */
    static List<String> headsDifferingFromTheRow(List<String> ids) {
        return named.queryForList("""
            SELECT s.id FROM data_submission s
            LEFT JOIN LATERAL (SELECT r.version, r.form_data, r.metadata FROM journal.submission_revision r
                               WHERE r.submission_id = s.id ORDER BY r.version DESC LIMIT 1) h ON true
            WHERE s.id IN (:ids)
              AND (h.version IS DISTINCT FROM s.lock_version OR h.form_data IS DISTINCT FROM s.form_data
                   OR h.metadata IS DISTINCT FROM journal.submission_revision_metadata(s))""",
            Map.of("ids", ids), String.class);
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    /**
     * One Liquibase run of the whole changelog, as a fresh process makes it. With {@code beforeStepC}, only the
     * changesets that come before step C's first one.
     */
    static void migrate(boolean beforeStepC) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database);
            if (beforeStepC) {
                List<ChangeSet> unrun = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
                int first = 0;
                while (first < unrun.size() && !unrun.get(first).getId().startsWith(STEP_C)) {
                    first++;
                }
                assertThat(first).as("step C's changesets are in the changelog").isLessThan(unrun.size());
                liquibase.update(first, new Contexts(), new LabelExpression());
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    /** One Liquibase rollback of the last {@code changes} changesets run, as a fresh process makes it. */
    static void rollback(int changes) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database).rollback(changes, (String) null);
        }
    }

    /** A stored submission, as JPA writes it (UTC times), with a fresh id; returns the id. */
    static String submission(long lockVersion, boolean deleted) {
        String id = ulid();
        named.update("""
                INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no,
                                             assignment_uid, activity_uid, team_uid, team_code, org_unit_uid,
                                             org_unit_code, org_unit_name, status, start_entry_time,
                                             finished_entry_time, form_data, lock_version, created_by, created_date,
                                             last_modified_by, last_modified_date, deleted, deleted_at)
                VALUES (:id, :uid, 'migForm0001', 'migVers0001', 2, 'migAssign01', 'migActiv001', 'migTeam0001',
                        'T1', 'migOrgUn001', 'OU1', 'a village', 'COMPLETED',
                        TIMESTAMP '2026-11-20 05:00:00.25', TIMESTAMP '2026-11-20 05:10:00',
                        CAST('{"main": {"name": "first"}}' AS jsonb), :lockVersion, 'mig-test',
                        TIMESTAMP '2026-11-21 08:00:00.123456', 'mig-test', TIMESTAMP '2026-11-22 09:30:00.5',
                        :deleted, CASE WHEN :deleted THEN TIMESTAMP '2026-11-22 09:30:00.5' END)""",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("uid", id.substring(15))
                .addValue("lockVersion", lockVersion)
                .addValue("deleted", deleted));
        return id;
    }

    /** A user who can act, with a fresh id; returns the id. */
    static String user() {
        String id = ulid();
        named.update("""
                INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
                VALUES (:id, :uid, :login, 'x', true, 'en', 'mig-test')""",
            Map.of("id", id, "uid", id.substring(15), "login", "mig-" + id.toLowerCase()));
        return id;
    }

    static String ulid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase();
    }

    /** Inserts a revision whose state is the submission's row as it is now. */
    static void revision(String submissionId, long version, Long previousVersion, String kind, String command,
                         String actorUserId, String actorSystem, String onBehalfOf, String reason) {
        named.update("""
                INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command,
                                                 actor_user_id, actor_system, on_behalf_of_user_id, reason,
                                                 form_data, metadata)
                SELECT s.id, :version, :previousVersion, :kind, :command, :actorUserId, :actorSystem, :onBehalfOf,
                       :reason, s.form_data, journal.submission_revision_metadata(s)
                FROM data_submission s WHERE s.id = :id""",
            new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("version", version)
                .addValue("previousVersion", previousVersion)
                .addValue("kind", kind)
                .addValue("command", command)
                .addValue("actorUserId", actorUserId)
                .addValue("actorSystem", actorSystem)
                .addValue("onBehalfOf", onBehalfOf)
                .addValue("reason", reason));
    }

    /** The SQLSTATE and constraint the database refused {@code work} with. */
    static String refusal(Runnable work) {
        try {
            work.run();
        } catch (DataAccessException e) {
            Throwable cause = e;
            while (cause != null && !(cause instanceof SQLException)) {
                cause = cause.getCause();
            }
            assertThat(cause).as("a database refusal").isNotNull();
            SQLException refusal = (SQLException) cause;
            String constraint = refusal instanceof org.postgresql.util.PSQLException psql
                && psql.getServerErrorMessage() != null ? psql.getServerErrorMessage().getConstraint() : null;
            return refusal.getSQLState() + (constraint == null ? "" : " " + constraint);
        }
        throw new AssertionError("the database accepted it");
    }

    // ---------------------------------------------------------------- the baseline

    @Test
    void theBaselineRecordsEveryStoredSubmissionAtItsVersionAndWritesNoOutboxRow() {
        assertThat(revisionsAfterStepC).as("one baseline per submission stored before step C")
            .isEqualTo(seeded.size());
        assertThat(outboxAfterStepC).as("the migration writes no outbox row").isEqualTo(outboxBeforeStepC);

        List<String> baselines = named.queryForList("""
            SELECT s.lock_version || ' ' || r.version || ' ' || r.kind || ' ' || r.command || ' '
                   || r.actor_system || ' ' || coalesce(r.actor_user_id, '-') || ' '
                   || coalesce(r.previous_version::text, '-') || ' ' || coalesce(r.reason, '-')
            FROM data_submission s JOIN journal.submission_revision r ON r.submission_id = s.id
            WHERE s.id IN (:ids) AND r.command = 'MIGRATION' ORDER BY s.lock_version, s.id""",
            Map.of("ids", seeded), String.class);
        assertThat(baselines).containsExactly(
            "0 0 BASELINE MIGRATION MIGRATION - - -", "0 0 BASELINE MIGRATION MIGRATION - - -",
            "0 0 BASELINE MIGRATION MIGRATION - - -", "1 1 BASELINE MIGRATION MIGRATION - - -",
            "3 3 BASELINE MIGRATION MIGRATION - - -");
        assertThat(headsDifferingFromTheRow(seeded))
            .as("each head is its row, deleted ones, ones never sent and one without data included").isEmpty();
        assertThat(db.queryForObject("""
                SELECT r.metadata ->> 'deleted' FROM journal.submission_revision r WHERE r.submission_id = ?""",
            String.class, deleted)).isEqualTo("true");
    }

    /** The integrity check reads the journal once it exists, and reads -1 for it before. */
    @Test
    void theIntegrityCheckReadsTheJournal() {
        assertThat(integrityBeforeStepC)
            .containsEntry("11a", -1L).containsEntry("11d", -1L).containsEntry("11h", -1L)
            .as("check 1 runs on the outbox without a version column").containsEntry("1d", 0L)
            .containsEntry("1a", 1L);
        assertThat(integrityAfterStepC)
            .containsEntry("11a", 0L).containsEntry("11b", 0L).containsEntry("11c", 0L).containsEntry("11d", 0L)
            .containsEntry("11e", 0L).containsEntry("11f", 0L).containsEntry("11g", 0L)
            .containsEntry("11h", (long) seeded.size())
            .containsEntry("1d", 0L).containsEntry("1a", 1L);
    }

    /** If the changelog history were lost, the baseline runs again and repeats nothing. */
    @Test
    void theBaselineRunAgainRepeatsNothing() throws Exception {
        List<Map<String, Object>> before = db.queryForList("""
            SELECT submission_id, count(*) AS revisions FROM journal.submission_revision
            GROUP BY submission_id ORDER BY submission_id""");
        db.update("DELETE FROM databasechangelog WHERE id = '20261124-c04-submission-revision-baseline'");

        migrate(false);

        List<Map<String, Object>> after = db.queryForList("""
            SELECT submission_id, count(*) AS revisions FROM journal.submission_revision
            GROUP BY submission_id ORDER BY submission_id""");
        assertThat(after).as("a submission that had revisions gets no other").containsAll(before);
        assertThat(count("SELECT count(*) FROM databasechangelog WHERE id = '20261124-c04-submission-revision-baseline'"))
            .isEqualTo(1);
    }

    /**
     * Production runs 6.5.4 again for a while after step C: its uploads raise versions without revisions and its new
     * submissions have none. The next start of step C records each as an explicit gap.
     */
    @Test
    void aWindowUnder654IsRecordedAsGapsAtTheNextStart() throws Exception {
        String gapped = submission(3, false);
        revision(gapped, 3, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);
        String untouched = submission(2, false);
        revision(untouched, 2, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);

        // 6.5.4: two changed re-sends of one submission, and a new one; its outbox rows carry no version.
        db.update("""
            UPDATE data_submission SET lock_version = 5, form_data = CAST('{"main": {"name": "changed"}}' AS jsonb),
                                       last_modified_date = TIMESTAMP '2026-12-01 10:00:00'
            WHERE id = ?""", gapped);
        legacyOutboxRow(gapped, "UPDATE");
        String createdIn654 = submission(0, false);
        legacyOutboxRow(createdIn654, "SAVE");

        migrate(false);

        assertThat(named.query("""
                SELECT s.id = :gapped AS gapped, r.version || ' ' || coalesce(r.previous_version::text, '-') || ' '
                       || r.kind || ' ' || r.command || ' ' || r.actor_system
                FROM journal.submission_revision r JOIN data_submission s ON s.id = r.submission_id
                WHERE s.id IN (:ids) AND r.command = 'GAP_REPAIR' ORDER BY 1""",
            Map.of("gapped", gapped, "ids", List.of(gapped, untouched, createdIn654)),
            (rs, i) -> rs.getString(2)))
            .containsExactly("0 - BASELINE GAP_REPAIR MIGRATION", "5 3 BASELINE GAP_REPAIR MIGRATION");
        assertThat(headsDifferingFromTheRow(List.of(gapped, untouched, createdIn654))).isEmpty();
        assertThat(count("SELECT count(*) FROM journal.submission_revision WHERE submission_id = ?", untouched)).isEqualTo(1);

        migrate(false);
        assertThat(count("SELECT count(*) FROM journal.submission_revision WHERE submission_id IN (?, ?, ?)",
            gapped, untouched, createdIn654)).as("the next start finds no gap").isEqualTo(4);
        assertThat(count("SELECT outbox_id FROM journal.outbox_watermark"))
            .as("the watermark moved past 6.5.4's rows").isEqualTo(count("SELECT max(outbox_id) FROM outbox"));
    }

    /**
     * The gap repair reads every submission only on a cue: an outbox row without a version above the watermark (a
     * release without the journal wrote), or a changeset that ran since it last did. A version raised by SQL outside
     * a deploy gives neither: the next start records nothing, check 11b's reconciliation reports the submission, and
     * the next cue records it.
     */
    @Test
    void withoutACueTheGapRepairReadsNothingAndCheck11bReportsTheGap() throws Exception {
        migrate(false);
        String raisedBySql = submission(1, false);
        revision(raisedBySql, 1, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);
        db.update("UPDATE data_submission SET lock_version = 2 WHERE id = ?", raisedBySql);

        migrate(false);

        assertThat(count("SELECT count(*) FROM journal.submission_revision WHERE submission_id = ? AND command = 'GAP_REPAIR'",
            raisedBySql)).as("no cue, no gap revision").isZero();
        assertThat(JournalReconciliation.findings(named, "s.id = :id", Map.of("id", raisedBySql)))
            .containsExactly(raisedBySql.substring(15) + ": head revision at 1, row at 2");

        String written654 = submission(0, false);
        legacyOutboxRow(written654, "SAVE");
        migrate(false);

        assertThat(named.queryForList("""
                SELECT r.version || ' ' || coalesce(r.previous_version::text, '-')
                FROM journal.submission_revision r
                WHERE r.submission_id IN (:raised, :written) AND r.command = 'GAP_REPAIR'
                ORDER BY r.submission_id = :raised""",
            Map.of("raised", raisedBySql, "written", written654), String.class))
            .as("a 6.5.4 row is the cue: the repair reads every submission and records both")
            .containsExactly("0 -", "2 1");
        assertThat(JournalReconciliation.findings(named, "s.id IN (:ids)", Map.of("ids", List.of(raisedBySql, written654))))
            .as("both heads are their rows; only 6.5.4's own outbox row, without a version, is reported")
            .containsExactly(written654.substring(15) + ": 1 outbox rows without a version");
    }

    /**
     * A rollback to 6.5.4 keeps the migrated schema: its changelog, which is this one without step C's includes,
     * runs on it without error and changes nothing, and the outbox rows 6.5.4 writes (no version) are accepted.
     */
    @Test
    void theReleaseBeforeStepCRunsItsChangelogOnTheMigratedSchema() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("changelog-654");
        try {
            String master = new String(SubmissionRevisionMigrationIT.class.getClassLoader()
                .getResourceAsStream(MASTER).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String before = master.lines()
                .filter(line -> !line.contains("20261124-submission-revision") && !line.contains("20261124-outbox-")
                    && !line.contains("Runs on every start"))
                .collect(java.util.stream.Collectors.joining("\n"));
            assertThat(before).doesNotContain("20261124-c").contains("20261124-drop-duplicate-events-indexes.xml");
            java.nio.file.Files.writeString(dir.resolve("master-6.5.4.xml"), before);
            long ranBefore = count("SELECT count(*) FROM databasechangelog");
            long revisionsBefore = count("SELECT count(*) FROM journal.submission_revision");

            ChangeLogHistoryServiceFactory.getInstance().resetAll();
            Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
            try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
                Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                Liquibase liquibase = new Liquibase("master-6.5.4.xml", new liquibase.resource.CompositeResourceAccessor(
                    new liquibase.resource.DirectoryResourceAccessor(dir), new ClassLoaderResourceAccessor()), database);
                assertThat(liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression()))
                    .as("6.5.4 finds nothing to run").isEmpty();
                liquibase.update(new Contexts(), new LabelExpression());
            }

            assertThat(count("SELECT count(*) FROM databasechangelog")).isEqualTo(ranBefore);
            assertThat(count("SELECT count(*) FROM journal.submission_revision")).isEqualTo(revisionsBefore);
            String id = submission(0, false);
            legacyOutboxRow(id, "SAVE");
            assertThat(count("SELECT count(*) FROM outbox WHERE submission_id = ? AND submission_version IS NULL", id))
                .as("6.5.4's outbox row, without a version").isEqualTo(1);
        } finally {
            try (var files = java.nio.file.Files.list(dir)) {
                for (java.nio.file.Path file : files.toList()) {
                    java.nio.file.Files.delete(file);
                }
            }
            java.nio.file.Files.delete(dir);
        }
    }

    /**
     * Once the application wrote a revision, a Liquibase rollback of step C refuses before it removes anything: the
     * journal, its triggers and the outbox's key all stay. The gap repair, which runs at every start and so is always
     * the latest changeset run, is rolled back first; it removes nothing, and the next start runs it again.
     */
    @Test
    void aRollbackOfStepCRefusesWithEveryGuardInPlace() throws Exception {
        String id = submission(0, false);
        revision(id, 0, null, "CREATE", "UPLOAD", user(), null, null, null);
        migrate(false);
        assertThat(db.queryForObject("SELECT id FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 1",
            String.class)).as("the gap repair is the latest changeset run").isEqualTo(GAP_REPAIR);
        long revisions = count("SELECT count(*) FROM journal.submission_revision");
        int stepC = (int) count("SELECT count(*) FROM databasechangelog WHERE id LIKE '" + STEP_C + "%'");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rollback(stepC))
            .hasStackTraceContaining("never roll it back");

        assertThat(count("""
            SELECT count(*) FROM pg_trigger WHERE tgrelid = 'journal.submission_revision'::regclass AND NOT tgisinternal"""))
            .as("the append-only, truncate and no-gap triggers").isEqualTo(3);
        assertThat(count("SELECT count(*) FROM pg_constraint WHERE conname = 'fk_outbox_submission_revision'"))
            .as("the outbox's key").isEqualTo(1);
        assertThat(count("SELECT count(*) FROM journal.submission_revision")).isEqualTo(revisions);
        assertThat(db.queryForList("SELECT id FROM databasechangelog WHERE id LIKE '" + STEP_C + "%' ORDER BY id",
            String.class)).as("only the gap repair was rolled back").hasSize(stepC - 1).doesNotContain(GAP_REPAIR);

        migrate(false);
        assertThat(count("SELECT count(*) FROM databasechangelog WHERE id = ?", GAP_REPAIR))
            .as("the next start runs the gap repair again").isEqualTo(1);
    }

    /**
     * The gap repair runs after every other changeset, by construction: it is last in this changelog, and stays last
     * in a changelog that adds a changeset after it. Such a later changeset, a data repair that raised a version
     * without a revision, is recorded as a gap in the same Liquibase run.
     */
    @Test
    void theGapRepairRunsLastEvenAfterChangesetsAddedLater() throws Exception {
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            List<ChangeSet> all = new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database)
                .getDatabaseChangeLog().getChangeSets();
            assertThat(all.get(all.size() - 1).getId()).isEqualTo(GAP_REPAIR);
        }

        String repaired = submission(2, false);
        revision(repaired, 2, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("changelog-later");
        String laterChangeset = "later-data-repair-" + repaired;
        try {
            java.nio.file.Files.writeString(dir.resolve("later.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">
                    <include file="config/liquibase/master.xml" relativeToChangelogFile="false"/>
                    <changeSet id="%s" author="test">
                        <sql>UPDATE data_submission SET lock_version = lock_version + 1 WHERE id = '%s'</sql>
                    </changeSet>
                </databaseChangeLog>""".formatted(laterChangeset, repaired));
            ChangeLogHistoryServiceFactory.getInstance().resetAll();
            Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
            try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
                Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                Liquibase later = new Liquibase("later.xml", new liquibase.resource.CompositeResourceAccessor(
                    new liquibase.resource.DirectoryResourceAccessor(dir), new ClassLoaderResourceAccessor()), database);
                List<ChangeSet> order = later.getDatabaseChangeLog().getChangeSets();
                assertThat(order.get(order.size() - 1).getId()).as("still last").isEqualTo(GAP_REPAIR);
                assertThat(order.get(order.size() - 2).getId()).isEqualTo(laterChangeset);
                later.update(new Contexts(), new LabelExpression());
            }
        } finally {
            try (var files = java.nio.file.Files.list(dir)) {
                for (java.nio.file.Path file : files.toList()) {
                    java.nio.file.Files.delete(file);
                }
            }
            java.nio.file.Files.delete(dir);
        }

        assertThat(db.queryForList("""
                SELECT version || ' ' || coalesce(previous_version::text, '-') || ' ' || command
                FROM journal.submission_revision WHERE submission_id = ? ORDER BY version""", String.class, repaired))
            .as("the later changeset's raised version, recorded in the same run")
            .containsExactly("2 - MIGRATION", "3 2 GAP_REPAIR");
    }

    // ---------------------------------------------------------------- the table and its constraints

    @Test
    void theMetadataIsWhatAWriteCanChange() {
        String id = submission(3, true);

        Map<String, Object> metadata = db.queryForMap("""
            SELECT m ->> 'template_version_uid' AS template_version_uid,
                   m ->> 'template_version_no' AS template_version_no, m ->> 'org_unit_name' AS org_unit_name,
                   m ->> 'status' AS status, m ->> 'deleted' AS deleted, m ->> 'deleted_at' AS deleted_at,
                   m ->> 'last_modified_date' AS last_modified_date,
                   (SELECT array_agg(k ORDER BY k) FROM jsonb_object_keys(m) AS k)::text AS keys
            FROM (SELECT journal.submission_revision_metadata(s) AS m FROM data_submission s WHERE s.id = ?) x""", id);

        assertThat(metadata)
            .containsEntry("template_version_uid", "migVers0001")
            .containsEntry("template_version_no", "2")
            .containsEntry("org_unit_name", "a village")
            .containsEntry("status", "COMPLETED")
            .containsEntry("deleted", "true")
            .containsEntry("deleted_at", "2026-11-22T09:30:00.500000Z")
            .containsEntry("last_modified_date", "2026-11-22T09:30:00.500000Z")
            .containsEntry("keys", "{activity_uid,assignment_uid,deleted,deleted_at,last_modified_date,org_unit_code,"
                + "org_unit_name,org_unit_uid,status,team_code,team_uid,template_version_no,template_version_uid}");
    }

    /** The rendering of times does not depend on the session's time zone. */
    @Test
    void theMetadataIsTheSameInEverySessionTimeZone() throws Exception {
        String id = submission(0, false);
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("SET TimeZone = 'Asia/Aden'");
            var aden = statement.executeQuery(
                "SELECT journal.submission_revision_metadata(s)::text FROM data_submission s WHERE s.id = '" + id + "'");
            aden.next();
            String inAden = aden.getString(1);
            statement.execute("SET TimeZone = 'UTC'");
            var utc = statement.executeQuery(
                "SELECT journal.submission_revision_metadata(s)::text FROM data_submission s WHERE s.id = '" + id + "'");
            utc.next();
            assertThat(inAden).isEqualTo(utc.getString(1))
                .contains("\"last_modified_date\": \"2026-11-22T09:30:00.500000Z\"");
        }
    }

    @Test
    void aRevisionOfEachKindIsAcceptedWithItsActor() {
        String admin = user();
        String uploader = user();
        String created = submission(0, false);
        String baselined = submission(4, false);

        revision(created, 0, null, "CREATE", "HELD_RELEASE", admin, null, uploader, "area went live");
        revision(created, 1, 0L, "UPDATE", "UPLOAD", uploader, null, null, null);
        revision(created, 2, 1L, "DELETE", "ADMIN_DELETE", admin, null, null, null);
        revision(baselined, 4, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);
        revision(baselined, 7, 4L, "BASELINE", "GAP_REPAIR", null, "MIGRATION", null, null);

        assertThat(db.queryForList("""
                SELECT version || ' ' || kind || ' ' || command || ' ' || coalesce(previous_version::text, '-')
                FROM journal.submission_revision WHERE submission_id IN (?, ?) ORDER BY submission_id = ?, version""",
            String.class, created, baselined, baselined))
            .containsExactly("0 CREATE HELD_RELEASE -", "1 UPDATE UPLOAD 0", "2 DELETE ADMIN_DELETE 1",
                "4 BASELINE MIGRATION -", "7 BASELINE GAP_REPAIR 4");
        assertThat(db.queryForObject("""
                SELECT recorded_at BETWEEN now() - interval '1 minute' AND now()
                FROM journal.submission_revision WHERE submission_id = ? AND version = 0""", Boolean.class, created))
            .as("recorded_at is the database's time").isTrue();
    }

    /**
     * Two writers of one submission that both read the same head: the no-gap check passes for both, and the
     * primary key refuses the second once the first commits.
     */
    @Test
    void aVersionIsRecordedOnceEvenByConcurrentWriters() throws Exception {
        String uploader = user();
        String id = submission(1, false);
        revision(id, 0, null, "CREATE", "UPLOAD", uploader, null, null, null);
        String insert = """
            INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command, actor_user_id,
                                             metadata)
            VALUES ('%s', 1, 0, 'UPDATE', 'UPLOAD', '%s', '{}')""".formatted(id, uploader);
        try (Connection first = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             Connection second = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            first.createStatement().execute(insert);
            var blocked = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    second.createStatement().execute(insert);
                    return "accepted";
                } catch (SQLException e) {
                    return e.getSQLState() + " " + ((org.postgresql.util.PSQLException) e).getServerErrorMessage()
                        .getConstraint();
                }
            });
            Thread.sleep(500);
            assertThat(blocked).as("the second waits for the first").isNotDone();
            first.commit();
            assertThat(blocked.get(30, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("23505 pk_submission_revision");
            second.rollback();
        }
    }

    @Test
    void aRevisionNamesAStoredSubmissionAndKnownUsers() {
        String uploader = user();
        String id = submission(0, false);

        assertThat(refusal(() -> named.update("""
                INSERT INTO journal.submission_revision (submission_id, version, kind, command, actor_user_id, metadata)
                VALUES ('01NOSUCHSUBMISSION00000000', 0, 'CREATE', 'UPLOAD', :actor, '{}')""",
            Map.of("actor", uploader))))
            .isEqualTo("23503 fk_submission_revision_submission");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", "01NOSUCHUSER00000000000000", null, null,
            null)))
            .isEqualTo("23503 fk_submission_revision_actor");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "HELD_RELEASE", uploader, null,
            "01NOSUCHUSER00000000000000", "note")))
            .isEqualTo("23503 fk_submission_revision_on_behalf_of");
    }

    @Test
    void everyRevisionHasExactlyOneActorOfTheRightKind() {
        String user = user();
        String id = submission(0, false);

        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", null, null, null, null)))
            .as("no actor").isEqualTo("23514 ck_submission_revision_actor");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", user, "MIGRATION", null, null)))
            .as("two actors").isEqualTo("23514 ck_submission_revision_actor");
        assertThat(refusal(() -> revision(id, 0, null, "BASELINE", "MIGRATION", user, null, null, null)))
            .as("a user on a system command").isEqualTo("23514 ck_submission_revision_system");
        assertThat(refusal(() -> revision(id, 0, null, "BASELINE", "MIGRATION", null, "SCRIPT", null, null)))
            .as("an unknown system").isEqualTo("23514 ck_submission_revision_system");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "HELD_RELEASE", user, null, null, null)))
            .as("a release without its uploader").isEqualTo("23514 ck_submission_revision_on_behalf_of");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", user, null, user, null)))
            .as("on behalf of, outside a release").isEqualTo("23514 ck_submission_revision_on_behalf_of");
    }

    @Test
    void eachKindTakesItsOwnVersionsAndCommands() {
        String user = user();
        String first = submission(5, false);
        String headed = submission(5, false);
        revision(headed, 4, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);

        assertThat(refusal(() -> revision(first, 1, null, "CREATE", "UPLOAD", user, null, null, null)))
            .as("a creation above 0").isEqualTo("23514 ck_submission_revision_version");
        assertThat(refusal(() -> revision(first, 5, null, "BASELINE", "UPLOAD", user, null, null, null)))
            .as("a baseline comes from a system command").isEqualTo("23514 ck_submission_revision_baseline");
        assertThat(refusal(() -> revision(headed, 6, 4L, "UPDATE", "UPLOAD", user, null, null, null)))
            .as("an update that skips a version").isEqualTo("23514 ck_submission_revision_version");
        assertThat(refusal(() -> revision(headed, 4, 4L, "BASELINE", "GAP_REPAIR", null, "MIGRATION", null, null)))
            .as("a baseline that does not move forward").isEqualTo("23514 ck_submission_revision_version");
        assertThat(refusal(() -> revision(headed, 5, 4L, "UPDATE", "MIGRATION", null, "MIGRATION", null, null)))
            .as("a system command writes only baselines").isEqualTo("23514 ck_submission_revision_baseline");
        assertThat(refusal(() -> revision(headed, 5, 4L, "RESTORE", "UPLOAD", user, null, null, null)))
            .as("an unknown kind").isEqualTo("23514 ck_submission_revision_kind");
    }

    /**
     * An UPDATE or DELETE names the version it follows. Without one, the no-gap trigger sees no head to compare with
     * on a submission the journal never recorded, so only the version check stands between such a row and a first
     * revision.
     */
    @Test
    void anUpdateOrDeleteWithoutItsPreviousVersionIsRefused() {
        String user = user();
        String unrecorded = submission(5, false);

        assertThat(refusal(() -> revision(unrecorded, 5, null, "UPDATE", "UPLOAD", user, null, null, null)))
            .as("an update as a first revision").isEqualTo("23514 ck_submission_revision_version");
        assertThat(refusal(() -> revision(unrecorded, 5, null, "DELETE", "ADMIN_DELETE", user, null, null, null)))
            .as("a delete as a first revision").isEqualTo("23514 ck_submission_revision_version");
        assertThat(count("SELECT count(*) FROM journal.submission_revision WHERE submission_id = ?", unrecorded)).isZero();
    }

    // ---------------------------------------------------------------- the guards

    @Test
    void theJournalIsNeverChanged() {
        String uploader = user();
        String id = submission(0, false);
        revision(id, 0, null, "CREATE", "UPLOAD", uploader, null, null, null);

        assertThat(refusal(() -> db.update("UPDATE journal.submission_revision SET reason = 'edited' WHERE submission_id = ?", id)))
            .isEqualTo("23000 submission_revision_append_only");
        assertThat(refusal(() -> db.update("DELETE FROM journal.submission_revision WHERE submission_id = ?", id)))
            .isEqualTo("23000 submission_revision_append_only");
        assertThat(refusal(() -> db.execute("TRUNCATE journal.submission_revision")))
            .as("the outbox's key refuses it first").isEqualTo("0A000");
        assertThat(refusal(() -> db.execute("TRUNCATE journal.submission_revision CASCADE")))
            .isEqualTo("23000 submission_revision_append_only");
        assertThat(count("SELECT count(*) FROM journal.submission_revision WHERE submission_id = ? AND reason IS NULL", id))
            .isEqualTo(1);
    }

    @Test
    void versionsRunWithoutGaps() {
        String user = user();
        String created = submission(0, false);
        revision(created, 0, null, "CREATE", "UPLOAD", user, null, null, null);
        String bare = submission(3, false);
        String baselined = submission(3, false);
        revision(baselined, 3, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);

        assertThat(refusal(() -> revision(created, 2, 1L, "UPDATE", "UPLOAD", user, null, null, null)))
            .as("an update past a missing version").isEqualTo("23000 submission_revision_no_gap");
        assertThat(refusal(() -> revision(bare, 3, 2L, "UPDATE", "UPLOAD", user, null, null, null)))
            .as("an update of a submission the journal never saw").isEqualTo("23000 submission_revision_no_gap");
        assertThat(refusal(() -> revision(baselined, 0, null, "CREATE", "UPLOAD", user, null, null, null)))
            .as("a creation after a baseline").isEqualTo("23000 submission_revision_no_gap");
        assertThat(refusal(() -> revision(baselined, 6, null, "BASELINE", "GAP_REPAIR", null, "MIGRATION", null, null)))
            .as("a gap repair that does not name the head it jumps from").isEqualTo("23000 submission_revision_no_gap");

        revision(created, 1, 0L, "UPDATE", "UPLOAD", user, null, null, null);
        revision(baselined, 4, 3L, "DELETE", "ADMIN_DELETE", user, null, null, null);
        revision(baselined, 6, 4L, "BASELINE", "GAP_REPAIR", null, "MIGRATION", null, null);
        assertThat(count("SELECT count(*) FROM journal.submission_revision WHERE submission_id IN (?, ?)", created, baselined))
            .isEqualTo(5);
    }

    @Test
    void theOutboxAnnouncesOnlyRevisionsThatExist() {
        String user = user();
        String id = submission(0, false);
        revision(id, 0, null, "CREATE", "UPLOAD", user, null, null, null);
        String insert = """
            INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload, event_type,
                                status, attempt, submission_version)
            SELECT id, serial_number, uid, template_version_uid, form_data, 'SAVE', 'pending', 0, ?
            FROM data_submission WHERE id = ?""";

        assertThat(refusal(() -> db.update(insert, 1L, id))).isEqualTo("23503 fk_outbox_submission_revision");
        db.update(insert, 0L, id);
        db.update(insert, null, id);
        assertThat(count("SELECT count(*) FROM outbox WHERE submission_id = ?", id))
            .as("the revision's row, and one without a version as 6.5.4 writes").isEqualTo(2);
    }

    // ---------------------------------------------------------------- the outbox

    @Test
    void theOutboxNamesAVersionAndTakesItsTimeFromTheDatabaseInUtc() throws Exception {
        String id = submission(0, false);
        revision(id, 0, null, "CREATE", "UPLOAD", user(), null, null, null);
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("SET TimeZone = 'Asia/Aden'");
            statement.execute("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, submission_version)
                SELECT id, serial_number, uid, template_version_uid, form_data, 'SAVE', 'pending', 0, lock_version
                FROM data_submission WHERE id = '%s'""".formatted(id));
        }
        Map<String, Object> row = db.queryForMap("""
            SELECT submission_version,
                   abs(extract(epoch FROM (timezone('UTC', now()) - created_at))) < 60 AS utc
            FROM outbox WHERE submission_id = ?""", id);
        assertThat(row).containsEntry("submission_version", 0L).containsEntry("utc", true);
        assertThat(db.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'outbox' AND column_name = 'submission_version'""", String.class))
            .as("rows written before step C keep NULL").isEqualTo("YES");
    }
}
