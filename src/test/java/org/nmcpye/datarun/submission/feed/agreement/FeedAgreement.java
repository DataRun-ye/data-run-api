package org.nmcpye.datarun.submission.feed.agreement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.submission.normalized.FieldValue;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.NormalizedRow;
import org.nmcpye.datarun.submission.normalized.NormalizedSubmission;
import org.nmcpye.datarun.submission.normalized.StoredJson;
import org.nmcpye.datarun.submission.normalized.StoredSubmission;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;
import org.nmcpye.datarun.submission.normalized.UnmappedValue;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

/**
 * The feed's agreement harness (build step F8's measure): the submission normalizer against the feed spike's oracle,
 * on every head of the revision journal, counts only.
 *
 * <p>The oracle ({@code scripts/feed/sql/oracle-walk.sql} and {@code oracle-header.sql}) reads each head in SQL: every
 * value at its data path in its row, rows keyed by the contract's recipe, the field each value is expected under by the
 * contract's resolution rule, and the ids of the form and form version the head names. The normalizer reads the same
 * head with the stored definitions ({@link FormDefinitionJdbcReader}), as the publisher will. For each head the two
 * are compared as multisets: mapped values by row, field id and JSON value, unmapped values by row, data path and JSON
 * value (numbers exact, an object's keys in any order), each reported apart, so that a value under the wrong field,
 * or wrongly unmapped, is counted; rows by id; and the form and form version ids. Empty arrays are counted apart:
 * without the shape of a definition they are zero rows or an empty multi-select. A head whose rows use one id twice is
 * compared without row ids, since the oracle cannot give the later rows their derived ids.</p>
 *
 * <p>The lines whose names start with "agree:" must read 0; the others are for reference. What the normalizer keeps
 * whole, an object at a field or rows at a path no version defines, is its own category: the value is set apart with
 * the oracle's values and rows inside it, and counted.</p>
 *
 * <p><b>Where it runs.</b> The operator names the database to measure ({@code FEED_AGREEMENT_DATABASE}), and the run
 * refuses unless the database it is connected to is that one ({@code current_database()}). A second guard refuses
 * production's known endpoints, its database host by name or address and its database's name, unless
 * {@code FEED_AGREEMENT_PRODUCTION=true}, as {@code scripts/integrity/check.sh} gates production.</p>
 *
 * <p><b>Safety.</b> One {@code REPEATABLE READ READ ONLY} transaction, so every count comes from one snapshot and
 * nothing can be written; {@code work_mem} 8MB, no parallel workers, no JIT, a statement timeout, a lock timeout, and
 * an idle timeout; batches of heads by submission id. It prints counts and form ids, never a value or another id.</p>
 */
public final class FeedAgreement {

    /** How the forms named, and the report, call the heads whose form is not stored: they have no form id. */
    public static final String NO_FORM = "-";

    /** Every line that must read 0 starts with this. */
    public static final String AGREE = "agree: ";

    public static final String MAPPED_MISSING = AGREE + "mapped values missing (in the head, not normalized under its field)";
    public static final String MAPPED_EXTRA = AGREE + "mapped values extra (normalized under a field the head does not "
        + "give it)";
    public static final String MAPPED_DIFFERENT = AGREE + "mapped values different (one field of one row, another value)";
    public static final String UNMAPPED_MISSING = AGREE + "unmapped values missing (in the head, not normalized as "
        + "unmapped)";
    public static final String UNMAPPED_EXTRA = AGREE + "unmapped values extra (normalized as unmapped, not in the head)";
    public static final String UNMAPPED_DIFFERENT = AGREE + "unmapped values different (one path of one row, another "
        + "value)";
    public static final String ROWS_MISSING = AGREE + "head rows missing from the normalized rows";
    public static final String ROWS_EXTRA = AGREE + "normalized rows not in the head";
    public static final String FORM_DIFFERS = AGREE + "form or form version differs from the head's";
    public static final String WHOLE = "values kept whole (an object at a field, or rows at a path no version defines)";

    private static final Path ORACLE_WALK = Path.of("scripts", "feed", "sql", "oracle-walk.sql");
    private static final Path ORACLE_HEADER = Path.of("scripts", "feed", "sql", "oracle-header.sql");

