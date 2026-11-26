package org.nmcpye.datarun.submission.feed.agreement;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.nmcpye.datarun.submission.normalized.FieldValue;
import org.nmcpye.datarun.submission.normalized.NormalizedRow;
import org.nmcpye.datarun.submission.normalized.NormalizedSubmission;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;
import org.nmcpye.datarun.submission.normalized.UnmappedReason;
import org.nmcpye.datarun.submission.normalized.UnmappedValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * The agreement harness proven on a database migrated by the whole changelog and seeded with heads of every shape
 * the harness must handle: rows with stored, empty and missing ids, nested rows, exact numbers, empty arrays, JSON
 * nulls and structure keys, a head newer than its first revision, values of another version and of a deleted one, a
 * document that is not an object, none at all, a row id used twice, one field stored at two paths (the pinned
 * version's value kept, else the first in walk order), a form with no definition, and a submission with no head. The
 * real normalizer agrees with the oracle on all of them, mapped values by field id and unmapped ones by path; a value
 * under the wrong field, a wrongly unmapped value and other faults are caught and named by form; what the normalizer
 * keeps whole is its own category. Ids and values are invented.
 */
class FeedAgreementIT {

    private static final String MASTER = "config/liquibase/master.xml";

    private static final String FORM_A = "agFormA0001";
    private static final String FORM_A_ID = "01JAGFM0000000000000000001";
    private static final String FORM_NONE = "agFormNone1";
    private static final String FORM_KEEP = "agFormKeep1";
    private static final String FORM_KEEP_ID = "01JAGFM0000000000000000003";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;
    private static NamedParameterJdbcTemplate named;
    private static long revisions;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    static void migrateAndSeed() throws Exception {
        PG.start();
        DriverManagerDataSource dataSource =
            new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        db = new JdbcTemplate(dataSource);
        named = new NamedParameterJdbcTemplate(dataSource);
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database).update(new Contexts(), new LabelExpression());
        }
        seed();
        revisions = db.queryForObject("SELECT count(*) FROM journal.submission_revision", Long.class);
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    private static void seed() {
        String user = "01JAGUS0000000000000000001";
        db.update("""
            INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
            VALUES (?, 'agUser00001', 'ag-user', 'x', true, 'en', 'ag-test')""", user);
        dataElement("01JAGDE0000000000000000001", "agDeTitle01", "Text");
        dataElement("01JAGDE0000000000000000002", "agDeCount01", "Integer");
        dataElement("01JAGDE0000000000000000003", "agDeAmount1", "Number");
        dataElement("01JAGDE0000000000000000004", "agDeChoice1", "SelectMulti");
        dataElement("01JAGDE0000000000000000005", "agDeOld0001", "Text");
        dataElement("01JAGDE0000000000000000006", "agDeAge0001", "Integer");
        dataElement("01JAGDE0000000000000000007", "agDeGeo0001", "Text");

        form(FORM_A, FORM_A_ID);
        String sections = """
            [{"name": "visit", "path": "visit"},
             {"name": "households", "path": "visit.households", "repeatable": true},
             {"name": "members", "path": "visit.households.members", "repeatable": true}]""";
        version(FORM_A, FORM_A_ID, "01JAGFV0000000000000000011", "agFormAVer1", 1, sections, """
            [{"id": "agDeTitle01", "name": "title", "path": "agDeTitle01", "type": "Text"},
             {"id": "agDeTitle01", "name": "headline", "path": "visit.agDeTitle01", "type": "Text"},
             {"id": "agDeOld0001", "name": "old_note", "path": "visit.agDeOld0001", "type": "Text"},
             {"id": "agDeCount01", "name": "count", "path": "visit.households.agDeCount01", "type": "Integer"},
             {"id": "agDeAge0001", "name": "age", "path": "visit.households.members.agDeAge0001", "type": "Integer"}]""");
        version(FORM_A, FORM_A_ID, "01JAGFV0000000000000000012", "agFormAVer2", 2, sections, """
            [{"id": "agDeTitle01", "name": "title", "path": "agDeTitle01", "type": "Text"},
             {"id": "agDeTitle01", "name": "visit-alt", "path": "agDeTitle01", "type": "Text"},
             {"id": "agDeAmount1", "name": "amount", "path": "visit.agDeAmount1", "type": "Number"},
             {"id": "agDeCount01", "name": "count", "path": "visit.households.agDeCount01", "type": "Integer"},
             {"id": "agDeChoice1", "name": "choice", "path": "visit.households.agDeChoice1", "type": "SelectMulti"},
             {"id": "agDeAge0001", "name": "age", "path": "visit.households.members.agDeAge0001", "type": "Integer"}]""");
        form(FORM_KEEP, FORM_KEEP_ID);
        version(FORM_KEEP, FORM_KEEP_ID, "01JAGFV0000000000000000031", "agFormKVer1", 1, "[]", """
            [{"id": "agDeTitle01", "name": "title", "path": "agDeTitle01", "type": "Text"},
             {"id": "agDeGeo0001", "name": "geo", "path": "agDeGeo0001", "type": "Text"}]""");

        // rows with a stored id, a missing one and an empty one; nested rows; zero rows; an exact decimal
        baseline("01JAGSB0000000000000000001", FORM_A, "agFormAVer2", false, """
            {"_status": "COMPLETED", "title": "T1", "visit": {"amount": 1.50, "households": [
              {"_id": "01JAGRW0000000000000000001", "_index": 1, "count": 3, "choice": ["a", "b"],
               "members": [{"age": 30}, {"_id": "", "age": 2}]},
              {"count": 0, "members": []}]}}""");
        // a head newer than the submission's first revision, a value at a path no version defines, a large decimal
        String newer = submission("01JAGSB0000000000000000002", FORM_A, "agFormAVer2", false, "{\"title\": \"old\"}");
        revision(newer, 0, null, "CREATE", "UPLOAD", user);
        db.update("""
            UPDATE data_submission SET lock_version = 1, form_data = CAST(? AS jsonb) WHERE id = ?""", """
            {"title": "new", "visit": {"amount": 12345678901234567890.123, "unknown_key": "x", "households": []}}""",
            newer);
        revision(newer, 1, 0L, "UPDATE", "UPLOAD", user);
        // pinned to version 1: one value of its own, one only version 2 defines
        baseline("01JAGSB0000000000000000003", FORM_A, "agFormAVer1", false,
            "{\"visit\": {\"old_note\": \"n\", \"amount\": 2}}");
        // pinned to a version that is gone
        baseline("01JAGSB0000000000000000004", FORM_A, "agGoneVer01", false,
            "{\"title\": \"T4\", \"visit\": {\"amount\": 4}}");
        // a document that is not an object, and none at all
        baseline("01JAGSB0000000000000000005", FORM_A, "agFormAVer2", false, "[1, 2]");
        baseline("01JAGSB0000000000000000006", FORM_A, "agFormAVer2", false, null);
        // one row id used twice
        baseline("01JAGSB0000000000000000007", FORM_A, "agFormAVer2", false, """
            {"visit": {"households": [{"_id": "01JAGRW0000000000000000007", "count": 1},
                                      {"_id": "01JAGRW0000000000000000007", "count": 2}]}}""");
        // JSON nulls, structure keys, an empty multi-select
        baseline("01JAGSB0000000000000000008", FORM_A, "agFormAVer2", false, """
            {"title": null, "visit": {"households": [{"_id": "01JAGRW0000000000000000008", "_parentId": "p",
              "_submissionUid": "s", "_index": 1, "count": null, "choice": []}]}}""");
        baseline("01JAGSB0000000000000000009", FORM_A, "agFormAVer2", true, "{\"title\": \"T9\"}");
        // a submission no revision records yet
        submission("01JAGSB0000000000000000010", FORM_A, "agFormAVer2", false, "{\"title\": \"T10\"}");
        // one field at two paths of one container: the pinned version's value is kept, the other unmapped
        baseline("01JAGSB0000000000000000013", FORM_A, "agFormAVer2", false,
            "{\"title\": \"a\", \"visit\": {\"headline\": \"b\"}}");
        // ... and with no pinned version, the first in walk order
        baseline("01JAGSB0000000000000000014", FORM_A, "agGoneVer02", false,
            "{\"title\": \"c\", \"visit\": {\"headline\": \"d\"}}");
        // walk order compares key by key: visit before visit-alt, though "visit-alt" sorts before "visit.headline"
        baseline("01JAGSB0000000000000000015", FORM_A, "agGoneVer02", false,
            "{\"visit\": {\"headline\": \"e\"}, \"visit-alt\": \"f\"}");
        // a form with no stored definition: every value unmapped at its path
        baseline("01JAGSB0000000000000000011", FORM_NONE, "agNoneVer01", false,
            "{\"title\": \"x\", \"group\": {\"inner\": 5, \"deeper\": {\"leaf\": true}}}");
        // rows at a path no version defines, and an object at a field: the normalizer keeps them whole
        baseline("01JAGSB0000000000000000012", FORM_KEEP, "agFormKVer1", false,
            "{\"title\": \"t\", \"notes\": [{\"text\": \"a\"}, {\"text\": \"b\"}]}");
        baseline("01JAGSB0000000000000000016", FORM_KEEP, "agFormKVer1", false,
            "{\"geo\": {\"lat\": 15.35, \"lng\": 44.2}}");
    }

    private static void dataElement(String id, String uid, String type) {
        db.update("INSERT INTO data_element (id, uid, code, name, type, created_by) VALUES (?, ?, ?, ?, ?, 'ag-test')",
            id, uid, uid, uid.toLowerCase(), type);
    }

    private static void form(String uid, String id) {
        db.update("""
            INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
            VALUES (?, ?, ?, ?, ?, 1, 'ag-test')""", id, uid, uid, "ag form " + uid, "agV" + uid.substring(3));
    }

    private static void version(String formUid, String formId, String id, String uid, int number, String sections,
                                String fields) {
        db.update("""
            INSERT INTO data_template_version
                (id, uid, template_uid, version_number, sections, fields, data_template_id, created_by)
            VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, 'ag-test')""",
            id, uid, formUid, number, sections, fields, formId);
    }

    private static String submission(String id, String formUid, String versionUid, boolean deleted, String document) {
        named.update("""
                INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no,
                                             assignment_uid, activity_uid, team_uid, org_unit_uid, status,
                                             form_data, lock_version, created_by, created_date, last_modified_date,
                                             deleted)
                VALUES (:id, :uid, :form, :version, 1, 'agAssign001', 'agActiv0001', 'agTeam00001', 'agOrgUn0001',
                        'COMPLETED', CAST(:document AS jsonb), 0, 'ag-test', TIMESTAMP '2026-11-20 08:00:00',
                        TIMESTAMP '2026-11-20 08:00:00', :deleted)""",
            new MapSqlParameterSource().addValue("id", id).addValue("uid", "ag" + id.substring(17))
                .addValue("form", formUid).addValue("version", versionUid).addValue("document", document)
                .addValue("deleted", deleted));
        return id;
    }

    private static void baseline(String id, String formUid, String versionUid, boolean deleted, String document) {
        submission(id, formUid, versionUid, deleted, document);
        revision(id, 0, null, "BASELINE", "MIGRATION", null);
    }

    private static void revision(String id, long version, Long previous, String kind, String command, String user) {
        named.update("""
                INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command,
                                                         actor_user_id, actor_system, form_data, metadata)
                SELECT s.id, :version, :previous, :kind, :command, :user,
                       CASE WHEN :user IS NULL THEN 'MIGRATION' END, s.form_data,
                       journal.submission_revision_metadata(s)
                FROM data_submission s WHERE s.id = :id""",
            new MapSqlParameterSource().addValue("id", id).addValue("version", version)
                .addValue("previous", previous).addValue("kind", kind).addValue("command", command)
                .addValue("user", user, java.sql.Types.VARCHAR));
    }

    private static FeedAgreement.Settings settings(List<String> forms, int batch) {
        return new FeedAgreement.Settings(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword(), PG.getDatabaseName(),
            false, forms, batch);
    }

    private static FeedAgreement.Report run(List<String> forms, int batch, FeedAgreement.Normalizing normalizing)
        throws Exception {
        FeedAgreement.Settings settings = settings(forms, batch);
        try (Connection connection = FeedAgreement.connect(settings)) {
            return FeedAgreement.run(connection, settings, normalizing);
        }
    }

    @Test
    void theNormalizerAgreesWithTheOracleOnEveryHead() throws Exception {
        // batches of 3, so that heads cross batch boundaries
        FeedAgreement.Report report = run(List.of(FORM_A_ID, FeedAgreement.NO_FORM), 3, SubmissionNormalizer::normalize);

        assertThat(report.agrees()).as(report.text()).isTrue();
        assertThat(report.disagreementsByForm()).isEmpty();
        assertThat(report.lines()).contains(
            entry("heads", 13L),
            entry("submissions without a head", 1L),
            entry("heads whose document is not an object", 1L),
            entry("oracle mapped values", 19L),
            entry("oracle unmapped values", 8L),
            entry("oracle empty arrays (apart)", 3L),
            entry("oracle rows", 7L),
            entry("normalized values", 20L),
            entry("normalized unmapped values", 8L),
            entry("normalized unmapped values, PATH_NOT_IN_FORM", 4L),
            entry("normalized unmapped values, FIELD_ALREADY_HAS_VALUE", 3L),
            entry("normalized unmapped values, DOCUMENT_NOT_AN_OBJECT", 1L),
            entry("normalized empty arrays (apart)", 1L),
            entry("normalized rows", 7L),
            entry("heads using a row id twice (compared without row ids)", 1L),
            entry(FeedAgreement.WHOLE, 0L),
            entry("normalization flags, DUPLICATE_ID", 1L),
            entry("normalization flags, PINNED_VERSION_MISSING", 4L),
            entry("normalization flags, FORM_MISSING", 1L),
            entry("normalization flags, DOCUMENT_MISSING", 1L));
        assertThat(run(List.of(FORM_A_ID, FeedAgreement.NO_FORM), 500, SubmissionNormalizer::normalize).lines())
            .as("the batch size changes no count").isEqualTo(report.lines());
    }

    /** The real normalizer's result for one head, changed by {@code change}; every other head as it is. */
    private static FeedAgreement.Normalizing changing(String submissionId,
                                                      java.util.function.UnaryOperator<NormalizedSubmission> change) {
        return (stored, form) -> {
            NormalizedSubmission real = SubmissionNormalizer.normalize(stored, form);
            return stored.id().equals(submissionId) ? change.apply(real) : real;
        };
    }

    /** The harness is not vacuous: a normalizer that drops a value and renames a row is caught, and its form named. */
    @Test
    void aFaultyNormalizerIsCaughtAndItsFormNamed() throws Exception {
        FeedAgreement.Report report = run(List.of(FORM_A_ID, FeedAgreement.NO_FORM), 500,
            changing("01JAGSB0000000000000000001", real -> {
                List<FieldValue> values = real.values().subList(1, real.values().size());
                List<NormalizedRow> rows = new ArrayList<>(real.rows());
                NormalizedRow first = rows.get(0);
                rows.set(0, new NormalizedRow("not-the-head-s-row", first.idOrigin(), first.storedId(),
                    first.parentRowId(), first.sectionId(), first.sectionPath(), first.index(), first.values(),
                    first.unmapped()));
                return new NormalizedSubmission(real.submissionId(), real.formId(), "another-version", values,
                    real.unmapped(), rows, real.flags());
            }));

        assertThat(report.agrees()).isFalse();
        assertThat(report.lines()).contains(
            entry(FeedAgreement.MAPPED_MISSING, 3L),
            entry(FeedAgreement.MAPPED_EXTRA, 2L),
            entry(FeedAgreement.MAPPED_DIFFERENT, 0L),
            entry(FeedAgreement.UNMAPPED_MISSING, 0L),
            entry(FeedAgreement.UNMAPPED_EXTRA, 0L),
            entry(FeedAgreement.ROWS_MISSING, 1L),
            entry(FeedAgreement.ROWS_EXTRA, 1L),
            entry(FeedAgreement.FORM_DIFFERS, 1L));
        assertThat(report.disagreementsByForm()).containsOnlyKeys(FORM_A_ID);
        assertThat(report.text()).contains("form " + FORM_A_ID + ": mapped missing 3; mapped extra 2; rows missing 1; "
            + "rows extra 1; form or version 1;");
    }

    /** A value kept under another field than the head's rule gives it is caught, though its path and value agree. */
    @Test
    void aValueUnderTheWrongFieldIdIsCaught() throws Exception {
        FeedAgreement.Report report = run(List.of(FORM_A_ID), 500, changing("01JAGSB0000000000000000001", real -> {
            List<FieldValue> values = new ArrayList<>(real.values());
            FieldValue title = values.get(0);
            values.set(0, new FieldValue("01JAGDE0000000000000000003", title.sectionId(), title.path(), title.value(),
                title.valueType(), title.issue(), title.formVersionId(), title.fromPinnedVersion()));
            return new NormalizedSubmission(real.submissionId(), real.formId(), real.formVersionId(), values,
                real.unmapped(), real.rows(), real.flags());
        }));

        assertThat(report.lines()).contains(
            entry(FeedAgreement.MAPPED_MISSING, 1L),
            entry(FeedAgreement.MAPPED_EXTRA, 1L),
            entry(FeedAgreement.UNMAPPED_MISSING, 0L),
            entry(FeedAgreement.UNMAPPED_EXTRA, 0L));
        assertThat(report.agrees()).isFalse();
    }

    /** A value the head's rule maps, kept as unmapped at its own path, is caught: the two kinds are compared apart. */
    @Test
    void aWronglyUnmappedValueIsCaught() throws Exception {
        FeedAgreement.Report report = run(List.of(FORM_A_ID), 500, changing("01JAGSB0000000000000000001", real -> {
            FieldValue title = real.values().get(0);
            List<UnmappedValue> unmapped = new ArrayList<>(real.unmapped());
            unmapped.add(new UnmappedValue(title.path(), title.value(), UnmappedReason.PATH_NOT_IN_FORM));
            return new NormalizedSubmission(real.submissionId(), real.formId(), real.formVersionId(),
                real.values().subList(1, real.values().size()), unmapped, real.rows(), real.flags());
        }));

        assertThat(report.lines()).contains(
            entry(FeedAgreement.MAPPED_MISSING, 1L),
            entry(FeedAgreement.MAPPED_EXTRA, 0L),
            entry(FeedAgreement.UNMAPPED_MISSING, 0L),
            entry(FeedAgreement.UNMAPPED_EXTRA, 1L));
        assertThat(report.agrees()).isFalse();
    }

    /**
     * Rows at a path no version defines, and an object at a field: each one value, kept whole. The oracle decides this
     * from the definitions in its own SQL and never walks into them, and the two sides' values kept whole are compared
     * apart.
     */
    @Test
    void whatIsKeptWholeIsDecidedByTheOracleAndComparedApart() throws Exception {
        FeedAgreement.Report report = run(List.of(FORM_KEEP_ID), 500, SubmissionNormalizer::normalize);

        assertThat(report.agrees()).as(report.text()).isTrue();
        assertThat(report.lines()).contains(
            entry("heads", 2L),
            entry(FeedAgreement.WHOLE, 2L),
            entry("oracle values kept whole", 2L),
            entry(FeedAgreement.WHOLE_MISSING, 0L),
            entry(FeedAgreement.WHOLE_EXTRA, 0L),
            entry(FeedAgreement.WHOLE_DIFFERENT, 0L),
            entry("oracle mapped values", 1L),
            entry("oracle rows", 0L));
    }

    /**
     * A normalizer that walks into an object the head's rule keeps whole (an object at a field) is caught: the oracle
     * keeps it whole and has nothing inside it, so the whole value is missing and the values inside are extra.
     */
    @Test
    void aValueKeptWholeThatTheNormalizerWalksIntoIsCaught() throws Exception {
        FeedAgreement.Report report = run(List.of(FORM_KEEP_ID), 500, changing("01JAGSB0000000000000000016", real -> {
            FieldValue geo = real.values().get(0);
            List<UnmappedValue> inside = new ArrayList<>(real.unmapped());
            geo.value().fields().forEachRemaining(member -> inside.add(new UnmappedValue(
                geo.path() + "." + member.getKey(), member.getValue(), UnmappedReason.PATH_NOT_IN_FORM)));
            return new NormalizedSubmission(real.submissionId(), real.formId(), real.formVersionId(),
                real.values().subList(1, real.values().size()), inside, real.rows(), real.flags());
        }));

        assertThat(report.lines()).contains(
            entry(FeedAgreement.WHOLE_MISSING, 1L),
            entry(FeedAgreement.WHOLE_EXTRA, 0L),
            entry(FeedAgreement.UNMAPPED_EXTRA, 2L),
            entry(FeedAgreement.MAPPED_MISSING, 0L));
        assertThat(report.agrees()).isFalse();
    }

    /** The coordinator's entry point: the environment names the database, and the counts go to a file. */
    @Test
    void theRunnerMeasuresTheDatabaseTheEnvironmentNames() throws Exception {
        java.nio.file.Path out = java.nio.file.Files.createTempDirectory("feed-agreement").resolve("report.txt");
        try {
            FeedAgreement.Report report = FeedAgreementRun.run(Map.of(
                "FEED_AGREEMENT_JDBC_URL", PG.getJdbcUrl(),
                "FEED_AGREEMENT_USER", PG.getUsername(),
                "FEED_AGREEMENT_PASSWORD", PG.getPassword(),
                "FEED_AGREEMENT_DATABASE", PG.getDatabaseName(),
                "FEED_AGREEMENT_FORMS", FORM_A_ID + "," + FeedAgreement.NO_FORM), out);

            assertThat(report.agrees()).isTrue();
            assertThat(java.nio.file.Files.readString(out)).isEqualTo(report.text())
                .contains("heads = 13\n", FeedAgreement.MAPPED_MISSING + " = 0\n")
                .doesNotContain("T1", "01JAGSB");
        } finally {
            java.nio.file.Files.deleteIfExists(out);
            java.nio.file.Files.deleteIfExists(out.getParent());
        }
    }

    /** The bundle's command, as a lab host runs it: the counts to the file named, and the exit status by the result. */
    @Test
    void theCommandWritesItsCountsToAFileAndExitsByTheResult() throws Exception {
        java.nio.file.Path out = java.nio.file.Files.createTempDirectory("feed-agreement").resolve("counts.txt");
        try {
            assertThat(FeedAgreementMain.run(Map.of(
                "FEED_AGREEMENT_JDBC_URL", PG.getJdbcUrl(),
                "FEED_AGREEMENT_USER", PG.getUsername(),
                "FEED_AGREEMENT_PASSWORD", PG.getPassword(),
                "FEED_AGREEMENT_DATABASE", PG.getDatabaseName(),
                "FEED_AGREEMENT_FORMS", FORM_A_ID + "," + FeedAgreement.NO_FORM,
                "FEED_AGREEMENT_REPORT", out.toString()))).as("every line agrees").isZero();
            assertThat(java.nio.file.Files.readString(out)).contains("heads = 13\n");
            assertThat(FeedAgreementMain.run(Map.of("FEED_AGREEMENT_JDBC_URL", PG.getJdbcUrl(),
                "FEED_AGREEMENT_REPORT", out.toString()))).as("no database named: refused").isEqualTo(2);
        } finally {
            java.nio.file.Files.deleteIfExists(out);
            java.nio.file.Files.deleteIfExists(out.getParent());
        }
    }

    @Test
    void itReadsOnlyAndHandsTheConnectionBackAsItCame() throws Exception {
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            FeedAgreement.run(connection, settings(List.of(), 500));
            assertThat(connection.getAutoCommit()).isTrue();
            assertThat(connection.isReadOnly()).isFalse();
        }
        assertThat(db.queryForObject("SELECT count(*) FROM journal.submission_revision", Long.class))
            .isEqualTo(revisions);
        assertThat(FeedAgreement.Settings.fromEnvironment(Map.of(
            "FEED_AGREEMENT_JDBC_URL", "jdbc:postgresql://clone:5432/db", "FEED_AGREEMENT_DATABASE", "db",
            "FEED_AGREEMENT_FORMS", " a1 , b2 ", "FEED_AGREEMENT_BATCH", "50")))
            .isEqualTo(new FeedAgreement.Settings("jdbc:postgresql://clone:5432/db", null, null, "db", false,
                List.of("a1", "b2"), 50));
    }

    /** The run measures only the database the operator names, and production only when production is allowed. */
    @Test
    void itMeasuresOnlyTheDatabaseNamed_andProductionOnlyWhenAllowed() throws Exception {
        String url = PG.getJdbcUrl();
        assertThatThrownBy(() -> run(new FeedAgreement.Settings(url, PG.getUsername(), PG.getPassword(), null, false,
            List.of(), 500))).hasMessageContaining("Name the database to measure");
        assertThatThrownBy(() -> run(new FeedAgreement.Settings(url, PG.getUsername(), PG.getPassword(),
            "datarun_staging", false, List.of(), 500)))
            .as("connected to another database than the one named").isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not to datarun_staging");

        // production's database host, by name or address, refused before any connection is tried
        for (String host : List.of("datarun-prod-db.lab", "DATARUN-PROD-DB.LAB", "192.168.1.111")) {
            assertThatThrownBy(() -> FeedAgreement.connect(new FeedAgreement.Settings(
                "jdbc:postgresql://" + host + ":5432/nmcpdb?sslmode=require", "u", "p", "nmcpdb", false, List.of(), 1)))
                .as(host).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("FEED_AGREEMENT_PRODUCTION");
        }
        // staging's clone is not production, whatever its host's name says
        assertThatThrownBy(() -> FeedAgreement.connect(new FeedAgreement.Settings(
            "jdbc:postgresql://product-staging-db.lab.invalid:5432/datarun_staging?connectTimeout=1", "u", "p",
            "datarun_staging", false, List.of(), 1)))
            .as("passes the guards and only then fails to connect").isInstanceOf(java.sql.SQLException.class);

        // production's database name, reached through any address (a tunnel, a clone under that name)
        db.execute("CREATE DATABASE nmcpdb");
        try {
            String nmcpdb = url.substring(0, url.lastIndexOf('/') + 1) + "nmcpdb";
            assertThatThrownBy(() -> run(new FeedAgreement.Settings(nmcpdb, PG.getUsername(), PG.getPassword(),
                "nmcpdb", false, List.of(), 500)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("production's database");
        } finally {
            db.execute("DROP DATABASE nmcpdb");
        }
    }

    private static FeedAgreement.Report run(FeedAgreement.Settings settings) throws Exception {
        try (Connection connection = FeedAgreement.connect(settings)) {
            return FeedAgreement.run(connection, settings);
        }
    }
}
