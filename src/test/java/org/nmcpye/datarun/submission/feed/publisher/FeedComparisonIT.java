package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.SqlTestContainer;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.normalized.DerivedIds;
import org.nmcpye.datarun.submission.normalized.FieldDefinition;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.COUNT_FIELD;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ITEM_FIELD;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.NAME_FIELD;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * The comparison queries and the crosswalk ({@code scripts/feed/comparison.sql}, {@code crosswalk.sql}), run as a lab
 * host runs them: each file alone, with psql, in the database's container. On submissions the feed and tall and events
 * both took, the feed's counts and tall's and events' agree, and the crosswalk maps each canonical element of the
 * fixture's form to its field. The two files restate rules of the Java code in SQL, accepted for migration aids
 * (engineering, 2026-11-26) because these tests pin them to it: a field's data path
 * ({@code FormVersionDefinition.parse}) on every branch of the rule, and the section id recipe
 * ({@link DerivedIds#sectionId}). Values are invented.
 */
@FeedPublisherIntegrationTest
class FeedComparisonIT {

    /** A form of the data path test's own, with a field on each branch of the rule; removed after the test. */
    private static final String PATHS_FORM_ID = "01FDPATHFORM00000000000001";
    private static final String PATHS_FORM_UID = "fpPathForm1";
    private static final String PATHS_VERSION_ID = "01FDPATHFORMVER00000000001";
    private static final String PATHS_VERSION_UID = "fpPathVer01";
    private static final String PATHS_FIELD_ID_PREFIX = "01FDPATHDE";

    /** The data path rule as a file of {@code scripts/feed} states it: from its CASE to the field's name check. */
    private static final Pattern DATA_PATH_RULE = Pattern.compile(
        "CASE WHEN f\\.field ->> 'path' IS NULL.*?WHERE f\\.field ->> 'name' IS NOT NULL", Pattern.DOTALL);

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private FormDefinitionJdbcReader definitions;

    @Autowired
    private SqlTestContainer database;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private FeedFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
    }

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    /** One SQL file of {@code scripts/feed}, alone, as psql runs it; its standard output. */
    private String psql(String file) throws Exception {
        JdbcDatabaseContainer<?> container = database.getTestContainer();
        container.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts", "feed", file)), "/tmp/" + file);
        var result = container.execInContainer("psql", "-X", "-q", "-U", container.getUsername(), "-d",
            container.getDatabaseName(), "-f", "/tmp/" + file);
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        assertThat(result.getStderr()).isEmpty();
        return result.getStdout();
    }

    /** The fixture's canonical element at a path, as the fixture names it. */
    private static String element(String path) {
        return UUID.nameUUIDFromBytes((FORM_UID + "|" + path).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** The data path test's canonical element at a path. */
    private static UUID pathsElement(String path) {
        return UUID.nameUUIDFromBytes((PATHS_FORM_UID + "|" + path).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The data path test's form: one version, a field on each branch of the rule. Its stored fields: one with no
     * stored path, one whose stored path is its data element's uid, one with a stored path of one key (each outside
     * every section), one in a section and one in a repeating section.
     */
    private void createPathsForm() {
        String[][] dataElements = {
            {"fpPathDe001", "fp_loose"}, {"fpPathDe002", "fp_top"}, {"fpPathDe003", "fp_flat"},
            {"fpPathDe004", "fp_inner"}, {"fpPathDe005", "fp_cell"}};
        for (int i = 0; i < dataElements.length; i++) {
            jdbc.update("""
                    INSERT INTO data_element (id, uid, code, name, type, created_by)
                    VALUES (:id, :uid, :uid, :name, 'Text', 'fp-test')""",
                Map.of("id", PATHS_FIELD_ID_PREFIX + "%016d".formatted(i + 1), "uid", dataElements[i][0], "name",
                    dataElements[i][1]));
        }
        jdbc.update("""
                INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
                VALUES (:id, :uid, :uid, 'fp path form', :version, 1, 'fp-test')""",
            Map.of("id", PATHS_FORM_ID, "uid", PATHS_FORM_UID, "version", PATHS_VERSION_UID));
        jdbc.update("""
                INSERT INTO data_template_version
                    (id, uid, template_uid, version_number, sections, fields, data_template_id, created_by)
                VALUES (:id, :uid, :formUid, 1, CAST(:sections AS jsonb), CAST(:fields AS jsonb), :formId,
                        'fp-test')""",
            Map.of("id", PATHS_VERSION_ID, "uid", PATHS_VERSION_UID, "formUid", PATHS_FORM_UID, "formId", PATHS_FORM_ID,
                "sections", """
                    [{"id": "main", "name": "main", "path": "main", "repeatable": false},
                     {"id": "rows", "name": "rows", "path": "main.rows", "parent": "main", "repeatable": true}]""",
                "fields", """
                    [{"id": "fpPathDe001", "name": "fp_loose", "type": "Text"},
                     {"id": "fpPathDe002", "name": "fp_top", "path": "fpPathDe002", "type": "Text"},
                     {"id": "fpPathDe003", "name": "fp_flat", "path": "flat", "type": "Text"},
                     {"id": "fpPathDe004", "name": "fp_inner", "path": "main.fpPathDe004", "type": "Text"},
                     {"id": "fpPathDe005", "name": "fp_cell", "path": "main.rows.fpPathDe005", "type": "Text"}]"""));
    }

    /** A canonical element of the data path test's form, at one data path. */
    private void insertPathsElement(String path) {
        jdbc.update("""
                INSERT INTO canonical_element (id, template_uid, preferred_name, safe_name, data_type, semantic_type,
                                               canonical_path, json_data_paths, parent_repeat_id, created_date)
                VALUES (:id, :template, :name, :name, 'TEXT', NULL, :path, CAST(:paths AS jsonb), NULL, now())""",
            new MapSqlParameterSource()
                .addValue("id", pathsElement(path))
                .addValue("template", PATHS_FORM_UID)
                .addValue("name", path.replace('.', '_'))
                .addValue("path", path)
                .addValue("paths", "[\"" + path + "\"]"));
    }

    private void removePathsForm() {
        Map<String, String> form = Map.of("id", PATHS_FORM_ID, "uid", PATHS_FORM_UID, "prefix",
            PATHS_FIELD_ID_PREFIX + "%");
        jdbc.update("DELETE FROM canonical_element WHERE template_uid = :uid", form);
        jdbc.update("DELETE FROM data_template_version WHERE data_template_id = :id", form);
        jdbc.update("DELETE FROM data_template WHERE id = :id", form);
        jdbc.update("DELETE FROM data_element WHERE id LIKE :prefix", form);
    }

    /** Each statement of the data path rule in a file of {@code scripts/feed}, its spaces folded. */
    private static List<String> dataPathRules(String file) throws IOException {
        Matcher rule = DATA_PATH_RULE.matcher(Files.readString(Path.of("scripts", "feed", file)));
        List<String> rules = new ArrayList<>();
        while (rule.find()) {
            rules.add(rule.group().replaceAll("\\s+", " "));
        }
        return rules;
    }

    /**
     * The crosswalk's data path rule is the Java code's on every branch: each field of the data path test's form gets
     * a canonical element at the data path the form definition gives it ({@code FormVersionDefinition.parse}, through
     * its reader), and crosswalk.sql maps each element to that field and to nothing else.
     */
    @Test
    void theCrosswalksDataPathRuleIsTheFormDefinitionsOnEveryBranch() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try {
            tx.executeWithoutResult(status -> createPathsForm());
            List<FieldDefinition> fields = tx.execute(status -> definitions.read(PATHS_FORM_ID).orElseThrow())
                .definition().versions().get(0).fields();
            assertThat(fields).as("the form definition's data paths, one per branch of the rule")
                .extracting(FieldDefinition::path)
                .containsExactly("fp_loose", "fp_top", "fp_flat", "main.fp_inner", "main.rows.fp_cell");
            assertThat(fields).allSatisfy(field -> assertThat(field.id()).startsWith(PATHS_FIELD_ID_PREFIX));
            tx.executeWithoutResult(status -> fields.forEach(field -> insertPathsElement(field.path())));

            List<String> mapped = Arrays.stream(psql("crosswalk.sql").split("\n"))
                .filter(line -> line.contains("," + PATHS_FORM_ID + ","))
                .toList();

            assertThat(mapped).containsExactlyInAnyOrderElementsOf(fields.stream()
                .map(field -> pathsElement(field.path()) + "," + PATHS_FORM_ID + "," + field.id() + ","
                    + PATHS_VERSION_ID)
                .toList());
        } finally {
            tx.executeWithoutResult(status -> removePathsForm());
        }
    }

    /**
     * comparison.sql states the data path rule twice, for its values and for its crosswalk counts: both are
     * crosswalk.sql's statement of it, which the test above holds to the Java code.
     */
    @Test
    void theComparisonQueriesStateTheCrosswalksDataPathRule() throws Exception {
        List<String> crosswalk = dataPathRules("crosswalk.sql");
        assertThat(crosswalk).hasSize(1);
        assertThat(dataPathRules("comparison.sql")).hasSize(2).containsOnly(crosswalk.get(0));
    }

    @Test
    void theFeedsCountsSitBesideTallsAndEventsAndTheCrosswalkMapsEachElementToItsField() throws Exception {
        fixture.create(document("a", 1, row("r1", "x"), row("r2", "y")));
        fixture.create(document("b", 2, row("r3", "z")));
        FeedFixture.Ids deleted = fixture.create(document("gone", 3, row("r4", "w")));
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // the feed, tall and events take every change
        }
        fixture.delete(deleted);
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // and the delete
        }

        List<String> lines = Arrays.asList(psql("comparison.sql").split("\n"));

        assertThat(lines.get(0)).startsWith("run|");
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertThat(lines).as("live submissions: stored, in the feed, in tall")
            .contains("submissions|" + FORM_ID + "|" + today + "|2|2|2");
        String section = DerivedIds.sectionId(FORM_ID, "main.rows");
        assertThat(lines).as("live rows of the repeating section: in the feed, as repeat events")
            .contains("rows|" + FORM_ID + "|" + section + "|3|3");
        assertThat(lines).as("values per field: in the feed, in tall through the crosswalk")
            .contains("values|" + FORM_ID + "|" + NAME_FIELD + "|2|2",
                "values|" + FORM_ID + "|" + COUNT_FIELD + "|2|2",
                "values|" + FORM_ID + "|" + ITEM_FIELD + "|3|3");
        assertThat(lines).contains("crosswalk|elements of a field mapping to more than one field (expected 0)|0");
        assertThat(lines).allMatch(line -> line.matches("[a-z]+\\|.*"));

        List<String> crosswalk = Arrays.asList(psql("crosswalk.sql").split("\n"));
        assertThat(crosswalk.get(0)).isEqualTo("canonical_element_id,form_id,field_id,form_version_ids");
        assertThat(crosswalk).contains(
            element("main.fp_name") + "," + FORM_ID + "," + NAME_FIELD + "," + FeedFixture.VERSION_1_ID + " "
                + FeedFixture.VERSION_2_ID,
            element("main.fp_count") + "," + FORM_ID + "," + COUNT_FIELD + "," + FeedFixture.VERSION_2_ID,
            element("main.rows.fp_item") + "," + FORM_ID + "," + ITEM_FIELD + "," + FeedFixture.VERSION_1_ID + " "
                + FeedFixture.VERSION_2_ID);
        assertThat(crosswalk).noneMatch(line -> line.startsWith(element("main.rows") + ","));
        assertThat(fixture.count("SELECT count(*) FROM feed.submission WHERE form_id = :form", Map.of("form", FORM_ID)))
            .as("the deleted submission's record stays, deleted").isEqualTo(3);
    }
}