    private static final List<String> SESSION = List.of(
        "SET LOCAL work_mem = '8MB'",
        "SET LOCAL max_parallel_workers_per_gather = 0",
        "SET LOCAL jit = off",
        "SET LOCAL statement_timeout = '300s'",
        "SET LOCAL lock_timeout = '5s'",
        "SET LOCAL idle_in_transaction_session_timeout = '10min'");

    private static final JsonNode EMPTY_ARRAY = new ObjectMapper().createArrayNode();

    /** Production's database host, by name and address ({@code docs/agent-context/release-boundary.md}). */
    static final Set<String> PRODUCTION_HOSTS = Set.of("datarun-prod-db.lab", "datarun-prod-db", "192.168.1.111");

    /** Production's database ({@code deploy/production/compose.yml}). */
    static final Set<String> PRODUCTION_DATABASES = Set.of("nmcpdb");

    /** How the normalizer side reads a head; the real normalizer, or a faulty one to prove the harness. */
    public interface Normalizing extends BiFunction<StoredSubmission, FormDefinition, NormalizedSubmission> {
    }

    /**
     * Where to connect, the database the operator names (the run refuses any other), whether production may be
     * measured, which forms (none: all), and how many heads per batch.
     */
    public record Settings(String jdbcUrl, String user, String password, String database, boolean production,
                           List<String> formIds, int batchSize) {

        public Settings {
            Objects.requireNonNull(jdbcUrl, "jdbcUrl");
            formIds = formIds == null ? List.of() : List.copyOf(formIds);
            if (batchSize < 1) {
                throw new IllegalArgumentException("a batch holds at least one head");
            }
        }

        /**
         * From the environment: FEED_AGREEMENT_JDBC_URL, _USER, _PASSWORD, _DATABASE, _PRODUCTION (true or not),
         * _FORMS (comma-separated), _BATCH.
         */
        public static Settings fromEnvironment(Map<String, String> environment) {
            String forms = environment.getOrDefault("FEED_AGREEMENT_FORMS", "");
            return new Settings(
                environment.get("FEED_AGREEMENT_JDBC_URL"),
                environment.get("FEED_AGREEMENT_USER"),
                environment.get("FEED_AGREEMENT_PASSWORD"),
                environment.get("FEED_AGREEMENT_DATABASE"),
                "true".equals(environment.get("FEED_AGREEMENT_PRODUCTION")),
                forms.isBlank() ? List.of() : List.of(forms.trim().split("\\s*,\\s*")),
                Integer.parseInt(environment.getOrDefault("FEED_AGREEMENT_BATCH", "500")));
        }
    }

    /** The counts, line by line, and the agreement lines that are not 0, by form id. */
    public record Report(Map<String, Long> lines, Map<String, Map<String, Long>> disagreementsByForm) {

        /** Whether every agreement line reads 0. */
        public boolean agrees() {
            return lines.entrySet().stream().filter(line -> line.getKey().startsWith(AGREE))
                .allMatch(line -> line.getValue() == 0);
        }

        public long count(String line) {
            return lines.getOrDefault(line, 0L);
        }

        /** The report as text: one "name: count" line each, then the forms that disagree. */
        public String text() {
            StringBuilder text = new StringBuilder();
            lines.forEach((name, count) -> text.append(name).append(" = ").append(count).append('\n'));
            disagreementsByForm.forEach((form, counts) -> {
                text.append("form ").append(form).append(':');
                counts.forEach((name, count) -> text.append(' ').append(name).append(' ').append(count).append(';'));
                text.append('\n');
            });
            return text.toString();
        }
    }

    private final Normalizing normalizing;
    private final Map<String, Long> lines = new LinkedHashMap<>();
    private final Map<String, Map<String, Long>> byForm = new TreeMap<>();

    private FeedAgreement(Normalizing normalizing) {
        this.normalizing = normalizing;
        for (String line : List.of("heads", "submissions without a head", "heads whose document is not an object",
            "oracle mapped values", "oracle unmapped values", "oracle empty arrays (apart)", "oracle rows",
            "normalized values", "normalized unmapped values", "normalized empty arrays (apart)", "normalized rows",
            "heads using a row id twice (compared without row ids)", WHOLE, "oracle values inside values kept whole",
            "oracle rows inside values kept whole",
            MAPPED_MISSING, MAPPED_EXTRA, MAPPED_DIFFERENT, UNMAPPED_MISSING, UNMAPPED_EXTRA, UNMAPPED_DIFFERENT,
            ROWS_MISSING, ROWS_EXTRA, FORM_DIFFERS)) {
            lines.put(line, 0L);
        }
    }

