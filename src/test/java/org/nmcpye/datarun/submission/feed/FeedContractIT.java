package org.nmcpye.datarun.submission.feed;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The submission feed's contract, version 1, as code (owner, 2026-11-26: "we need to guard such things, and not just
 * stay as chat prose"). It reads the schema every changeset has migrated, as the application runs it, and holds it to
 * {@code docs/features/submission-feed/contract.md} as it stands: the five streams with exactly the contract's columns,
 * types and nullability (§2, §3, §6, §7), their keys (§1), the BRIN cursor index and fillfactor of the two submission
 * streams, and the shared position sequence with CACHE 1 (§8.1). Two rules hold whatever the contract says: no column
 * is a uid (§5), and no column is history (who changed a record, how, or who last modified it).
 *
 * <p>Any drift fails, whichever changeset makes it, so a change to the contract is a change to this test, made
 * deliberately and reviewed with it. {@code FeedSchemaMigrationIT} tests the first changeset alone, which a later
 * revision does not change; this test follows the contract.</p>
 *
 * <p>BRIN and fillfactor are not in the contract's text: they come from the schema's physical design
 * ({@code changelog/feed/20261125-feed-schema.xml}), which the cursor's reads depend on.</p>
 */
@IntegrationTest
class FeedContractIT {

    /**
     * The streams, column by column, as the contract types them: name, type, then {@code null} or {@code not null}.
     * Types are the contract's: timestamptz is {@code timestamp with time zone}.
     */
    static final Map<String, List<String>> STREAMS = Map.of(
        "submission", List.of(
            "position bigint not null",
            "id text not null",
            "version bigint not null",
            "deleted boolean not null",
            "form_id text not null",
            "form_version_id text null",
            "assignment_id text not null",
            "team_id text not null",
            "org_unit_id text not null",
            "activity_id text not null",
            "created_at timestamptz not null",
            "entered_start_at timestamptz null",
            "entered_end_at timestamptz null",
            "changed_at timestamptz not null",
            "published_at timestamptz not null",
            "field_values jsonb not null",
            "unmapped_values jsonb null"),
        "submission_row", List.of(
            "position bigint not null",
            "submission_id text not null",
            "id text not null",
            "id_origin text not null",
            "parent_row_id text null",
            "section_id text not null",
            "row_index integer not null",
            "deleted boolean not null",
            "changed_in_version bigint not null",
            "published_at timestamptz not null",
            "field_values jsonb not null",
            "unmapped_values jsonb null"),
        "form_field", List.of(
            "form_id text not null",
            "form_version_id text not null",
            "form_version_number integer not null",
            "field_id text not null",
            "section_id text null",
            "repeat_section_id text null",
            "name text not null",
            "label jsonb null",
            "value_type text null",
            "option_set_id text null",
            "mandatory boolean null",
            "sort_order integer null",
            "published_at timestamptz not null"),
        "form_section", List.of(
            "form_id text not null",
            "form_version_id text not null",
            "form_version_number integer not null",
            "section_id text not null",
            "parent_section_id text null",
            "name text null",
            "label jsonb null",
            "repeatable boolean not null",
            "sort_order integer null",
            "published_at timestamptz not null"),
        "freshness", List.of(
            "feed text not null",
            "last_position bigint null",
            "last_published_at timestamptz null",
            "last_changed_at timestamptz null",
            "backlog bigint not null",
            "oldest_unpublished_at timestamptz null",
            "pending bigint not null",
            "oldest_pending_at timestamptz null",
            "dead bigint not null",
            "unpublished bigint null",
            "last_reconciled_at timestamptz null",
            "first_publication_completed_at timestamptz null",
            "checked_at timestamptz not null"));

