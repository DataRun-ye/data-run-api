package org.nmcpye.datarun.submission.normalized;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The normalizer stays a pure function both callers can share: the feed publisher and submission review's read
 * model, inside a transaction or outside one. Its package reaches no database, no Spring, no entity and no clock, and
 * holds no state that changes.
 */
class NormalizedSubmissionPurityTest {

    private static final String PACKAGE = "org.nmcpye.datarun.submission.normalized";

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(PACKAGE);

    @Test
    void itDependsOnlyOnTheJdkJacksonAndTheValueTypes() {
        classes().that().resideInAPackage(PACKAGE)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                PACKAGE,
                "java.lang..",
                "java.util..",
                "java.math",
                "java.time",
                "java.nio.charset..",
                "java.nio",
                "java.security",
                "com.fasterxml.jackson.core..",
                "com.fasterxml.jackson.databind..",
                "org.nmcpye.datarun.datatemplateelement.enumeration")
            .because("a pure function reads its arguments and nothing else")
            .check(CLASSES);
    }

    @Test
    void itReadsNoClockAndDrawsNoRandomNumber() {
        // the same arguments must always give an equal result
        noClasses().that().resideInAPackage(PACKAGE)
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .orShould().callMethod(System.class, "nanoTime")
            .orShould().callMethod(Math.class, "random")
            .orShould().callMethod(UUID.class, "randomUUID")
            .orShould().dependOnClassesThat().areAssignableTo(Random.class)
            .orShould().dependOnClassesThat().areAssignableTo(Clock.class)
            .check(CLASSES);
    }

    @Test
    void itsStaticFieldsAreConstants() {
        fields().that().areDeclaredInClassesThat().resideInAPackage(PACKAGE)
            .and().areStatic()
            .should().beFinal()
            .check(CLASSES);
    }
}
