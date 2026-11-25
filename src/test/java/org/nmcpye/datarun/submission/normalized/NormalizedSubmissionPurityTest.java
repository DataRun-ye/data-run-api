package org.nmcpye.datarun.submission.normalized;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

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
                "com.fasterxml.jackson.databind..",
                "org.nmcpye.datarun.datatemplateelement.enumeration")
            .because("a pure function reads its arguments and nothing else")
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
