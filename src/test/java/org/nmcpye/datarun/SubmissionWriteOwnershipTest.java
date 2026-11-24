package org.nmcpye.datarun;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldSubmissionDecisions;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.outbox.repository.OutboxWritePort;
import org.nmcpye.datarun.outbox.repository.OutboxWriter;

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
 * {@code held_submission} and the outbox's events (B1). Everything else adapts to a command of
 * {@code jpa.datasubmission.command.SubmissionCommandService}.
 *
 * <p>The persistence layer is the {@code DataSubmissionService} port and its implementation, the submission
 * repository, {@code SubmissionIntake.hold}, {@code HeldSubmissionRepository} and {@code OutboxWriter}. The rule
 * forbids every other class to call their write methods. What it cannot see:</p>
 * <ul>
 *   <li>writes made through the generic {@code IdentifiableObjectService} or {@code JpaIdentifiableRepository}
 *       types: the inherited {@code PUT /dataSubmission/{uid}} body is one; it answers 400 before its body runs,
 *       and B2 turns the route into a 405;</li>
 *   <li>SQL that names the tables directly: none exists outside these classes today, apart from the ETL consumer's
 *       claim and status updates on the outbox, which are its own progress, not submission events.</li>
 * </ul>
 * <p>The frozen store ({@code src/test/resources/archunit_store}) keeps the one violation that stays: the
 * outbox backfill ({@code etl.admin.BackfillServiceImpl}), reachable only from {@code /admin/etl}, which the security
 * chain refuses to everyone; D2 replaces it. A new violation fails the build; a fixed one leaves the store.</p>
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
                "save", "persist", "update", "delete", "merge", "insert", "remove"))
            .or(writeOn(HeldSubmissionRepository.class, "hold", "markDecided", "recordReleaseError"))
            .or(writeOn(SubmissionIntake.class, "hold"))
            .or(writeOn(OutboxWritePort.class, "insert"));

    static final ArchRule ONLY_THE_COMMAND_SERVICE_WRITES_SUBMISSIONS = noClasses()
        .that(not(OWNERS))
        .should().callMethodWhere(SUBMISSION_WRITE)
        .because("submissions are created, changed, deleted, held and released only through "
            + "SubmissionCommandService, which runs one set of checks and writes the outbox in the same transaction");

    /** The upload pipeline is reached through the command service; a held release replays through it too. */
    static final ArchRule ONLY_THE_COMMANDS_USE_THE_UPLOAD_PIPELINE = noClasses()
        .that().doNotBelongToAnyOf(SubmissionCommandService.class, HeldSubmissionDecisions.class, SubmissionUploads.class)
        .should().dependOnClassesThat().belongToAnyOf(SubmissionUploads.class)
        .because("the upload pipeline is a component of SubmissionCommandService, not an entry point");

    static final ArchRule ONLY_THE_COMMANDS_USE_THE_HELD_DECISIONS = noClasses()
        .that().doNotBelongToAnyOf(SubmissionCommandService.class, HeldSubmissionDecisions.class)
        .should().dependOnClassesThat().belongToAnyOf(HeldSubmissionDecisions.class)
        .because("held releases and discards are commands of SubmissionCommandService, not an entry point");

    /** Where SQL may write each table: nowhere for data_submission, which JPA writes through the persistence port. */
    private static final Map<String, String> SQL_HOMES = Map.of(
        "held_submission", "org/nmcpye/datarun/jpa/intake/",
        "outbox", "org/nmcpye/datarun/outbox/repository/");
    private static final Pattern SQL_WRITE = Pattern.compile(
        "(?i)\\b(insert\\s+into|update|delete\\s+from)\\s+(data_submission|held_submission|outbox)\\b");

    @Test
    void onlyTheCommandServiceAndItsPersistenceWriteSubmissions() {
        FreezingArchRule.freeze(ONLY_THE_COMMAND_SERVICE_WRITES_SUBMISSIONS).check(MAIN);
    }

    @Test
    void onlyTheCommandServiceUsesItsComponents() {
        ONLY_THE_COMMANDS_USE_THE_UPLOAD_PIPELINE.check(MAIN);
        ONLY_THE_COMMANDS_USE_THE_HELD_DECISIONS.check(MAIN);
    }

    /**
     * What ArchUnit cannot see: SQL text. Every string literal and text block in the main sources is scanned for an
     * INSERT, UPDATE or DELETE on the three tables. held_submission may be written only in jpa.intake (its
     * repository), the outbox only in outbox.repository (the writer's inserts, and the ETL consumer's claim and status
     * updates), and data_submission nowhere. Comments are skipped; SQL assembled from a table name held in a variable
     * would not be seen.
     */
    @Test
    void noSqlOutsideThePersistenceLayerWritesTheTables() throws IOException {
        Path root = Path.of("src", "main", "java");
        List<String> outside = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                for (String literal : stringLiterals(Files.readString(file))) {
                    Matcher write = SQL_WRITE.matcher(literal);
                    while (write.find()) {
                        String table = write.group(2).toLowerCase(Locale.ROOT);
                        String found = relative + ": " + write.group().replaceAll("\\s+", " ");
                        seen.add(found);
                        if (!SQL_HOMES.containsKey(table) || !relative.startsWith(SQL_HOMES.get(table))) {
                            outside.add(found);
                        }
                    }
                }
            }
        }
        assertThat(seen).as("the scan finds the writes that do exist").isNotEmpty();
        assertThat(outside).isEmpty();
    }

    /** The scan reads string literals and text blocks, not comments. */
    @Test
    void theSqlScanReadsLiteralsAndSkipsComments() {
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
    }

    /** The contents of the string literals and text blocks in Java source, comments and char literals skipped. */
    static List<String> stringLiterals(String source) {
        List<String> literals = new ArrayList<>();
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
                literals.add(source.substring(i + 3, end));
                i = end + 3;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < source.length() && source.charAt(j) != c) {
                    j += source.charAt(j) == '\\' ? 2 : 1;
                }
                if (c == '"') {
                    literals.add(source.substring(i + 1, Math.min(j, source.length())));
                }
                i = j + 1;
            } else {
                i++;
            }
        }
        return literals;
    }

    /** The rule is not vacuous: a class outside the command service that saves a submission breaks it. */
    @Test
    void aWriterOutsideTheCommandServiceBreaksTheRule() {
        JavaClasses withABackDoor = new ClassFileImporter().importClasses(BackDoor.class);

        assertThat(ONLY_THE_COMMAND_SERVICE_WRITES_SUBMISSIONS.evaluate(withABackDoor).getFailureReport().getDetails())
            .hasSize(4)
            .allSatisfy(line -> assertThat(line).contains(BackDoor.class.getName()));
    }

    /** Four writes the rule must catch, for the test above only. */
    @SuppressWarnings("unused")
    static class BackDoor {
        void writes(DataSubmissionService submissions, DataSubmissionRepository repository,
                    HeldSubmissionRepository held, OutboxWritePort outbox) {
            submissions.upsertAll(List.of(), new EntitySaveSummaryVM());
            repository.deleteAllByUidIn(List.of("a"));
            held.recordReleaseError("a", "b");
            outbox.insertByEventType(List.of(), "SAVE");
        }
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
