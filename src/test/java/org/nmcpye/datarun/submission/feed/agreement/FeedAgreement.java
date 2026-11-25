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
 * <p>The oracle ({@code scripts/feed/sql/oracle-walk.sql} and {@code oracle-header.sql}) reads each head with no form
 * definition: every value at its data path in its row, rows keyed by the contract's recipe, and the ids of the form and
 * form version the head names. The normalizer reads the same head with the stored definitions
 * ({@link FormDefinitionJdbcReader}), as the publisher will. For each head the two are compared as multisets of
 * values by row, data path and JSON value (numbers exact, an object's keys in any order), rows by id, and the form and
 * form version ids. Empty arrays are counted apart: without a definition they are zero rows or an empty
 * multi-select. A head whose rows use one id twice is compared without row ids, since the oracle cannot give the later
 * rows their derived ids.</p>
 *
 * <p>The lines whose names start with "agree:" must read 0; the others are for reference. The normalizer keeps whole
 * what a definition gives one field (an object, rows at a path no version defines), and the oracle walks into it:
 * such values are counted in "values holding objects or rows", and each shows as missing and extra values.</p>
 *
 * <p><b>Safety.</b> One {@code REPEATABLE READ READ ONLY} transaction, so every count comes from one snapshot and
 * nothing can be written; {@code work_mem} 8MB, no parallel workers, no JIT, a statement timeout, a lock timeout, and
 * an idle timeout; batches of heads by submission id; it refuses a host whose name says production. It prints counts
 * and form uids, never a value or another id.</p>
 */
public final class FeedAgreement {

    /** Every line that must read 0 starts with this. */
    public static final String AGREE = "agree: ";

    private static final Path ORACLE_WALK = Path.of("scripts", "feed", "sql", "oracle-walk.sql");
    private static final Path ORACLE_HEADER = Path.of("scripts", "feed", "sql", "oracle-header.sql");

    private static final String HEADS = """
        SELECT s.id, s.template_uid, h.version, h.metadata ->> 'template_version_uid' AS form_version_uid,
               h.form_data::text AS form_data
        FROM data_submission s
        CROSS JOIN LATERAL (SELECT r.version, r.metadata, r.form_data FROM journal.submission_revision r
                            WHERE r.submission_id = s.id ORDER BY r.version DESC LIMIT 1) h
        WHERE s.id > ? AND (cardinality(?::text[]) = 0 OR s.template_uid = ANY (?::text[]))
        ORDER BY s.id
        LIMIT ?""";

    private static final String WITHOUT_HEAD = """
        SELECT count(*) FROM data_submission s
        WHERE (cardinality(?::text[]) = 0 OR s.template_uid = ANY (?::text[]))
          AND NOT EXISTS (SELECT 1 FROM journal.submission_revision r WHERE r.submission_id = s.id)""";

    private static final List<String> SESSION = List.of(
        "SET LOCAL work_mem = '8MB'",
        "SET LOCAL max_parallel_workers_per_gather = 0",
        "SET LOCAL jit = off",
        "SET LOCAL statement_timeout = '300s'",
        "SET LOCAL lock_timeout = '5s'",
        "SET LOCAL idle_in_transaction_session_timeout = '10min'");

    private static final JsonNode EMPTY_ARRAY = new ObjectMapper().createArrayNode();

    /** How the normalizer side reads a head; the real normalizer, or a faulty one to prove the harness. */
    public interface Normalizing extends BiFunction<StoredSubmission, FormDefinition, NormalizedSubmission> {
    }

    /** Where to connect, which forms (none: all), and how many heads per batch. */
    public record Settings(String jdbcUrl, String user, String password, List<String> formUids, int batchSize) {

        public Settings {
            Objects.requireNonNull(jdbcUrl, "jdbcUrl");
            formUids = formUids == null ? List.of() : List.copyOf(formUids);
            if (batchSize < 1) {
                throw new IllegalArgumentException("a batch holds at least one head");
            }
        }

        /** From the environment: FEED_AGREEMENT_JDBC_URL, _USER, _PASSWORD, _FORMS (comma-separated), _BATCH. */
        public static Settings fromEnvironment(Map<String, String> environment) {
            String forms = environment.getOrDefault("FEED_AGREEMENT_FORMS", "");
            return new Settings(
                environment.get("FEED_AGREEMENT_JDBC_URL"),
                environment.get("FEED_AGREEMENT_USER"),
                environment.get("FEED_AGREEMENT_PASSWORD"),
                forms.isBlank() ? List.of() : List.of(forms.trim().split("\\s*,\\s*")),
                Integer.parseInt(environment.getOrDefault("FEED_AGREEMENT_BATCH", "500")));
        }
    }

    /** The counts, line by line, and the agreement lines that are not 0, by form uid. */
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
            "oracle values", "oracle empty arrays (apart)", "oracle rows", "normalized values",
            "normalized unmapped values", "normalized empty arrays (apart)", "normalized rows",
            "heads using a row id twice (compared without row ids)",
            "values holding objects or rows (kept whole; the oracle walks into them)",
            AGREE + "values missing (in the head, not normalized)", AGREE + "values extra (normalized, not in the head)",
            AGREE + "values different (a missing and an extra at one row and path)",
            AGREE + "head rows missing from the normalized rows", AGREE + "normalized rows not in the head",
            AGREE + "form or form version differs from the head's")) {
            lines.put(line, 0L);
        }
    }

    /** Connects to the database the settings name, refusing a production host; the caller closes it. */
    public static Connection connect(Settings settings) throws SQLException {
        String host = host(settings.jdbcUrl());
        if (host.toLowerCase(Locale.ROOT).contains("prod")) {
            throw new IllegalArgumentException("The agreement harness never runs on a production host");
        }
        return DriverManager.getConnection(settings.jdbcUrl(), settings.user(), settings.password());
    }

    private static String host(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("The agreement harness reads PostgreSQL only");
        }
        String host = URI.create(jdbcUrl.substring("jdbc:".length())).getHost();
        return host == null ? "" : host;
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
                for (String setting : SESSION) {
                    statement.execute(setting);
                }
                requireSetting(statement, "SHOW transaction_isolation", "repeatable read");
                requireSetting(statement, "SHOW transaction_read_only", "on");
            }
            FeedAgreement agreement = new FeedAgreement(normalizing);
            Map<String, FormDefinitionJdbcReader.Snapshot> definitions = new FormDefinitionJdbcReader(
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true)), new ObjectMapper())
                .readAll();
            Array forms = connection.createArrayOf("text", settings.formUids().toArray());
            try (PreparedStatement withoutHead = connection.prepareStatement(WITHOUT_HEAD)) {
                withoutHead.setArray(1, forms);
                withoutHead.setArray(2, forms);
                try (ResultSet rs = withoutHead.executeQuery()) {
                    rs.next();
                    agreement.add("submissions without a head", rs.getLong(1));
                }
            }
            String after = "";
            while (true) {
                List<Head> heads = heads(connection, forms, after, settings.batchSize());
                if (heads.isEmpty()) {
                    break;
                }
                agreement.compare(connection, walk, header, heads, definitions);
                after = heads.get(heads.size() - 1).submissionId();
            }
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

    /** One head of the journal and the submission's form uid. */
    private record Head(String submissionId, String formUid, long version, String formVersionUid, String formData) {
    }

    private static List<Head> heads(Connection connection, Array forms, String after, int limit) throws SQLException {
        List<Head> heads = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(HEADS)) {
            statement.setString(1, after);
            statement.setArray(2, forms);
            statement.setArray(3, forms);
            statement.setInt(4, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    heads.add(new Head(rs.getString("id"), rs.getString("template_uid"), rs.getLong("version"),
                        rs.getString("form_version_uid"), rs.getString("form_data")));
                }
            }
        }
        return heads;
    }

    /** A value as compared: its row (null outside every row), its data path, and its JSON written exactly. */
    private record Value(String rowId, String path, String json) {
    }

    /** What one side holds of one head. */
    private static final class Side {
        final List<Value> values = new ArrayList<>();
        final List<String> rows = new ArrayList<>();
        String formId;
        String formVersionId;
    }

    private void compare(Connection connection, String walk, String header, List<Head> heads,
                         Map<String, FormDefinitionJdbcReader.Snapshot> definitions) throws SQLException {
        Map<String, Side> oracle = new HashMap<>();
        heads.forEach(head -> oracle.put(head.submissionId(), new Side()));
        Array ids = connection.createArrayOf("text", heads.stream().map(Head::submissionId).toArray());
        Array versions = connection.createArrayOf("bigint", heads.stream().map(Head::version).toArray());
        try (PreparedStatement statement = connection.prepareStatement(walk)) {
            statement.setArray(1, ids);
            statement.setArray(2, versions);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Side side = oracle.get(rs.getString("submission_id"));
                    if (rs.getBoolean("is_row")) {
                        side.rows.add(rs.getString("row_id"));
                    } else {
                        String json = exact(rs.getString("value"));
                        if (json.equals("[]")) {
                            add("oracle empty arrays (apart)", 1);
                        } else {
                            side.values.add(new Value(rs.getString("row_id"), rs.getString("path"), json));
                            if (rs.getString("path").isEmpty()) {
                                add("heads whose document is not an object", 1);
                            }
                        }
                    }
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(header)) {
            statement.setArray(1, ids);
            statement.setArray(2, versions);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Side side = oracle.get(rs.getString("submission_id"));
                    side.formId = rs.getString("form_id");
                    side.formVersionId = rs.getString("form_version_id");
                }
            }
        }
        for (Head head : heads) {
            FormDefinitionJdbcReader.Snapshot definition = definitions.get(head.formUid());
            NormalizedSubmission normalized = normalizing.apply(
                new StoredSubmission(head.submissionId(), head.formUid(), head.formVersionUid(), head.formData()),
                definition == null ? null : definition.definition());
            compare(head, oracle.get(head.submissionId()), normalized);
        }
    }

    private void compare(Head head, Side oracle, NormalizedSubmission normalized) {
        add("heads", 1);
        Side side = new Side();
        side.formId = normalized.formId();
        side.formVersionId = normalized.formVersionId();
        collect(side, null, normalized.values(), normalized.unmapped());
        for (NormalizedRow row : normalized.rows()) {
            side.rows.add(row.id());
            collect(side, row.id(), row.values(), row.unmapped());
        }
        normalized.flags().forEach((flag, count) -> add("normalization flags, " + flag, count));

        add("oracle values", oracle.values.size());
        add("oracle rows", oracle.rows.size());
        add("normalized rows", side.rows.size());

        boolean rowIdUsedTwice = new HashSet<>(oracle.rows).size() < oracle.rows.size();
        if (rowIdUsedTwice) {
            add("heads using a row id twice (compared without row ids)", 1);
        }
        Map<Value, Integer> expected = multiset(oracle.values, rowIdUsedTwice);
        Map<Value, Integer> actual = multiset(side.values, rowIdUsedTwice);
        long missing = 0;
        long extra = 0;
        Map<List<String>, long[]> byPlace = new HashMap<>();
        Set<Value> keys = new HashSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (Value value : keys) {
            int difference = expected.getOrDefault(value, 0) - actual.getOrDefault(value, 0);
            long[] place = byPlace.computeIfAbsent(Arrays.asList(value.rowId(), value.path()), p -> new long[2]);
            if (difference > 0) {
                missing += difference;
                place[0] += difference;
            } else if (difference < 0) {
                extra -= difference;
                place[1] -= difference;
            }
        }
        long different = byPlace.values().stream().mapToLong(place -> Math.min(place[0], place[1])).sum();

        long rowsMissing;
        long rowsExtra;
        if (rowIdUsedTwice) {
            rowsMissing = Math.max(0, oracle.rows.size() - side.rows.size());
            rowsExtra = Math.max(0, side.rows.size() - oracle.rows.size());
        } else {
            Set<String> normalizedRows = new HashSet<>(side.rows);
            Set<String> headRows = new HashSet<>(oracle.rows);
            rowsMissing = headRows.stream().filter(id -> !normalizedRows.contains(id)).count();
            rowsExtra = normalizedRows.stream().filter(id -> !headRows.contains(id)).count();
        }
        long header = Objects.equals(oracle.formId, side.formId)
            && Objects.equals(oracle.formVersionId, side.formVersionId) ? 0 : 1;

        disagree(head.formUid(), "values missing", AGREE + "values missing (in the head, not normalized)", missing);
        disagree(head.formUid(), "values extra", AGREE + "values extra (normalized, not in the head)", extra);
        disagree(head.formUid(), "values different",
            AGREE + "values different (a missing and an extra at one row and path)", different);
        disagree(head.formUid(), "rows missing", AGREE + "head rows missing from the normalized rows", rowsMissing);
        disagree(head.formUid(), "rows extra", AGREE + "normalized rows not in the head", rowsExtra);
        disagree(head.formUid(), "form or version", AGREE + "form or form version differs from the head's", header);
    }

    /** One side's values of one container: mapped and unmapped alike, at their data paths; empty arrays apart. */
    private void collect(Side side, String rowId, List<FieldValue> values, List<UnmappedValue> unmapped) {
        for (FieldValue value : values) {
            if (keep(value.value())) {
                add("normalized values", 1);
                side.values.add(new Value(rowId, value.path(), StoredJson.write(value.value())));
            }
        }
        for (UnmappedValue value : unmapped) {
            if (keep(value.value())) {
                add("normalized unmapped values", 1);
                add("normalized unmapped values, " + value.reason(), 1);
                side.values.add(new Value(rowId, value.path(), StoredJson.write(value.value())));
            }
        }
    }

    /** Whether a normalized value is compared: not an empty array (counted apart); one holding more is counted too. */
    private boolean keep(JsonNode value) {
        if (value.equals(EMPTY_ARRAY)) {
            add("normalized empty arrays (apart)", 1);
            return false;
        }
        if (holdsObjects(value)) {
            add("values holding objects or rows (kept whole; the oracle walks into them)", 1);
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

    /** The values as a multiset, by row or, for a head using a row id twice, without rows. */
    private static Map<Value, Integer> multiset(List<Value> values, boolean withoutRows) {
        Map<Value, Integer> multiset = new HashMap<>();
        for (Value value : values) {
            multiset.merge(withoutRows ? new Value(null, value.path(), value.json()) : value, 1, Integer::sum);
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

    private void disagree(String formUid, String shortName, String line, long count) {
        add(line, count);
        if (count > 0) {
            byForm.computeIfAbsent(formUid == null ? "-" : formUid, f -> new LinkedHashMap<>())
                .merge(shortName, count, Long::sum);
        }
    }
}