    /**
     * Connects to the URL the settings give, after the guards that need no connection: a database is named, and the
     * host is not production's unless production is allowed. The caller closes the connection; {@link #run} checks
     * that it reached the database named.
     */
    public static Connection connect(Settings settings) throws SQLException {
        requireNamedDatabase(settings);
        String host = host(settings.jdbcUrl()).toLowerCase(Locale.ROOT);
        if (PRODUCTION_HOSTS.contains(host) && !settings.production()) {
            throw new IllegalArgumentException("Host " + host + " is production's database host: set "
                + "FEED_AGREEMENT_PRODUCTION=true to measure production");
        }
        return DriverManager.getConnection(settings.jdbcUrl(), settings.user(), settings.password());
    }

    private static void requireNamedDatabase(Settings settings) {
        if (settings.database() == null || settings.database().isBlank()) {
            throw new IllegalArgumentException("Name the database to measure in FEED_AGREEMENT_DATABASE");
        }
    }

    private static String host(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("The agreement harness reads PostgreSQL only");
        }
        String host = URI.create(jdbcUrl.substring("jdbc:".length())).getHost();
        return host == null ? "" : host;
    }

    /** The run's database is the one named, and production's only when production is allowed. */
    private static void requireTheDatabaseNamed(Statement statement, Settings settings) throws SQLException {
        requireNamedDatabase(settings);
        try (ResultSet rs = statement.executeQuery("SELECT current_database()")) {
            rs.next();
            String connected = rs.getString(1);
            if (!connected.equals(settings.database())) {
                throw new IllegalStateException("Connected to database " + connected + ", not to "
                    + settings.database() + ", the database named in FEED_AGREEMENT_DATABASE");
            }
            if (PRODUCTION_DATABASES.contains(connected) && !settings.production()) {
                throw new IllegalStateException("Database " + connected + " is production's database: set "
                    + "FEED_AGREEMENT_PRODUCTION=true to measure production");
            }
        }
    }

    /** Measures every head of the forms named (none: every head) with the real normalizer. */
    public static Report run(Connection connection, Settings settings) throws SQLException, IOException {
        return run(connection, settings, SubmissionNormalizer::normalize);
    }

    /** Measures every head with the given normalizer side; read-only, in one snapshot, rolled back at the end. */
    public static Report run(Connection connection, Settings settings, Normalizing normalizing)
        throws SQLException, IOException {
        String walk = Files.readString(ORACLE_WALK);
        String header = Files.readString(ORACLE_HEADER);
        boolean autoCommit = connection.getAutoCommit();
        int isolation = connection.getTransactionIsolation();
        connection.setAutoCommit(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        connection.setReadOnly(true);
        try {
            try (Statement statement = connection.createStatement()) {
                requireTheDatabaseNamed(statement, settings);
                for (String setting : SESSION) {
                    statement.execute(setting);
                }
                requireSetting(statement, "SHOW transaction_isolation", "repeatable read");
                requireSetting(statement, "SHOW transaction_read_only", "on");
            }
            FeedAgreement agreement = new FeedAgreement(normalizing);
            NamedParameterJdbcTemplate jdbc =
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));
            Map<String, FormDefinitionJdbcReader.Snapshot> definitions = new FormDefinitionJdbcReader(jdbc,
                new ObjectMapper()).readAll();
            // the normalizer's side reads heads through the stored-submission read seam, as the publisher does
            StoredSubmissionJdbcReader submissions = new StoredSubmissionJdbcReader(jdbc, new ObjectMapper());
            long withoutHead = 0;
            String after = "";
            while (true) {
                List<StoredSubmissionJdbcReader.HeadVersion> page =
                    submissions.headVersionsAfter(after, settings.batchSize());
                if (page.isEmpty()) {
                    break;
                }
                after = page.get(page.size() - 1).submissionId();
                List<String> ids = new ArrayList<>();
                for (StoredSubmissionJdbcReader.HeadVersion head : page) {
                    if (!settings.formIds().isEmpty()
                        && !settings.formIds().contains(head.formId() == null ? NO_FORM : head.formId())) {
                        continue;
                    }
                    if (head.version() == null) {
                        withoutHead++;
                    } else {
                        ids.add(head.submissionId());
                    }
                }
                List<Head> heads = submissions.heads(ids).stream()
                    .map(head -> new Head(head.submission().id(), head.submission().formId(), head.version(),
                        head.submission().formVersionId(), head.submission().formData()))
                    .sorted(Comparator.comparing(Head::submissionId))
                    .toList();
                if (!heads.isEmpty()) {
                    agreement.compare(connection, walk, header, heads, definitions);
                }
            }
            agreement.add("submissions without a head", withoutHead);
            return new Report(Collections.unmodifiableMap(new LinkedHashMap<>(agreement.lines)),
                Collections.unmodifiableMap(new TreeMap<>(agreement.byForm)));
        } finally {
            connection.rollback();
            connection.setReadOnly(false);
            connection.setTransactionIsolation(isolation);
            connection.setAutoCommit(autoCommit);
        }
    }

    private static void requireSetting(Statement statement, String show, String expected) throws SQLException {
        try (ResultSet rs = statement.executeQuery(show)) {
            rs.next();
            if (!expected.equals(rs.getString(1))) {
                throw new IllegalStateException(show + " is not " + expected + ": the harness runs read-only only");
            }
        }
    }

    /** One head of the journal, with the ids of its form and pinned version, as the read seam resolved them. */
    private record Head(String submissionId, String formId, long version, String formVersionId, String formData) {
    }

    /** A value as compared: its row (null outside every row), its field id or data path, and its JSON, exact. */
    private record Value(String rowId, String key, String json) {
    }

    /** A value of the oracle: its row, data path, JSON, and the field it is expected under (null: unmapped). */
    private record Leaf(String rowId, String path, String json, String fieldId) {
    }

    /** A row of the oracle: its id, the row it is nested in, and its section's data path. */
    private record OracleRow(String id, String parentId, String path) {
    }

    /** What the oracle holds of one head. */
    private static final class OracleSide {
        final List<Leaf> leaves = new ArrayList<>();
        final List<OracleRow> rows = new ArrayList<>();
        String formId;
        String formVersionId;
    }

    /** A value the normalizer keeps whole: its row and data path. */
    private record Whole(String rowId, String path) {
    }

    private void compare(Connection connection, String walk, String header, List<Head> heads,
                         Map<String, FormDefinitionJdbcReader.Snapshot> definitions) throws SQLException {
        Map<String, OracleSide> oracle = new HashMap<>();
        heads.forEach(head -> oracle.put(head.submissionId(), new OracleSide()));
        Array ids = connection.createArrayOf("text", heads.stream().map(Head::submissionId).toArray());
        Array versions = connection.createArrayOf("bigint", heads.stream().map(Head::version).toArray());
        try (PreparedStatement statement = connection.prepareStatement(walk)) {
            statement.setArray(1, ids);
            statement.setArray(2, versions);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    OracleSide side = oracle.get(rs.getString("submission_id"));
                    if (rs.getBoolean("is_row")) {
                        side.rows.add(new OracleRow(rs.getString("row_id"), rs.getString("parent_row_id"),
                            rs.getString("path")));
                    } else {
                        side.leaves.add(new Leaf(rs.getString("row_id"), rs.getString("path"),
                            exact(rs.getString("value")), rs.getString("field_id")));
                    }
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(header)) {
            statement.setArray(1, ids);
            statement.setArray(2, versions);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    OracleSide side = oracle.get(rs.getString("submission_id"));
                    side.formId = rs.getString("form_id");
                    side.formVersionId = rs.getString("form_version_id");
                }
            }
        }
        for (Head head : heads) {
            FormDefinitionJdbcReader.Snapshot definition = head.formId() == null ? null : definitions.get(head.formId());
            NormalizedSubmission normalized = normalizing.apply(
                new StoredSubmission(head.submissionId(), head.formId(), head.formVersionId(), head.formData()),
                definition == null ? null : definition.definition());
            compare(head, oracle.get(head.submissionId()), normalized);
        }
    }

    private void compare(Head head, OracleSide oracle, NormalizedSubmission normalized) {
        add("heads", 1);
        normalized.flags().forEach((flag, count) -> add("normalization flags, " + flag, count));

        // the normalizer's side: mapped by field id, unmapped by data path, what it keeps whole apart
        List<Value> mapped = new ArrayList<>();
        List<Value> unmapped = new ArrayList<>();
        List<Whole> wholes = new ArrayList<>();
        List<String> rows = new ArrayList<>();
        collect(null, normalized.values(), normalized.unmapped(), mapped, unmapped, wholes);
        for (NormalizedRow row : normalized.rows()) {
            rows.add(row.id());
            collect(row.id(), row.values(), row.unmapped(), mapped, unmapped, wholes);
        }
        add("normalized rows", rows.size());

        // the oracle's side, without what lies inside a value the normalizer keeps whole
        Set<String> rowsInside = rowsInside(oracle.rows, wholes);
        List<Value> expectedMapped = new ArrayList<>();
        List<Value> expectedUnmapped = new ArrayList<>();
        for (Leaf leaf : oracle.leaves) {
            if (rowsInside.contains(leaf.rowId()) || insideAWhole(leaf.rowId(), leaf.path(), wholes)) {
                add("oracle values inside values kept whole", 1);
            } else if (leaf.json().equals("[]")) {
                add("oracle empty arrays (apart)", 1);
            } else {
                if (leaf.path().isEmpty()) {
                    add("heads whose document is not an object", 1);
                }
                if (leaf.fieldId() != null) {
                    expectedMapped.add(new Value(leaf.rowId(), leaf.fieldId(), leaf.json()));
                } else {
                    expectedUnmapped.add(new Value(leaf.rowId(), leaf.path(), leaf.json()));
                }
            }
        }
        List<String> headRows = new ArrayList<>();
        for (OracleRow row : oracle.rows) {
            if (rowsInside.contains(row.id())) {
                add("oracle rows inside values kept whole", 1);
            } else {
                headRows.add(row.id());
            }
        }
        add("oracle mapped values", expectedMapped.size());
        add("oracle unmapped values", expectedUnmapped.size());
        add("oracle rows", headRows.size());

        boolean rowIdUsedTwice = new HashSet<>(headRows).size() < headRows.size();
        if (rowIdUsedTwice) {
            add("heads using a row id twice (compared without row ids)", 1);
        }
        long[] mappedCounts = difference(expectedMapped, mapped, rowIdUsedTwice);
        long[] unmappedCounts = difference(expectedUnmapped, unmapped, rowIdUsedTwice);

        long rowsMissing;
        long rowsExtra;
        if (rowIdUsedTwice) {
            rowsMissing = Math.max(0, headRows.size() - rows.size());
            rowsExtra = Math.max(0, rows.size() - headRows.size());
        } else {
            Set<String> normalizedRows = new HashSet<>(rows);
            Set<String> expectedRows = new HashSet<>(headRows);
            rowsMissing = expectedRows.stream().filter(id -> !normalizedRows.contains(id)).count();
            rowsExtra = normalizedRows.stream().filter(id -> !expectedRows.contains(id)).count();
        }
        long header = Objects.equals(oracle.formId, normalized.formId())
            && Objects.equals(oracle.formVersionId, normalized.formVersionId()) ? 0 : 1;

        String form = head.formId();
        disagree(form, "mapped missing", MAPPED_MISSING, mappedCounts[0]);
        disagree(form, "mapped extra", MAPPED_EXTRA, mappedCounts[1]);
        disagree(form, "mapped different", MAPPED_DIFFERENT, mappedCounts[2]);
        disagree(form, "unmapped missing", UNMAPPED_MISSING, unmappedCounts[0]);
        disagree(form, "unmapped extra", UNMAPPED_EXTRA, unmappedCounts[1]);
        disagree(form, "unmapped different", UNMAPPED_DIFFERENT, unmappedCounts[2]);
        disagree(form, "rows missing", ROWS_MISSING, rowsMissing);
        disagree(form, "rows extra", ROWS_EXTRA, rowsExtra);
        disagree(form, "form or version", FORM_DIFFERS, header);
    }

    /**
     * Missing, extra and different values between the expected and the normalized multisets: a missing and an extra
     * value at one row and key (field id or data path) count once more as different.
     */
    private static long[] difference(List<Value> expected, List<Value> actual, boolean withoutRows) {
        Map<Value, Integer> want = multiset(expected, withoutRows);
        Map<Value, Integer> have = multiset(actual, withoutRows);
        Set<Value> keys = new HashSet<>(want.keySet());
        keys.addAll(have.keySet());
        long missing = 0;
        long extra = 0;
        Map<List<String>, long[]> byPlace = new HashMap<>();
        for (Value value : keys) {
            int difference = want.getOrDefault(value, 0) - have.getOrDefault(value, 0);
            long[] place = byPlace.computeIfAbsent(Arrays.asList(value.rowId(), value.key()), p -> new long[2]);
            if (difference > 0) {
                missing += difference;
                place[0] += difference;
            } else if (difference < 0) {
                extra -= difference;
                place[1] -= difference;
            }
        }
        long different = byPlace.values().stream().mapToLong(place -> Math.min(place[0], place[1])).sum();
        return new long[]{missing, extra, different};
    }

    /** One container's values, by kind; empty arrays counted apart, values kept whole set apart. */
    private void collect(String rowId, List<FieldValue> values, List<UnmappedValue> unmappedValues, List<Value> mapped,
                         List<Value> unmapped, List<Whole> wholes) {
        for (FieldValue value : values) {
            add("normalized values", 1);
            if (keep(rowId, value.path(), value.value(), wholes)) {
                mapped.add(new Value(rowId, value.fieldId(), StoredJson.write(value.value())));
            }
        }
        for (UnmappedValue value : unmappedValues) {
            add("normalized unmapped values", 1);
            add("normalized unmapped values, " + value.reason(), 1);
            if (keep(rowId, value.path(), value.value(), wholes)) {
                unmapped.add(new Value(rowId, value.path(), StoredJson.write(value.value())));
            }
        }
    }

    /** Whether a normalized value is compared: not an empty array, and not a value kept whole. */
    private boolean keep(String rowId, String path, JsonNode value, List<Whole> wholes) {
        if (value.equals(EMPTY_ARRAY)) {
            add("normalized empty arrays (apart)", 1);
            return false;
        }
        // a document that is not an object is one value to both sides
        if (!path.isEmpty() && holdsObjects(value)) {
            add(WHOLE, 1);
            wholes.add(new Whole(rowId, path));
            return false;
        }
        return true;
    }

    private static boolean holdsObjects(JsonNode value) {
        if (value.isObject()) {
            return true;
        }
        if (value.isArray()) {
            for (JsonNode element : value) {
                if (element.isObject()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether a place (a row, or outside every row, and a data path) lies in a value kept whole. */
    private static boolean insideAWhole(String rowId, String path, List<Whole> wholes) {
        for (Whole whole : wholes) {
            if (Objects.equals(whole.rowId(), rowId)
                && (path.equals(whole.path()) || path.startsWith(whole.path() + "."))) {
                return true;
            }
        }
        return false;
    }

    /** The oracle's rows that lie in a value the normalizer keeps whole, nested ones included. */
    private static Set<String> rowsInside(List<OracleRow> rows, List<Whole> wholes) {
        Set<String> inside = new HashSet<>();
        if (wholes.isEmpty()) {
            return inside;
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (OracleRow row : rows) {
                if (!inside.contains(row.id())
                    && (insideAWhole(row.parentId(), row.path(), wholes) || inside.contains(row.parentId()))) {
                    inside.add(row.id());
                    grew = true;
                }
            }
        }
        return inside;
    }

    /** The values as a multiset, by row or, for a head using a row id twice, without rows. */
    private static Map<Value, Integer> multiset(List<Value> values, boolean withoutRows) {
        Map<Value, Integer> multiset = new HashMap<>();
        for (Value value : values) {
            multiset.merge(withoutRows ? new Value(null, value.key(), value.json()) : value, 1, Integer::sum);
        }
        return multiset;
    }

    /** A JSON text written as the normalizer writes a value: parsed exactly, written without spacing. */
    private static String exact(String json) {
        try {
            return StoredJson.write(StoredJson.parse(json));
        } catch (JsonProcessingException e) {
            // the oracle's values are jsonb, which always prints as JSON
            throw new IllegalStateException("The oracle returned a value that is not JSON", e);
        }
    }

    private void add(String line, long count) {
        lines.merge(line, count, Long::sum);
    }

    private void disagree(String formId, String shortName, String line, long count) {
        add(line, count);
        if (count > 0) {
            byForm.computeIfAbsent(formId == null ? NO_FORM : formId, f -> new LinkedHashMap<>())
                .merge(shortName, count, Long::sum);
        }
    }
}
