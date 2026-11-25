package org.nmcpye.datarun;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldSubmissionDecisions;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionWriter;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.service.DefaultDataSubmissionService;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.outbox.repository.OutboxWritePort;
import org.nmcpye.datarun.outbox.repository.OutboxWriter;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Only the submission command service and its persistence layer write {@code data_submission},
 * {@code held_submission}, the change record ({@code submission_revision}) and the outbox's events (B1, C). Everything
 * else adapts to a command of {@code jpa.datasubmission.command.SubmissionCommandService}; within the persistence
 * layer, only {@code DefaultDataSubmissionService} appends revisions and their outbox rows, after the change they
 * record and in its transaction.
 *
 * <p>The persistence layer is the {@code DataSubmissionService} port and its implementation, the submission
 * repository, {@code SubmissionIntake.hold}, {@code HeldSubmissionRepository} and {@code OutboxWriter}. The rule
 * forbids every other class to call their write methods, and the submission lock every write starts with
 * ({@code DataSubmissionRepository.findByUidForWrite}). What it cannot see:</p>
 * <ul>
 *   <li>writes made through the generic {@code IdentifiableObjectService} or {@code JpaIdentifiableRepository}
 *       types. The submission resource has inherited no generic write since 6.5.5: {@code PUT /dataSubmission/{uid}},
 *       {@code POST /} and {@code POST /return} answer 405 (B2);</li>
 *   <li>SQL text. A second test scans it, as a tripwire, not proof: SQL built at run time is not seen.</li>
 * </ul>
 * <p>No violation is accepted: the one the rule first met, the unreachable outbox backfill behind
 * {@code /admin/etl}, left with that code in 6.5.3.</p>
 */
class SubmissionWriteOwnershipTest {

    private static final JavaClasses MAIN = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("org.nmcpye.datarun");

    /** The command service, and the persistence layer it writes through. */
    private static final DescribedPredicate<JavaClass> OWNERS =
        resideInAnyPackage(
            "org.nmcpye.datarun.jpa.datasubmission.command..",
            "org.nmcpye.datarun.jpa.datasubmission.service..")
            .or(belongToAnyOf(SubmissionIntake.class, HeldSubmissionRepository.class, OutboxWriter.class));

    private static final DescribedPredicate<JavaMethodCall> SUBMISSION_WRITE =
        writeOn(DataSubmissionService.class, "upsert", "softDelete", "save", "update", "delete")
            .or(writeOn(DataSubmissionRepository.class,
                "save", "persist", "update", "delete", "merge", "insert", "remove", "findByUidForWrite"))
            .or(writeOn(HeldSubmissionRepository.class, "hold", "markDecided", "recordReleaseError"))
            .or(writeOn(SubmissionIntake.class, "hold"))
            .or(writeOn(OutboxWritePort.class, "insert"))
            .or(writeOn(SubmissionRevisionWriter.class, "append"))
            .or(modifyingQueryOn(DataSubmissionRepository.class));

    static final ArchRule ONLY_THE_COMMAND_SERVICE_WRITES_SUBMISSIONS = noClasses()
        .that(not(OWNERS))
        .should().callMethodWhere(SUBMISSION_WRITE)
        .because("submissions are created, changed, deleted, held and released only through "
            + "SubmissionCommandService, which runs one set of checks and writes the outbox in the same transaction");

    /**
     * A revision, and the outbox row that names it, are written by the persistence layer after the change they record,
     * in its transaction: never by a command, which names the revision's context instead.
     */
    static final ArchRule ONLY_THE_PERSISTENCE_LAYER_RECORDS_CHANGES = noClasses()
        .that().doNotBelongToAnyOf(DefaultDataSubmissionService.class)
        .should().callMethodWhere(writeOn(SubmissionRevisionWriter.class, "append")
            .or(writeOn(OutboxWritePort.class, "insert")))
        .because("each change gets one revision and one outbox row, written by DefaultDataSubmissionService after the "
            + "flush that gave the change its version, in the same transaction");

