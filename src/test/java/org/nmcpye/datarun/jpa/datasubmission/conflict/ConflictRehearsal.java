package org.nmcpye.datarun.jpa.datasubmission.conflict;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.config.activation.ActivationSwitch;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.submission.normalized.StoredJson;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Conflict detection's rehearsal on a real database (spec, build order, step S2): what detection decides for every
 * stored submission re-sent unchanged, for a sample of changed copies after seeded server-side changes, and what the
 * history says of {@code _status}. It drives {@link ConflictDetection} itself, switched on, with the journal reader,
 * on the database's own data. The upload checks are not run: a copy is taken to pass them, except where a move's
 * access refusal is rehearsed.
 *
 * <p><b>Whose copy.</b> A submission's copy is held by whoever last uploaded it: the newest {@code UPLOAD} or
 * {@code LEGACY_UPLOAD} revision's actor, or a {@code HELD_RELEASE}'s uploader, holding that revision's state. For a
 * submission no upload has changed since the journal began, it is the row's last modifier, holding the first
 * revision's state. Every copy is sent as phones send it, {@code deleted} false. A holder without a base of their own
 * (a last modifier who is not the creator, or a creator whose baseline is deleted) is counted apart: after a
 * server-side change, a deleted baseline included, their copy is kept aside by design. Each copy is re-sent in two shapes: as stored
 * (apps 6.0 and later, row ids included) and with every row's structure keys removed (apps 5.2 and 5.3). Documents are
 * read as the upload routes read a request, decimals as binary doubles.</p>
 *
 * <p><b>A clone only.</b> The re-send and history passes run in read-only transactions, and the conflict store counts
 * instead of writing, but the changed-copy pass writes: its seeded change (the submission's row, its revision and its
 * outbox row, as submission review's methods will) inside its transaction, under the submission lock, then rolled back.
 * That takes row locks while it lasts and consumes sequence values (the outbox's) for good, so it runs on a clone,
 * never on a database serving phones. The operator names the database ({@link Settings#database}); the rehearsal
 * refuses to start unless {@code current_database()} is that one, and refuses production's known database host, by
 * name or address, and its database's name, with no override. Each connection runs with a 30 s statement timeout, a
 * 2 s lock timeout, 4 MB of work memory and no parallel workers, and names itself
 * {@code conflict-detection-rehearsal}.</p>
 *
 * <p><b>It reports counts only</b>: no uid, no user, no value. Detection's own ERROR lines (identifiers and failure
 * shapes, as in production) stay on; its INFO lines are silenced.</p>
 */
public final class ConflictRehearsal implements AutoCloseable {

    /**
     * What to rehearse, and where.
     *
     * @param database the database to rehearse on, named by the operator: the run refuses any other
     * @param limit    at most this many submissions in the re-send pass, or null for all
     * @param sample  how many submissions get a changed copy after a seeded server-side change
     * @param threads how many connections work at once
     * @param seed    the sample's seed: the same seed picks the same submissions
     * @param scope   an SQL {@code LIKE} pattern on uids, or null for every submission
     */
    public record Settings(String url, String user, String password, String database, Integer limit, int sample,
                           int threads, String seed, String scope) {
    }

    /** Production's database host, by name and address, as the feed's agreement harness lists them. */
    static final Set<String> PRODUCTION_HOSTS = Set.of("datarun-prod-db.lab", "datarun-prod-db", "192.168.1.111");
    /** Production's database ({@code deploy/production/compose.yml}). */
    static final Set<String> PRODUCTION_DATABASES = Set.of("nmcpdb");

    /** Counts by outcome, in a stable order. */
    public static final class Tally {
        private final Map<String, LongAdder> counts = new ConcurrentHashMap<>();

        void add(String key) {
            counts.computeIfAbsent(key, k -> new LongAdder()).increment();
        }

        public long get(String key) {
            LongAdder count = counts.get(key);
            return count == null ? 0 : count.sum();
        }

        /** The sum over every key that starts with the prefix. */
        public long sum(String prefix) {
            return counts.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .mapToLong(entry -> entry.getValue().sum()).sum();
        }

        /** Every count, by key in order. */
        public Map<String, Long> sorted() {
            Map<String, Long> sorted = new TreeMap<>();
            counts.forEach((key, count) -> sorted.put(key, count.sum()));
            return sorted;
        }
    }

    /** As the upload routes read a request body: decimals as binary doubles. */
    private static final ObjectMapper REQUEST = new ObjectMapper();
    private static final String SESSION = "SELECT set_config('statement_timeout', '30s', false), "
        + "set_config('lock_timeout', '2s', false), set_config('work_mem', '4MB', false), "
        + "set_config('max_parallel_workers_per_gather', '0', false), "
        + "set_config('idle_in_transaction_session_timeout', '60s', false)";
    private static final List<String> SEEDED_KINDS = List.of("EDIT", "MOVE", "DELETE", "ADMIN_DELETE");
    private static final String PHONE_CHANGE = "conflict_rehearsal_phone_change";
    private static final String NO_BASE = "no base of their own";
    private static final String OWN_BASE = "own base";
    private static final String SERVER_CHANGE = "conflict_rehearsal_server_change";
    private static final int PAYLOAD_BATCH = 200;

    private final Settings settings;
    private final HikariDataSource dataSource;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate readWrite;
    private final CountingStore store;
    private final ConflictDetection detection;
    private final Map<String, TemplateVersionContext> templates = new ConcurrentHashMap<>();
    private final List<String> report = new ArrayList<>();

    /** The re-send pass, by shape and outcome. */
    public final Tally unchanged = new Tally();
    /** The changed-copy pass, by seeded change, shape and outcome. */
    public final Tally changed = new Tally();
    /** Whose copies the passes re-sent, by how the holder was found. */
    public final Tally holders = new Tally();
    /** The {@code _status} measures. */
    public final Tally status = new Tally();

    public ConflictRehearsal(Settings settings) {
        requireAClone(settings);
        this.settings = settings;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.url());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.threads() + 1);
        config.setConnectionInitSql(SESSION);
        config.setPoolName("conflict-detection-rehearsal");
        config.addDataSourceProperty("ApplicationName", "conflict-detection-rehearsal");
        this.dataSource = new HikariDataSource(config);
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        DataSourceTransactionManager transactions = new DataSourceTransactionManager(dataSource);
        transactions.setEnforceReadOnly(true);
        this.readOnly = new TransactionTemplate(transactions);
        this.readOnly.setReadOnly(true);
        this.readWrite = new TransactionTemplate(transactions);
        this.store = new CountingStore(jdbc);
        this.detection = new ConflictDetection(switchedOn(), new SubmissionRevisionReader(jdbc), store, jdbc,
            new ObjectMapper());
    }

    /** Runs the three passes and returns the report ({@link #report}), each line also printed as it comes. */
    public List<String> run() {
        Level detectionLevel = quietDetection(Level.ERROR);
        try {
            requireTheDatabaseNamed();
            requireTheSchema();
            line("database " + jdbc.getJdbcTemplate().queryForObject("SELECT current_database()", String.class)
                + ", " + jdbc.getJdbcTemplate().queryForObject("SHOW server_version", String.class)
                + (settings.scope() == null ? "" : ", submissions in the given scope only"));
            reSendEveryHeadUnchanged();
            changeASample();
            statusEvidence();
        } finally {
            quietDetection(detectionLevel);
        }
        return report();
    }

    /** The report so far: every line printed. */
    public List<String> report() {
        synchronized (report) {
            return List.copyOf(report);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

    // ------------------------------------------------------------------------------------------ pass 1

    private void reSendEveryHeadUnchanged() {
        List<String> ids = jdbc.queryForList(
            "SELECT id FROM data_submission WHERE " + scope() + " ORDER BY id"
                + (settings.limit() == null ? "" : " LIMIT " + settings.limit()),
            scopeParameters(), String.class);
        long started = System.nanoTime();
        AtomicLong done = new AtomicLong();
        inParallel(ids, id -> {
            readOnly.executeWithoutResult(transaction -> {
                transaction.setRollbackOnly();
                reSendUnchanged(id);
            });
            long count = done.incrementAndGet();
            if (count % 5_000 == 0) {
                line("re-sent " + count + " of " + ids.size() + " submissions so far");
            }
        });
        line("pass 1, every submission re-sent unchanged: " + ids.size() + " submissions in "
            + seconds(started) + " s");
        holders.sorted().forEach((key, count) -> line("  copy holder " + key + ": " + count));
        unchanged.sorted().forEach((key, count) -> line("  " + key + ": " + count));
        line("  expected: kept aside 0 for a holder with a base of their own, failed 0; met: "
            + (unchangedConflicts() == 0 && unchangedFailures() == 0 ? "yes" : "no"));
    }

    /**
     * Conflicts the re-send pass would have opened for a holder with a base of their own: kept aside, for a server-side
     * change or a detection error. A holder without one is kept aside by design after a server-side change.
     */
    public long unchangedConflicts() {
        return unchanged.counts.keySet().stream().filter(key -> key.contains("kept aside") && key.contains(OWN_BASE))
            .mapToLong(unchanged::get).sum();
    }

    /** Re-sends that failed outright, outside detection's own handling. */
    public long unchangedFailures() {
        return unchanged.counts.keySet().stream().filter(key -> key.contains("failed"))
            .mapToLong(unchanged::get).sum();
    }

    private void reSendUnchanged(String id) {
        Optional<Head> head = head(id, false);
        if (head.isEmpty()) {
            unchanged.add("skipped: gone since the pass began");
            return;
        }
        Optional<Holder> holder = holder(head.get(), holders);
        if (holder.isEmpty()) {
            return;
        }
        Optional<SubmissionRevisionReader.State> state = state(head.get(), holder.get());
        if (state.isEmpty()) {
            unchanged.add("skipped: the holder's revision is unreadable");
            return;
        }
        String asStored = state.get().document();
        if (onlyStatusDiffers(head.get().document(), asStored)) {
            status.add("heads whose re-sent copy differs from the stored document only in _status");
        }
        String base = holder.get().ownBase(state.get()) ? OWN_BASE : NO_BASE;
        for (Shape shape : Shape.values()) {
            JsonNode document = shape.of(parse(asStored));
            unchanged.add(shape.label + ", " + base + ", "
                + outcome(head.get(), holder.get(), state.get(), document, null));
        }
    }

    // ------------------------------------------------------------------------------------------ pass 2

    private void changeASample() {
        List<String> ids = jdbc.queryForList(
            "SELECT id FROM data_submission WHERE " + scope() + " ORDER BY md5(id || :seed) LIMIT :sample",
            scopeParameters().addValue("seed", settings.seed()).addValue("sample", settings.sample()),
            String.class);
        long started = System.nanoTime();
        // Each kind in turn, by the sample's order, so the same seed seeds the same changes.
        Map<String, String> kinds = new ConcurrentHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            kinds.put(ids.get(i), SEEDED_KINDS.get(i % SEEDED_KINDS.size()));
        }
        Tally sampleHolders = new Tally();
        inParallel(ids, id -> {
            String kind = kinds.get(id);
            try {
                readWrite.executeWithoutResult(transaction -> {
                    transaction.setRollbackOnly();
                    changeOne(id, kind, sampleHolders);
                });
            } catch (RuntimeException failure) {
                changed.add(kind + ", failed: " + FailureShape.of(failure));
            }
        });
        line("pass 2, a changed copy after a seeded server-side change: " + ids.size() + " submissions in "
            + seconds(started) + " s");
        sampleHolders.sorted().forEach((key, count) -> line("  copy holder " + key + ": " + count));
        changed.sorted().forEach((key, count) -> line("  " + key + ": " + count));
        line("  expected: every rehearsed copy kept aside; met: " + (changedNotKeptAside() == 0 ? "yes" : "no"));
    }

    /**
     * Changed copies the sample rehearsed that were not kept aside. A refusal standing is expected only for a holder
     * without a base of their own, whose refusals always stand.
     */
    public long changedNotKeptAside() {
        return changed.counts.keySet().stream()
            .filter(key -> !key.contains("kept aside") && !key.contains("skipped") && !key.contains(" seeded as ")
                && !(key.contains("refusal stands") && key.contains(NO_BASE)))
            .mapToLong(changed::get).sum();
    }

    private void changeOne(String id, String requested, Tally sampleHolders) {
        Optional<Head> locked = head(id, true);
        if (locked.isEmpty()) {
            changed.add(requested + ", skipped: gone since the pass began");
            return;
        }
        Head head = locked.get();
        Optional<Holder> found = holder(head, sampleHolders);
        if (found.isEmpty()) {
            changed.add(requested + ", skipped: no holder");
            return;
        }
        Holder holder = found.get();
        Optional<SubmissionRevisionReader.State> state = state(head, holder);
        if (state.isEmpty() || !(parse(state.get().document()) instanceof ObjectNode)) {
            changed.add(requested + ", skipped: no document to change");
            return;
        }
        String kind = seed(head, requested);
        if (!kind.equals(requested)) {
            changed.add(requested + " seeded as " + kind + " (" + (requested.equals("MOVE")
                ? "no other assignment in the activity" : "already deleted") + ")");
        }
        boolean ownBase = holder.ownBase(state.get());
        for (Shape shape : Shape.values()) {
            ObjectNode document = (ObjectNode) shape.of(parse(state.get().document()));
            document.put(PHONE_CHANGE, true);
            String label = kind + ", " + shape.label + ", " + holder.how() + (ownBase ? "" : ", " + NO_BASE);
            changed.add(label + ", " + outcome(head, holder, state.get(), document, null));
            if (kind.equals("MOVE")) {
                changed.add(label + ", refused for access (E4124), "
                    + outcome(head, holder, state.get(), document, ErrorCode.E4124));
            }
        }
    }

    /** Writes the server-side change, as submission review's methods will; returns the kind it could seed. */
    private String seed(Head head, String requested) {
        String kind = requested;
        String destination = null;
        if (kind.equals("MOVE")) {
            destination = jdbc.queryForList("""
                    SELECT other.uid FROM assignment other
                    JOIN assignment current ON current.activity_id = other.activity_id
                    WHERE current.uid = :assignment AND other.uid <> current.uid AND other.team_id IS NOT NULL
                      AND other.org_unit_id IS NOT NULL
                    ORDER BY other.uid LIMIT 1""",
                Map.of("assignment", String.valueOf(head.assignment())), String.class).stream().findFirst()
                .orElse(null);
            if (destination == null) {
                kind = "EDIT";
            }
        }
        if ((kind.equals("DELETE") || kind.equals("ADMIN_DELETE")) && head.deleted()) {
            kind = "EDIT";
        }
        String resolver = jdbc.queryForList("""
                SELECT u.id FROM app_user u JOIN app_user_authority a ON a.user_id = u.id
                WHERE a.authority_name = 'ROLE_ADMIN' ORDER BY u.id LIMIT 1""",
            Map.of(), String.class).stream().findFirst().orElseThrow(
                () -> new IllegalStateException("The database has no admin to name as the resolver"));
        MapSqlParameterSource parameters = new MapSqlParameterSource("id", head.id())
            .addValue("destination", destination).addValue("change", SERVER_CHANGE);
        String rowChange = switch (kind) {
            case "MOVE" -> """
                UPDATE data_submission s
                SET assignment_uid = a.uid, team_uid = t.uid, team_code = t.code, org_unit_uid = o.uid,
                    org_unit_code = o.code, org_unit_name = o.name, activity_uid = act.uid,
                    lock_version = s.lock_version + 1, last_modified_date = timezone('UTC', now())
                FROM assignment a
                JOIN team t ON t.id = a.team_id
                JOIN org_unit o ON o.id = a.org_unit_id
                JOIN activity act ON act.id = a.activity_id
                WHERE s.id = :id AND a.uid = :destination""";
            case "DELETE", "ADMIN_DELETE" -> """
                UPDATE data_submission
                SET deleted = true, deleted_at = timezone('UTC', now()), lock_version = lock_version + 1,
                    last_modified_date = timezone('UTC', now())
                WHERE id = :id""";
            default -> """
                UPDATE data_submission
                SET form_data = coalesce(form_data, '{}'::jsonb) || jsonb_build_object(CAST(:change AS text), true),
                    lock_version = lock_version + 1, last_modified_date = timezone('UTC', now())
                WHERE id = :id""";
        };
        if (jdbc.update(rowChange, parameters) != 1) {
            throw new IllegalStateException("The seeded change wrote no row");
        }
        String revisionKind = kind.equals("DELETE") || kind.equals("ADMIN_DELETE") ? "DELETE" : "UPDATE";
        jdbc.update("""
                INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command,
                                                 actor_user_id, reason, form_data, metadata)
                SELECT s.id, s.lock_version, s.lock_version - 1, :kind, :command, :resolver, 'conflict rehearsal',
                       s.form_data, journal.submission_revision_metadata(s)
                FROM data_submission s WHERE s.id = :id""",
            new MapSqlParameterSource("id", head.id()).addValue("kind", revisionKind).addValue("command", kind)
                .addValue("resolver", resolver));
        jdbc.update("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, submission_version)
                SELECT s.id, s.serial_number, s.uid, s.template_version_uid, s.form_data, :eventType, 'pending', 0,
                       s.lock_version
                FROM data_submission s WHERE s.id = :id""",
            Map.of("id", head.id(), "eventType", revisionKind));
        return kind;
    }

    // ------------------------------------------------------------------------------------------ pass 3

    private void statusEvidence() {
        long started = System.nanoTime();
        readOnly.executeWithoutResult(transaction -> {
            transaction.setRollbackOnly();
            Map<String, Object> heads = jdbc.queryForMap("""
                    SELECT count(*) AS heads,
                           count(*) FILTER (WHERE jsonb_typeof(form_data) = 'object'
                                                  AND form_data -> '_status' IS NOT NULL) AS with_status,
                           count(DISTINCT form_data ->> '_status') AS values
                    FROM data_submission WHERE""" + " " + scope(),
                scopeParameters());
            status.counts.put("heads", adder((Number) heads.get("heads")));
            status.counts.put("heads with a top-level _status", adder((Number) heads.get("with_status")));
            status.counts.put("distinct top-level _status values", adder((Number) heads.get("values")));
        });
        readOnly.executeWithoutResult(transaction -> {
            transaction.setRollbackOnly();
            // One sort of every outbox row's keys: allowed longer than a single read.
            jdbc.getJdbcTemplate().queryForObject("SELECT set_config('statement_timeout', '300s', true)",
                String.class);
            history();
        });
        line("pass 3, _status: in " + seconds(started) + " s");
        status.sorted().forEach((key, count) -> line("  " + key + ": " + count));
    }

    /**
     * Consecutive states of each submission in the outbox, whose rows record every saved change since the ETL began:
     * how many changes differ only in {@code _status}, the comparison's other rules applied.
     */
    private void history() {
        List<Map<String, Object>> events = jdbc.queryForList(
            "SELECT o.outbox_id, o.submission_id FROM outbox o WHERE o.payload IS NOT NULL"
                + (settings.scope() == null ? ""
                : " AND o.submission_id IN (SELECT id FROM data_submission WHERE uid LIKE :scope)")
                + " ORDER BY o.submission_id, o.outbox_id",
            scopeParameters());
        String submission = null;
        JsonNode previous = null;
        String lastChange = null;
        List<Long> batch = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        for (int i = 0; i <= events.size(); i++) {
            if (i < events.size()) {
                batch.add(((Number) events.get(i).get("outbox_id")).longValue());
                owners.add((String) events.get(i).get("submission_id"));
                if (batch.size() < PAYLOAD_BATCH) {
                    continue;
                }
            }
            if (batch.isEmpty()) {
                break;
            }
            Map<Long, String> payloads = new LinkedHashMap<>();
            jdbc.query("SELECT outbox_id, payload::text AS payload FROM outbox WHERE outbox_id IN (:ids)",
                Map.of("ids", batch), row -> {
                    payloads.put(row.getLong("outbox_id"), row.getString("payload"));
                });
            for (int j = 0; j < batch.size(); j++) {
                String owner = owners.get(j);
                JsonNode payload = exact(payloads.get(batch.get(j)));
                if (!owner.equals(submission)) {
                    countLastChange(lastChange);
                    submission = owner;
                    previous = payload;
                    lastChange = null;
                    continue;
                }
                String change = change(previous, payload);
                status.add("history: consecutive states, " + change);
                if (!change.equals("identical")) {
                    lastChange = change;
                }
                previous = payload;
            }
            batch.clear();
            owners.clear();
        }
        countLastChange(lastChange);
    }

    private void countLastChange(String lastChange) {
        if ("only _status differs".equals(lastChange)) {
            status.add("history: submissions whose latest change differs only in _status");
        }
    }

    private static String change(JsonNode before, JsonNode after) {
        if (ConflictComparison.sameDocument(before, after)) {
            return "identical";
        }
        if (ConflictComparison.sameDocument(withoutStatus(before), withoutStatus(after))) {
            return "only _status differs";
        }
        JsonNode statusBefore = before == null ? null : before.get("_status");
        JsonNode statusAfter = after == null ? null : after.get("_status");
        return java.util.Objects.equals(statusBefore, statusAfter)
            ? "other values differ, _status the same" : "other values differ, _status too";
    }

    // ------------------------------------------------------------------------------------------ detection

    /**
     * What detection decides for one copy: before the checks, or once they pass, or, given a refusal, when the checks
     * refuse it.
     */
    private String outcome(Head head, Holder holder, SubmissionRevisionReader.State state, JsonNode document,
                           ErrorCode refusal) {
        store.clearLast();
        PhoneCopy copy = copy(head, holder, state, document);
        try {
            ConflictCheck check = detection.check(() -> copy, head.asStored(), template(state.formVersion()),
                holder.userId(), user(holder.login()));
            if (check.answeredBeforeChecks()) {
                return store.last() == null ? "changes nothing, before the checks"
                    : "counts on the open conflict, before the checks";
            }
            if (refusal != null) {
                return check.keepsAsideOnRefusal(refusal) ? kept(check) : "refusal stands";
            }
            return switch (check.afterChecks()) {
                case NOTHING -> "changes nothing, after the checks";
                case APPLY -> head.deleted() ? "applies as today (submission deleted)" : "applies as today";
                case KEEP_ASIDE -> kept(check);
            };
        } catch (RuntimeException failure) {
            return "failed: " + FailureShape.of(failure);
        }
    }

    /** Keeps the copy aside, which the counting store only counts; the Reference checks are not rehearsed. */
    private String kept(ConflictCheck check) {
        check.keepAside();
        CountingStore.Kept kept = store.last();
        if (kept == null) {
            return "kept aside, nothing stored";
        }
        return "kept aside, " + kept.cause() + (kept.rowsLackIds() ? ", rows lack ids" : "");
    }

    private static PhoneCopy copy(Head head, Holder holder, SubmissionRevisionReader.State state, JsonNode document) {
        boolean deleted = false;
        ObjectNode item = REQUEST.createObjectNode();
        item.put("uid", head.uid());
        item.put("form", head.form());
        item.put("formVersion", state.formVersion());
        item.put("assignment", state.assignment());
        item.put("deleted", deleted);
        item.put("status", state.status());
        item.set("formData", document);
        return new PhoneCopy(UploadRoute.LEGACY, item,
            new ComparedState(document, deleted, state.assignment(), state.status()));
    }

    // ------------------------------------------------------------------------------------------ reading

    /** A submission as the passes read it. */
    private record Head(String id, String uid, String form, String createdBy, String lastModifiedBy,
                        boolean deleted, String assignment, String document) {

        DataSubmission asStored() {
            DataSubmission stored = new DataSubmission();
            stored.setId(id);
            stored.setUid(uid);
            stored.setForm(form);
            stored.setCreatedBy(createdBy);
            return stored;
        }
    }

    /**
     * Whose phone holds the copy, and which revision's state it holds.
     *
     * @param how how they were found: {@code upload}, {@code release}, {@code creator} or {@code last modifier}, the
     *            last two for a submission no upload changed since the journal began
     */
    private record Holder(String userId, String login, String how, long version) {

        /** Whether the revision they hold is their base: not for a last modifier, nor a creator's deleted baseline. */
        boolean ownBase(SubmissionRevisionReader.State state) {
            return !how.equals("last modifier") && !(how.equals("creator") && state.deleted());
        }
    }

    private Optional<Head> head(String id, boolean lock) {
        return jdbc.query("""
                SELECT id, uid, template_uid, created_by, last_modified_by, coalesce(deleted, false) AS deleted,
                       assignment_uid, form_data::text AS document
                FROM data_submission WHERE id = :id""" + (lock ? " FOR NO KEY UPDATE" : ""),
            Map.of("id", id),
            (row, n) -> new Head(row.getString("id"), row.getString("uid"), row.getString("template_uid"),
                row.getString("created_by"), row.getString("last_modified_by"), row.getBoolean("deleted"),
                row.getString("assignment_uid"), row.getString("document"))).stream().findFirst();
    }

    private Optional<Holder> holder(Head head, Tally tally) {
        List<Map<String, Object>> uploads = jdbc.queryForList("""
                SELECT version, command, actor_user_id, on_behalf_of_user_id FROM journal.submission_revision
                WHERE submission_id = :id AND command IN ('UPLOAD', 'LEGACY_UPLOAD', 'HELD_RELEASE')
                ORDER BY version DESC LIMIT 1""",
            Map.of("id", head.id()));
        if (!uploads.isEmpty()) {
            Map<String, Object> upload = uploads.get(0);
            boolean release = "HELD_RELEASE".equals(upload.get("command"));
            String userId = (String) (release ? upload.get("on_behalf_of_user_id") : upload.get("actor_user_id"));
            Optional<String> login = loginOf(userId);
            if (login.isEmpty()) {
                tally.add("not found: the uploader is not a user");
                return Optional.empty();
            }
            String how = release ? "release" : "upload";
            tally.add(how);
            return Optional.of(new Holder(userId, login.get(), how, ((Number) upload.get("version")).longValue()));
        }
        Long first = jdbc.queryForObject(
            "SELECT min(version) FROM journal.submission_revision WHERE submission_id = :id",
            Map.of("id", head.id()), Long.class);
        if (first == null) {
            tally.add("not found: no revision");
            return Optional.empty();
        }
        String login = head.lastModifiedBy() != null ? head.lastModifiedBy() : head.createdBy();
        Optional<String> userId = login == null ? Optional.empty() : jdbc.queryForList(
            "SELECT id FROM app_user WHERE lower(login) = lower(:login) ORDER BY id LIMIT 1",
            Map.of("login", login), String.class).stream().findFirst();
        if (userId.isEmpty()) {
            tally.add("not found: the last modifier is not a user");
            return Optional.empty();
        }
        boolean creator = head.createdBy() != null && head.createdBy().equalsIgnoreCase(login);
        String how = creator ? "creator" : "last modifier";
        tally.add(how);
        return Optional.of(new Holder(userId.get(), login, how, first));
    }

    private Optional<SubmissionRevisionReader.State> state(Head head, Holder holder) {
        try {
            return Optional.of(new SubmissionRevisionReader(jdbc).stateAt(head.id(), holder.version()));
        } catch (RuntimeException unreadable) {
            return Optional.empty();
        }
    }

    private Optional<String> loginOf(String userId) {
        return userId == null ? Optional.empty() : jdbc.queryForList(
            "SELECT login FROM app_user WHERE id = :id", Map.of("id", userId), String.class).stream().findFirst();
    }

    /** The form version's repeatable sections, which say which arrays hold rows; none when it cannot be read. */
    private TemplateVersionContext template(String formVersion) {
        return templates.computeIfAbsent(String.valueOf(formVersion), uid -> {
            Map<String, AbstractElement> byPath = new LinkedHashMap<>();
            try {
                String sections = jdbc.queryForList(
                    "SELECT sections::text FROM data_template_version WHERE uid = :uid", Map.of("uid", uid),
                    String.class).stream().findFirst().orElse("[]");
                List<FormSectionConf> parsed = new ObjectMapper().readValue(sections, new TypeReference<>() {
                });
                parsed.stream().filter(section -> section.getPath() != null)
                    .forEach(section -> byPath.put(section.getPath(), section));
            } catch (Exception unreadable) {
                holders.add("form versions whose sections could not be read (copies not tagged)");
            }
            TemplateVersionContext context = mock(TemplateVersionContext.class);
            when(context.getElementsByPath()).thenReturn(Map.copyOf(byPath));
            return context;
        });
    }

    /** Before any connection: a database is named, and neither it nor the host is production's. */
    static void requireAClone(Settings settings) {
        if (settings.database() == null || settings.database().isBlank()) {
            throw new IllegalArgumentException("Name the database to rehearse on: it must be a clone");
        }
        if (PRODUCTION_DATABASES.contains(settings.database().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Database " + settings.database() + " is production's: the rehearsal "
                + "writes and rolls back, so it runs on a clone only");
        }
        String url = settings.url() == null ? "" : settings.url();
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("The rehearsal reads PostgreSQL only");
        }
        String host;
        try {
            host = URI.create(url.substring("jdbc:".length())).getHost();
        } catch (IllegalArgumentException unreadable) {
            host = null;
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("The rehearsal needs one host it can read in the URL");
        }
        if (PRODUCTION_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Host " + host + " is production's database host: the rehearsal runs "
                + "on a clone only");
        }
    }

    /** Connected: the database is the one named, and not production's. */
    private void requireTheDatabaseNamed() {
        String connected = jdbc.getJdbcTemplate().queryForObject("SELECT current_database()", String.class);
        if (!settings.database().equals(connected)) {
            throw new IllegalStateException("Connected to database " + connected + ", not to " + settings.database()
                + ", the database named");
        }
        if (PRODUCTION_DATABASES.contains(String.valueOf(connected).toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Database " + connected + " is production's: the rehearsal runs on a "
                + "clone only");
        }
    }

    private void requireTheSchema() {
        Boolean conflicts = jdbc.getJdbcTemplate().queryForObject(
            "SELECT to_regclass('submission_review.conflict') IS NOT NULL", Boolean.class);
        String commands = jdbc.getJdbcTemplate().queryForList(
            "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_submission_revision_command'",
            String.class).stream().findFirst().orElse("");
        if (!Boolean.TRUE.equals(conflicts) || !commands.contains("MOVE")) {
            throw new IllegalStateException("The database lacks step S2's schema (submission_review.conflict and the "
                + "journal's review commands): run the S2 build's migrations on it first");
        }
        String readOnlyState = readOnly.execute(transaction -> {
            transaction.setRollbackOnly();
            return jdbc.getJdbcTemplate().queryForObject("SHOW transaction_read_only", String.class);
        });
        if (!"on".equals(readOnlyState)) {
            throw new IllegalStateException("The read-only passes would not run read-only");
        }
    }

    // ------------------------------------------------------------------------------------------ helpers

    /** The two shapes a phone re-sends a copy in. */
    private enum Shape {
        ROWS_WITH_IDS("apps 6.0 and later (rows with ids)"),
        ROWS_WITHOUT_IDS("apps 5.2 and 5.3 (rows without ids)");

        private final String label;

        Shape(String label) {
            this.label = label;
        }

        JsonNode of(JsonNode document) {
            if (this == ROWS_WITH_IDS || document == null) {
                return document;
            }
            JsonNode copy = document.deepCopy();
            stripRows(copy, false);
            return copy;
        }

        private static void stripRows(JsonNode node, boolean row) {
            if (node instanceof ObjectNode object) {
                if (row) {
                    object.remove(SubmissionNormalizer.ROW_STRUCTURE_KEYS);
                }
                for (Iterator<JsonNode> values = object.elements(); values.hasNext(); ) {
                    stripRows(values.next(), false);
                }
            } else if (node instanceof ArrayNode array) {
                array.forEach(element -> stripRows(element, true));
            }
        }
    }

    private static boolean onlyStatusDiffers(String stored, String copy) {
        JsonNode a = exact(stored);
        JsonNode b = exact(copy);
        return !ConflictComparison.sameDocument(a, b)
            && ConflictComparison.sameDocument(withoutStatus(a), withoutStatus(b));
    }

    private static JsonNode withoutStatus(JsonNode document) {
        if (!(document instanceof ObjectNode object) || !object.has("_status")) {
            return document;
        }
        ObjectNode copy = object.deepCopy();
        copy.remove("_status");
        return copy;
    }

    /** As a request body is read. */
    private static JsonNode parse(String text) {
        try {
            return text == null ? null : REQUEST.readTree(text);
        } catch (Exception e) {
            throw new IllegalStateException("A stored document is not readable JSON");
        }
    }

    /** As stored text is read, every number exact. */
    private static JsonNode exact(String text) {
        try {
            return text == null ? null : StoredJson.parse(text);
        } catch (Exception e) {
            throw new IllegalStateException("A stored document is not readable JSON");
        }
    }

    private String scope() {
        return settings.scope() == null ? "true" : "uid LIKE :scope";
    }

    private MapSqlParameterSource scopeParameters() {
        return new MapSqlParameterSource("scope", settings.scope());
    }

    private void inParallel(List<String> ids, Consumer<String> work) {
        ExecutorService pool = Executors.newFixedThreadPool(settings.threads());
        try {
            List<Future<?>> parts = new ArrayList<>();
            int threads = settings.threads();
            for (int t = 0; t < threads; t++) {
                int part = t;
                parts.add(pool.submit(() -> {
                    for (int i = part; i < ids.size(); i += threads) {
                        work.accept(ids.get(i));
                    }
                }));
            }
            for (Future<?> part : parts) {
                part.get();
            }
        } catch (Exception e) {
            throw new IllegalStateException("A rehearsal worker failed: " + FailureShape.of(e), e);
        } finally {
            pool.shutdownNow();
        }
    }

    private static ActivationSwitches switchedOn() {
        DefaultListableBeanFactory declared = new DefaultListableBeanFactory();
        declared.registerSingleton("conflictDetection", ConflictDetectionSwitch.CONFLICT_DETECTION);
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("rehearsal",
            Map.of("datarun.activation." + ConflictDetectionSwitch.CONFLICT_DETECTION.name(), "true")));
        ActivationSwitches switches = new ActivationSwitches(declared.getBeanProvider(ActivationSwitch.class),
            environment);
        if (!switches.isOn(ConflictDetectionSwitch.CONFLICT_DETECTION)) {
            throw new IllegalStateException("Conflict detection did not switch on");
        }
        return switches;
    }

    /** The uploader as detection sees them: only their login, by which it knows a creator. */
    private static CurrentUserDetails user(String login) {
        return (CurrentUserDetails) Proxy.newProxyInstance(CurrentUserDetails.class.getClassLoader(),
            new Class<?>[]{CurrentUserDetails.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("getUsername")) {
                    return login;
                }
                throw new UnsupportedOperationException("The rehearsal knows only the uploader's login");
            });
    }

    private static Level quietDetection(Level level) {
        ch.qos.logback.classic.Logger logger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ConflictDetection.class);
        Level before = logger.getLevel();
        logger.setLevel(level);
        return before;
    }

    private static LongAdder adder(Number value) {
        LongAdder adder = new LongAdder();
        adder.add(value == null ? 0 : value.longValue());
        return adder;
    }

    private static String seconds(long started) {
        return String.format("%.1f", (System.nanoTime() - started) / 1e9);
    }

    private void line(String text) {
        synchronized (report) {
            report.add(text);
        }
        System.out.println("[conflict-rehearsal] " + text);
    }

    /** A conflict store that counts what detection would keep aside, and never writes. */
    static final class CountingStore extends ConflictStore {

        record Kept(ConflictCause cause, boolean rowsLackIds) {
        }

        private final ThreadLocal<Kept> last = new ThreadLocal<>();

        CountingStore(NamedParameterJdbcTemplate jdbc) {
            super(jdbc);
        }

        void clearLast() {
            last.remove();
        }

        Kept last() {
            return last.get();
        }

        @Override
        public String open(NewConflict conflict) {
            last.set(new Kept(conflict.cause(), conflict.rowsLackIds()));
            return "rehearsal";
        }

        @Override
        public void sentAgain(String id) {
            last.set(new Kept(null, false));
        }

        @Override
        public void replace(String id, ConflictCause cause, Long baseVersion, boolean rowsLackIds, UploadRoute route,
                            String phoneCopy) {
            last.set(new Kept(cause, rowsLackIds));
        }
    }
}
