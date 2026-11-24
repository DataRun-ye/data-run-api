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
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.outbox.repository.OutboxWritePort;
import org.nmcpye.datarun.outbox.repository.OutboxWriter;

import java.util.Arrays;
import java.util.List;

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

    @Test
    void onlyTheCommandServiceAndItsPersistenceWriteSubmissions() {
        FreezingArchRule.freeze(ONLY_THE_COMMAND_SERVICE_WRITES_SUBMISSIONS).check(MAIN);
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