    /** The upload pipeline is reached through the command service; a held release replays through it too. */
    static final ArchRule ONLY_THE_COMMANDS_USE_THE_UPLOAD_PIPELINE = noClasses()
        .that().doNotBelongToAnyOf(SubmissionCommandService.class, HeldSubmissionDecisions.class, SubmissionUploads.class)
        .should().dependOnClassesThat().belongToAnyOf(SubmissionUploads.class)
        .because("the upload pipeline is a component of SubmissionCommandService, not an entry point");

    static final ArchRule ONLY_THE_COMMANDS_USE_THE_HELD_DECISIONS = noClasses()
        .that().doNotBelongToAnyOf(SubmissionCommandService.class, HeldSubmissionDecisions.class)
        .should().dependOnClassesThat().belongToAnyOf(HeldSubmissionDecisions.class)
        .because("held releases and discards are commands of SubmissionCommandService, not an entry point");

    // SQL text, which ArchUnit cannot see. Scanned as a tripwire, not proof: SQL built at run time is not seen.

    /**
     * Where text may name each table at all, by source path: data_submission only in its entity's mapping (JPA
     * writes it through the persistence port), held_submission in its repository, and the outbox in its writer and
     * in the ETL consumer's claim, due check and backlog reads.
     */
    private static final Map<String, List<String>> NAMED_IN = Map.of(
        "data_submission", List.of("org/nmcpye/datarun/jpa/datasubmission/"),
        "held_submission", List.of("org/nmcpye/datarun/jpa/intake/"),
        "submission_revision", List.of("org/nmcpye/datarun/jpa/datasubmission/revision/"),
        "outbox", List.of("org/nmcpye/datarun/outbox/repository/",
            "org/nmcpye/datarun/etl/admin/ScheduledOrchestratorRunner.java",
            "org/nmcpye/datarun/etl/metrics/EtlMetrics.java"));
    /** Where SQL may write each table: data_submission nowhere, the journal only in its writer. */
    private static final Map<String, List<String>> WRITTEN_IN = Map.of(
        "data_submission", List.of(),
        "held_submission", List.of("org/nmcpye/datarun/jpa/intake/"),
        "submission_revision", List.of("org/nmcpye/datarun/jpa/datasubmission/revision/SubmissionRevisionWriter.java"),
        "outbox", List.of("org/nmcpye/datarun/outbox/repository/"));
    /** Resources may name the tables only in the schema's own changelogs. */
    private static final String RESOURCES_NAMED_IN = "config/liquibase/";
    /** Where JPQL may name the DataSubmission entity. */
    private static final String ENTITY_NAMED_IN = "org/nmcpye/datarun/jpa/datasubmission/";

    /**
     * A table, bare, schema-qualified (the journal lives in the schema journal) or quoted: the word boundaries hold
     * at the dot and the quote.
     */
    private static final Pattern TABLE =
        Pattern.compile("(?i)\\b(data_submission|held_submission|submission_revision|outbox)\\b");
    /** A literal that is just a table name, for SQL assembled from pieces. */
    private static final Pattern WHOLE_TABLE = Pattern.compile(
        "(?i)^\\s*(?:\\w+\\s*\\.\\s*)?(?:\\\\?\")?(data_submission|held_submission|submission_revision|outbox)"
            + "(?:\\\\?\")?\\s*$");
    private static final Pattern WRITE = Pattern.compile("(?i)\\b(insert\\s+into|update|delete\\s+from|merge\\s+into"
        + "|truncate(?:\\s+table)?|copy)\\s+(?:only\\s+)?(?:\\w+\\s*\\.\\s*)?(?:\\\\?\")?"
        + "(data_submission|held_submission|submission_revision|outbox)\\b");
    /**
     * "outbox" is an ordinary word in log and metric texts, so it counts as a table only beside an SQL word or as a
     * whole literal; the two other names count wherever they appear.
     */
    private static final Pattern SQL_WORD = Pattern.compile(
        "(?i)\\b(select|insert|update|delete|merge|truncate|copy|from|into|join|where|set|values|returning)\\b");
    private static final Pattern ENTITY = Pattern.compile("\\bDataSubmission\\b");

