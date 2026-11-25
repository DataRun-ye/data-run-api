package org.nmcpye.datarun;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.outbox.service.impl.OutboxClaimServiceImpl;
import org.nmcpye.datarun.outbox.service.impl.OutboxProcessingServiceImpl;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nothing new may depend on tall, events or pivots (owner, 2026-11-25), so that retiring them removes code and changes
 * nothing else. Their code is the runtime under {@code etl}, which also holds the pivots ({@code etl.pivot}).
 *
 * <ul>
 *   <li>The packages built beside them, {@code submission} (the normalizer, the feed, submission review) and the v2
 *       API, depend on nothing under {@code etl} or any {@code pivot} package.</li>
 *   <li>The outbox, the change log both the runtime and its outputs read, depends on nothing under {@code etl}: the
 *       runtime depends on the outbox and its seam ({@code outbox.processing}), never the other way. Two classes broke
 *       this before the rule existed and are listed here until their own cleanup; the list may only shrink.</li>
 * </ul>
 */
class LegacyDependencyRuleTest {

    private static final JavaClasses MAIN = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("org.nmcpye.datarun");

    static final ArchRule NEW_CODE_DOES_NOT_DEPEND_ON_THE_LEGACY_OUTPUTS = noClasses()
        .that().resideInAnyPackage("org.nmcpye.datarun.submission..", "org.nmcpye.datarun.web.v2..")
        .should().dependOnClassesThat().resideInAnyPackage("org.nmcpye.datarun.etl..", "..pivot..")
        .because("nothing new may depend on tall, events or pivots (owner, 2026-11-25)");

    /** Outbox classes that depended on the runtime before the rule; each goes once its dependency is removed. */
    private static final Class<?>[] OUTBOX_CLASSES_STILL_ON_THE_ETL = {
        OutboxClaimServiceImpl.class,     // reads its lease from EtlProperties
        OutboxProcessingServiceImpl.class // counts outcomes on the ETL's run through EtlRunService
    };

    static final ArchRule THE_OUTBOX_DOES_NOT_DEPEND_ON_THE_RUNTIME = noClasses()
        .that().resideInAPackage("org.nmcpye.datarun.outbox..")
        .and().doNotBelongToAnyOf(OUTBOX_CLASSES_STILL_ON_THE_ETL)
        .should().dependOnClassesThat().resideInAPackage("org.nmcpye.datarun.etl..")
        .because("the runtime depends on the outbox and its seam, never the other way");

    @Test
    void theSubmissionPackagesAndTheV2ApiDependOnNoLegacyOutput() {
        NEW_CODE_DOES_NOT_DEPEND_ON_THE_LEGACY_OUTPUTS.check(MAIN);
    }

    @Test
    void theOutboxDependsOnNothingOfTheRuntimeBeyondItsListedExceptions() {
        THE_OUTBOX_DOES_NOT_DEPEND_ON_THE_RUNTIME.check(MAIN);
    }

    /** The exceptions still need their place on the list: one that no longer depends on the runtime must leave it. */
    @Test
    void eachListedOutboxClassStillDependsOnTheRuntime() {
        for (Class<?> listed : OUTBOX_CLASSES_STILL_ON_THE_ETL) {
            classes().that().belongToAnyOf(listed)
                .should().dependOnClassesThat(resideInAPackage("org.nmcpye.datarun.etl.."))
                .because(listed.getSimpleName() + " is listed as still depending on the runtime")
                .check(MAIN);
        }
    }

    /** The rules can fail: a class in either guarded package that reaches the runtime is caught. */
    @Test
    void aClassReachingTheRuntimeBreaksTheRules() {
        JavaClasses withABackDoor = new ClassFileImporter().importClasses(
            org.nmcpye.datarun.submission.LegacyBackDoor.class, org.nmcpye.datarun.outbox.LegacyBackDoor.class,
            EtlProperties.class);

        assertThat(NEW_CODE_DOES_NOT_DEPEND_ON_THE_LEGACY_OUTPUTS.evaluate(withABackDoor).getFailureReport().getDetails())
            .isNotEmpty()
            .allSatisfy(line -> assertThat(line).contains("submission.LegacyBackDoor"));
        assertThat(THE_OUTBOX_DOES_NOT_DEPEND_ON_THE_RUNTIME.evaluate(withABackDoor).getFailureReport().getDetails())
            .isNotEmpty()
            .allSatisfy(line -> assertThat(line).contains("outbox.LegacyBackDoor"));
    }
}