    /**
     * Every index of the feed's tables: the keys of §1, and the BRIN cursor index of each submission stream (a range of
     * 4 pages, summarized as rows come). No other index: a btree on position would make every rewrite a non-HOT update.
     */
    static final Set<String> INDEXES = Set.of(
        "submission: primary key btree (id)",
        "submission: brin (position) autosummarize=on,pages_per_range=4",
        "submission_row: primary key btree (submission_id, id)",
        "submission_row: brin (position) autosummarize=on,pages_per_range=4",
        "form_field: unique nulls not distinct btree (form_version_id, section_id, field_id)",
        "form_section: primary key btree (form_version_id, section_id)",
        "freshness: primary key btree (feed)");

    /** The tables' storage options: headroom for in-place rewrites on the submission streams, none elsewhere. */
    static final Map<String, String> TABLE_OPTIONS = Map.of(
        "submission", "fillfactor=80",
        "submission_row", "fillfactor=80",
        "form_field", "",
        "form_section", "",
        "freshness", "");

    /** Every relation of schema feed but the indexes: the five streams and the position sequence (§8.1). */
    static final Set<String> RELATIONS = Set.of(
        "submission table", "submission_row table", "form_field table", "form_section table", "freshness table",
        "position_seq sequence");

    /**
     * The actor columns the contract allows: none in version 1. The creator's user id is the one the contract will
     * allow, in a later revision; it is added here then, by name.
     */
    static final Set<String> ACTOR_COLUMNS_ALLOWED = Set.of();

    /** A uid, by name: the column {@code uid}, or one ending in {@code _uid}. */
    static boolean isUid(String column) {
        return column.equals("uid") || column.endsWith("_uid");
    }

    /** Words of a column's name that make it history: who made a change or last modified a record, or how. */
    private static final Pattern HISTORY = Pattern.compile(
        "(^|_)(by|editor|editors|edited|actor|actors|user|users|command|commands|modified|modifier|history|"
            + "revision|revisions|on_behalf_of)(_|$)");

    /** A history column, by name: an editor, an actor, a command, a last-modified-by, or a revision. */
    static boolean isHistory(String column) {
        return HISTORY.matcher(column).find();
    }

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void theStreamsHaveExactlyTheContractsColumnsTypesAndNullability() {
        Map<String, Set<String>> migrated = new TreeMap<>();
        jdbc.query("""
            SELECT table_name, column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'feed'""", Map.of(), rs -> {
            String type = rs.getString("data_type").replace("timestamp with time zone", "timestamptz");
            migrated.computeIfAbsent(rs.getString("table_name"), table -> new TreeSet<>()).add(
                rs.getString("column_name") + " " + type + ("YES".equals(rs.getString("is_nullable")) ? " null"
                    : " not null"));
        });

        Map<String, Set<String>> contract = new TreeMap<>();
        STREAMS.forEach((stream, columns) -> contract.put(stream, new TreeSet<>(columns)));
        assertThat(migrated).isEqualTo(contract);
    }

    @Test
    void theStreamsHaveExactlyTheContractsKeysAndCursorIndexes() {
        Set<String> migrated = new TreeSet<>(jdbc.queryForList("""
            SELECT t.relname || ': '
                   || CASE WHEN x.indisprimary THEN 'primary key '
                           WHEN x.indisunique AND x.indnullsnotdistinct THEN 'unique nulls not distinct '
                           WHEN x.indisunique THEN 'unique '
                           ELSE '' END
                   || am.amname || ' ('
                   || (SELECT string_agg(a.attname, ', ' ORDER BY k.n)
                       FROM unnest(x.indkey::int2[]) WITH ORDINALITY AS k(attnum, n)
                       LEFT JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum)
                   || ')'
                   || CASE WHEN x.indpred IS NOT NULL THEN ' where ' || pg_get_expr(x.indpred, t.oid) ELSE '' END
                   || coalesce(' ' || (SELECT string_agg(o, ',' ORDER BY o) FROM unnest(i.reloptions) AS o), '')
            FROM pg_index x
            JOIN pg_class i ON i.oid = x.indexrelid
            JOIN pg_class t ON t.oid = x.indrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            JOIN pg_am am ON am.oid = i.relam
            WHERE n.nspname = 'feed'""", Map.of(), String.class));

        assertThat(migrated).isEqualTo(new TreeSet<>(INDEXES));
    }

