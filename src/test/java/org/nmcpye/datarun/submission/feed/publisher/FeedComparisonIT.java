package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.SqlTestContainer;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.normalized.DerivedIds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 * fixture's form to its field. Values are invented.
 */
@FeedPublisherIntegrationTest
class FeedComparisonIT {

    @Autowired
    private EtlOrchestrator orchestrator;

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