    @Test
    void onlyTheCommandServiceAndItsPersistenceWriteSubmissions() {
        ONLY_THE_COMMAND_SERVICE_WRITES_SUBMISSIONS.check(MAIN);
    }

    @Test
    void onlyThePersistenceLayerRecordsChanges() {
        ONLY_THE_PERSISTENCE_LAYER_RECORDS_CHANGES.check(MAIN);
    }

    @Test
    void onlyTheCommandServiceUsesItsComponents() {
        ONLY_THE_COMMANDS_USE_THE_UPLOAD_PIPELINE.check(MAIN);
        ONLY_THE_COMMANDS_USE_THE_HELD_DECISIONS.check(MAIN);
    }

    /**
     * A tripwire, not proof, for what ArchUnit cannot see: SQL text. Every string literal and text block in the main
     * sources, and every line of the main resources, is read for the three tables. A table may be named only where
     * {@link #NAMED_IN} allows it, whatever the verb (split literals, MERGE, TRUNCATE and COPY included), and written
     * only where {@link #WRITTEN_IN} does; a JPQL {@code @Query} may name the DataSubmission entity only in its own
     * package; resources may name the tables only in the Liquibase changelogs. SQL assembled at run time from names
     * held elsewhere is not seen.
     */
    @Test
    void noSqlTextOutsideThePersistenceLayerNamesTheTables() throws IOException {
        List<String> outside = new ArrayList<>();
        int named = 0;
        Path sources = Path.of("src", "main", "java");
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                List<String> found = sourceFindings(relative(sources, file), Files.readString(file));
                named += found.size();
                found.stream().filter(finding -> finding.startsWith("outside ")).forEach(outside::add);
            }
        }
        Path resources = Path.of("src", "main", "resources");
        try (Stream<Path> files = Files.walk(resources)) {
            for (Path file : files.filter(path -> path.toString().matches(".*\\.(xml|ya?ml|sql|properties|json)$"))
                .toList()) {
                outside.addAll(resourceFindings(relative(resources, file), Files.readString(file)));
            }
        }
        assertThat(named).as("the scan finds the SQL that does exist").isPositive();
        assertThat(outside)
            .as("SQL text naming data_submission, held_submission or the outbox outside its persistence classes "
                + "(a tripwire, not proof: SQL built at run time is not seen)")
            .isEmpty();
    }

    /**
     * Two things switch off the journal's guards: {@code session_replication_role = replica}, which a superuser may set
     * and which stops every trigger and foreign key check of a session, and {@code DISABLE TRIGGER}, which the table's
     * owner (the application's own database role) may run. Only the tests' purge sets the first
     * ({@code SubmissionTestPurge}); main code and resources, the changelogs included, never name either.
     */
    @Test
    void noMainCodeSwitchesOffTheDatabaseGuards() throws IOException {
        List<String> naming = new ArrayList<>();
        for (Path root : List.of(Path.of("src", "main", "java"), Path.of("src", "main", "resources"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String text = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.ISO_8859_1)
                        .toLowerCase(Locale.ROOT);
                    if (text.contains("session_replication_role") || DISABLE_TRIGGER.matcher(text).find()) {
                        naming.add(relative(root, file));
                    }
                }
            }
        }
        assertThat(naming).as("main files that switch the database guards off").isEmpty();
        assertThat(DISABLE_TRIGGER.matcher("alter table journal.x disable  trigger all").find()).isTrue();
    }

    private static final Pattern DISABLE_TRIGGER = Pattern.compile("disable\\s+trigger");

    /** The scan's rules on small sources: what it reads, skips and flags. */
    @Test
    void theSqlScanFlagsWhatItShould() {
        String source = String.join("\n",
            "// update outbox status in a comment",
            "/* insert into data_submission in a block comment */",
            "String a = \"UPDATE held_submission SET x = 1\";",
            "String b = \"\"\"",
            "    DELETE FROM outbox",
            "    WHERE id = ?\"\"\";",
            "char c = '\"';",
            "String d = \"a \\\" quote and update data_submission\";");
        assertThat(stringLiterals(source))
            .containsExactly("UPDATE held_submission SET x = 1", "\n    DELETE FROM outbox\n    WHERE id = ?",
                "a \\\" quote and update data_submission");

        assertThat(sourceFindings("org/nmcpye/datarun/web/X.java", String.join("\n",
            "String split = \"TRUNCATE \" + \"public.held_submission\";",
            "String quoted = \"COPY \\\"data_submission\\\" FROM STDIN\";",
            "String piece = \"outbox\";",
            "String log = \"Could not read the outbox backlog: {}\";",
            "@Query(\"update DataSubmission s set s.status = null\")")))
            .containsExactly(
                "outside org/nmcpye/datarun/web/X.java: names held_submission in \"public.held_submission\"",
                "outside org/nmcpye/datarun/web/X.java: names data_submission in \"COPY \\\"data_submission\\\" FROM STDIN\"",
                "outside org/nmcpye/datarun/web/X.java: writes data_submission in \"COPY \\\"data_submission\\\" FROM STDIN\"",
                "outside org/nmcpye/datarun/web/X.java: names outbox in \"outbox\"",
                "outside org/nmcpye/datarun/web/X.java: names the DataSubmission entity in \"update DataSubmission s set s.status = null\"");
        assertThat(sourceFindings("org/nmcpye/datarun/etl/metrics/EtlMetrics.java", "String read = \"SELECT count(*) FROM outbox\";"))
            .containsExactly("allowed org/nmcpye/datarun/etl/metrics/EtlMetrics.java: names outbox in \"SELECT count(*) FROM outbox\"");
        assertThat(sourceFindings("org/nmcpye/datarun/etl/metrics/X.java", "String read = \"SELECT count(*) FROM outbox\";"))
            .containsExactly("outside org/nmcpye/datarun/etl/metrics/X.java: names outbox in \"SELECT count(*) FROM outbox\"");
        assertThat(sourceFindings("org/nmcpye/datarun/etl/metrics/X.java", "String sneak = \"UPDATE outbox SET x = 1\";"))
            .contains("outside org/nmcpye/datarun/etl/metrics/X.java: writes outbox in \"UPDATE outbox SET x = 1\"");
        assertThat(sourceFindings("org/nmcpye/datarun/jpa/datasubmission/revision/Reader.java",
            "String read = \"SELECT * FROM submission_revision\";"))
            .containsExactly("allowed org/nmcpye/datarun/jpa/datasubmission/revision/Reader.java: names submission_revision "
                + "in \"SELECT * FROM submission_revision\"");
        assertThat(sourceFindings("org/nmcpye/datarun/jpa/datasubmission/revision/Reader.java",
            "String write = \"INSERT INTO submission_revision VALUES (1)\";"))
            .contains("outside org/nmcpye/datarun/jpa/datasubmission/revision/Reader.java: writes submission_revision "
                + "in \"INSERT INTO submission_revision VALUES (1)\"");
        assertThat(sourceFindings("org/nmcpye/datarun/web/X.java",
            "String write = \"DELETE FROM journal.submission_revision WHERE true\";"))
            .as("a schema-qualified write")
            .contains("outside org/nmcpye/datarun/web/X.java: writes submission_revision "
                + "in \"DELETE FROM journal.submission_revision WHERE true\"");
        assertThat(sourceFindings("org/nmcpye/datarun/web/X.java", "String piece = \"journal.submission_revision\";"))
            .as("a schema-qualified name on its own")
            .contains("outside org/nmcpye/datarun/web/X.java: names submission_revision in \"journal.submission_revision\"");
        assertThat(sourceFindings("org/nmcpye/datarun/etl/X.java", "String read = \"SELECT submission_revision_metadata(s)\";"))
            .as("the metadata function is not the table").isEmpty();
        assertThat(resourceFindings("config/application.yml", String.join("\n",
            "# the outbox consumer", "query: DELETE FROM outbox")))
            .containsExactly("outside config/application.yml:2: names outbox in \"query: DELETE FROM outbox\"");
    }

    /**
     * What a Java source says about the tables: one entry per table named, written, or entity named in a JPQL query,
     * each starting with "allowed " or "outside ".
     */
    static List<String> sourceFindings(String path, String source) {
        List<String> findings = new ArrayList<>();
        for (Literal literal : literals(source)) {
            Matcher table = TABLE.matcher(literal.text());
            boolean sqlLike = SQL_WORD.matcher(literal.text()).find() || WHOLE_TABLE.matcher(literal.text()).matches();
            List<String> seen = new ArrayList<>();
            while (table.find()) {
                String name = table.group(1).toLowerCase(Locale.ROOT);
                if (!seen.contains(name) && (sqlLike || !"outbox".equals(name))) {
                    seen.add(name);
                    findings.add(where(path, NAMED_IN.get(name)) + path + ": names " + name + " in \"" + literal.text()
                        + "\"");
                }
            }
            Matcher write = WRITE.matcher(literal.text());
            while (write.find()) {
                String name = write.group(2).toLowerCase(Locale.ROOT);
                findings.add(where(path, WRITTEN_IN.get(name)) + path + ": writes " + name + " in \"" + literal.text()
                    + "\"");
            }
            if (literal.inQuery() && ENTITY.matcher(literal.text()).find()) {
                findings.add(where(path, List.of(ENTITY_NAMED_IN)) + path + ": names the DataSubmission entity in \""
                    + literal.text() + "\"");
            }
        }
        return findings;
    }

    /** Lines of a resource that name a table outside the changelogs. */
    static List<String> resourceFindings(String path, String text) {
        if (path.startsWith(RESOURCES_NAMED_IN)) {
            return List.of();
        }
        List<String> findings = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher table = TABLE.matcher(lines[i]);
            boolean sqlLike = SQL_WORD.matcher(lines[i]).find();
            while (table.find()) {
                String name = table.group(1).toLowerCase(Locale.ROOT);
                if (sqlLike || !"outbox".equals(name)) {
                    findings.add("outside " + path + ":" + (i + 1) + ": names " + name + " in \"" + lines[i].strip()
                        + "\"");
                }
            }
        }
        return findings;
    }

    private static String where(String path, List<String> homes) {
        return homes.stream().anyMatch(path::startsWith) ? "allowed " : "outside ";
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    /** A string literal or text block, and whether it sits in a {@code @Query} or {@code @NativeQuery}. */
    record Literal(String text, boolean inQuery) {
    }

    /** The contents of the string literals and text blocks in Java source, comments and char literals skipped. */
    static List<String> stringLiterals(String source) {
        return literals(source).stream().map(Literal::text).toList();
    }

    static List<Literal> literals(String source) {
        List<Literal> literals = new ArrayList<>();
        int queryDepth = 0;
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (source.startsWith("//", i)) {
                int end = source.indexOf('\n', i);
                i = end < 0 ? source.length() : end + 1;
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
            } else if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                end = end < 0 ? source.length() : end;
                literals.add(new Literal(source.substring(i + 3, end), queryDepth > 0));
                i = end + 3;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < source.length() && source.charAt(j) != c) {
                    j += source.charAt(j) == '\\' ? 2 : 1;
                }
                if (c == '"') {
                    literals.add(new Literal(source.substring(i + 1, Math.min(j, source.length())), queryDepth > 0));
                }
                i = j + 1;
            } else if (queryDepth == 0 && (source.startsWith("@Query", i) || source.startsWith("@NativeQuery", i))) {
                int open = source.indexOf('(', i);
                boolean annotation = open > 0 && source.substring(i + 1, open).strip().matches("Query|NativeQuery");
                i = annotation ? open + 1 : i + 1;
                queryDepth = annotation ? 1 : 0;
            } else {
                if (queryDepth > 0 && c == '(') {
                    queryDepth++;
                } else if (queryDepth > 0 && c == ')') {
                    queryDepth--;
                }
                i++;
            }
        }
        return literals;
    }

    /** The rule is not vacuous: a class outside the command service that saves a submission breaks it. */
    @Test
    void aWriterOutsideTheCommandServiceBreaksTheRule() {
        JavaClasses withABackDoor = new ClassFileImporter().importClasses(BackDoor.class, ModifyingSubmissions.class);

        assertThat(ONLY_THE_COMMAND_SERVICE_WRITES_SUBMISSIONS.evaluate(withABackDoor).getFailureReport().getDetails())
            .hasSize(7)
            .allSatisfy(line -> assertThat(line).contains(BackDoor.class.getName()))
            .anySatisfy(line -> assertThat(line).contains("DataSubmissionRepository.findByUidForWrite"))
            .anySatisfy(line -> assertThat(line).contains("ModifyingSubmissions.tidy"));
    }

    /**
     * Seven writes the rule must catch, for the test above only: the submission lock among them, and the last one by
     * its @Modifying, not its name.
     */
    @SuppressWarnings("unused")
    static class BackDoor {
        void writes(DataSubmissionService submissions, DataSubmissionRepository repository,
                    HeldSubmissionRepository held, OutboxWritePort outbox, SubmissionRevisionWriter revisions,
                    ModifyingSubmissions modifying) {
            submissions.upsert(null, null, new EntitySaveSummaryVM(), null);
            repository.deleteAllByUidIn(List.of("a"));
            repository.findByUidForWrite("a");
            held.recordReleaseError("a", "b");
            outbox.insert(List.of());
            revisions.append("a", 0, null, null, null);
            modifying.tidy("a");
        }
    }

    /** The persistence layer's rule is not vacuous either: the back door's revision and outbox writes break it. */
    @Test
    void aRevisionWriterOutsideThePersistenceLayerBreaksTheRule() {
        JavaClasses withABackDoor = new ClassFileImporter().importClasses(BackDoor.class);

        assertThat(ONLY_THE_PERSISTENCE_LAYER_RECORDS_CHANGES.evaluate(withABackDoor).getFailureReport().getDetails())
            .hasSize(2)
            .anySatisfy(line -> assertThat(line).contains("SubmissionRevisionWriter.append"))
            .anySatisfy(line -> assertThat(line).contains("OutboxWritePort.insert"));
    }

    /** A JPQL write whose name says nothing. Nested, so Spring Data's repository scan leaves it alone. */
    interface ModifyingSubmissions extends DataSubmissionRepository {
        @Modifying
        @Query("update DataSubmission s set s.status = null where s.uid = ?1")
        int tidy(String uid);
    }

    /** A call to a {@code @Modifying} query method of {@code owner}: a JPQL write, whatever its name. */
    private static DescribedPredicate<JavaMethodCall> modifyingQueryOn(Class<?> owner) {
        return new DescribedPredicate<>("a @Modifying query on " + owner.getSimpleName()) {
            @Override
            public boolean test(JavaMethodCall call) {
                return call.getTargetOwner().isAssignableTo(owner)
                    && call.getTarget().resolveMember().map(method -> method.isAnnotatedWith(Modifying.class))
                    .orElse(false);
            }
        };
    }

    private static DescribedPredicate<JavaMethodCall> writeOn(Class<?> owner, String... namePrefixes) {
        return new DescribedPredicate<>("a write on " + owner.getSimpleName()) {
            @Override
            public boolean test(JavaMethodCall call) {
                return call.getTargetOwner().isAssignableTo(owner)
                    && Arrays.stream(namePrefixes).anyMatch(prefix -> call.getName().startsWith(prefix));
            }
        };
    }
}