    @Test
    void theSubmissionStreamsKeepFillfactorHeadroomAndTheOthersNoOptions() {
        Map<String, String> migrated = new TreeMap<>();
        jdbc.query("""
            SELECT c.relname, coalesce((SELECT string_agg(o, ',' ORDER BY o) FROM unnest(c.reloptions) AS o), '')
                              AS options
            FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'feed' AND c.relkind IN ('r', 'p')""", Map.of(),
            rs -> {
                migrated.put(rs.getString("relname"), rs.getString("options"));
            });

        assertThat(migrated).isEqualTo(new TreeMap<>(TABLE_OPTIONS));
    }

    @Test
    void theSchemaHoldsTheFiveStreamsAndOneSharedPositionSequenceCachingOneValue() {
        Set<String> relations = new TreeSet<>(jdbc.queryForList("""
            SELECT c.relname || ' ' || CASE c.relkind WHEN 'r' THEN 'table' WHEN 'p' THEN 'partitioned table'
                                                     WHEN 'v' THEN 'view' WHEN 'm' THEN 'materialized view'
                                                     WHEN 'S' THEN 'sequence' WHEN 'f' THEN 'foreign table'
                                                     ELSE 'relation of kind ' || c.relkind::text END
            FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'feed' AND c.relkind NOT IN ('i', 'I', 't', 'c')""", Map.of(), String.class));
        assertThat(relations).isEqualTo(new TreeSet<>(RELATIONS));

        assertThat(jdbc.queryForMap("""
            SELECT data_type::text AS type, start_value, increment_by, cache_size, cycle
            FROM pg_sequences WHERE schemaname = 'feed' AND sequencename = 'position_seq'""", Map.of()))
            .containsEntry("type", "bigint")
            .containsEntry("start_value", 1L)
            .containsEntry("increment_by", 1L)
            .containsEntry("cache_size", 1L)
            .containsEntry("cycle", false);
    }

    /** §5: no uid. A model that still needs a submission's uid joins public.data_submission (id, uid). */
    @Test
    void noFeedColumnIsAUid() {
        assertThat(feedColumns().stream().filter(column -> isUid(column.split("\\.", 2)[1])).toList()).isEmpty();
    }

    /**
     * The feed publishes current state, no history: no editor, no command, nothing last-modified-by (owner,
     * 2026-11-25). History stays in DataRun's revision journal.
     */
    @Test
    void noFeedColumnIsHistory() {
        assertThat(feedColumns().stream()
            .filter(column -> isHistory(column.split("\\.", 2)[1]))
            .filter(column -> !ACTOR_COLUMNS_ALLOWED.contains(column))
            .toList()).isEmpty();
    }

    /** The two rules catch what they are for, and pass what the contract has. */
    @Test
    void theRulesCatchAUidAndHistoryAndPassTheContractsColumns() {
        assertThat(List.of("uid", "submission_uid", "template_uid")).allMatch(FeedContractIT::isUid);
        assertThat(List.of("last_modified_by", "modified_by", "created_by", "updated_by", "edited_by", "editor",
            "editor_id", "actor_user_id", "command", "command_id", "on_behalf_of", "revision", "user_id"))
            .allMatch(FeedContractIT::isHistory);
        List<String> contractColumns = STREAMS.values().stream().flatMap(List::stream)
            .map(column -> column.split(" ", 2)[0]).distinct().toList();
        assertThat(contractColumns).noneMatch(FeedContractIT::isUid).noneMatch(FeedContractIT::isHistory)
            .as("columns that look like history, and are not").contains("changed_at", "changed_in_version",
                "last_changed_at", "last_published_at", "unpublished");
    }

    /** Every column of every table in schema feed, as {@code table.column}. */
    private List<String> feedColumns() {
        return jdbc.queryForList("""
            SELECT table_name || '.' || column_name FROM information_schema.columns
            WHERE table_schema = 'feed' ORDER BY 1""", Map.of(), String.class);
    }
}
