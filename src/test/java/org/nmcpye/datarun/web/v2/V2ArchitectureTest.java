package org.nmcpye.datarun.web.v2;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * v2's web layer is an adapter for a designed contract: it never touches the storage model, so no entity can become
 * a payload, and it shares nothing with the v1 resources, so v1's shapes and error handling cannot leak in. Domain
 * work for v2 lives outside {@code web.v2} and hands it its own types.
 */
class V2ArchitectureTest {

    private static final JavaClasses MAIN = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("org.nmcpye.datarun");

    static final ArchRule NO_ENTITIES = noClasses().that().resideInAPackage("org.nmcpye.datarun.web.v2..")
        .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
        .orShould().dependOnClassesThat().areAnnotatedWith(Embeddable.class)
        .orShould().dependOnClassesThat().areAnnotatedWith(MappedSuperclass.class)
        .because("the v2 wire is a designed contract, never the storage model (the V2 condition)");

    static final ArchRule NOTHING_FROM_V1 = noClasses().that().resideInAPackage("org.nmcpye.datarun.web.v2..")
        .should().dependOnClassesThat().resideInAPackage("org.nmcpye.datarun.web.rest..")
        .because("v2 does not inherit v1's resources, payloads or error shapes");

    @Test
    void v2NeverTouchesTheStorageModel() {
        NO_ENTITIES.check(MAIN);
    }

    @Test
    void v2SharesNothingWithTheV1Resources() {
        NOTHING_FROM_V1.check(MAIN);
    }

    /** The rules can fail: a v2 class handing out an entity, or reusing a v1 error type, is caught. */
    @Test
    void theRulesCatchALeak() {
        JavaClasses leaks = new ClassFileImporter().importClasses(EntityLeak.class, V1Leak.class);
        assertThatThrownBy(() -> NO_ENTITIES.check(leaks)).isInstanceOf(AssertionError.class)
            .hasMessageContaining("EntityLeak");
        assertThatThrownBy(() -> NOTHING_FROM_V1.check(leaks)).isInstanceOf(AssertionError.class)
            .hasMessageContaining("V1Leak");
    }

    static final class EntityLeak {
        org.nmcpye.datarun.jpa.datasubmission.DataSubmission payload() {
            return null;
        }
    }

    static final class V1Leak {
        Class<?> errors() {
            return org.nmcpye.datarun.web.rest.errors.ExceptionTranslator.class;
        }
    }
}
