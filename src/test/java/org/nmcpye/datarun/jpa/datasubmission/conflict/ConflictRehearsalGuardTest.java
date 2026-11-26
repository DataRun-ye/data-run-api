package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictRehearsal.requireAClone;

/**
 * The rehearsal writes and rolls back, so it runs on a clone only: what it refuses before any connection. It also
 * runs every stored submission through production code in one process, so its own classes refer to no Mockito type.
 */
class ConflictRehearsalGuardTest {

    /**
     * A mock remembers every call it answers. The repeat-id walk asks a template context for its paths once for every
     * key of every document, and a cached mock context ran the rehearsal out of memory at about 30,000 submissions. The
     * rule covers the rehearsal's own classes, nested and anonymous ones included, not code they call.
     */
    @Test
    void theRehearsalUsesNoMock() throws Exception {
        // Only the rehearsal's own class files, nested and anonymous ones included, and what they name is not looked up
        // on the classpath: a dependency's package is known from its name, and the lookup costs seconds.
        Pattern rehearsal = Pattern.compile(".*/ConflictRehearsal(\\$[^/]*)?\\.class$");
        Path directory = Path.of(ConflictRehearsal.class.getResource("").toURI());
        JavaClasses classes = ArchConfiguration.withThreadLocalScope(configuration -> {
            configuration.setResolveMissingDependenciesFromClassPath(false);
            return new ClassFileImporter().withImportOption(location -> location.matches(rehearsal))
                .importPath(directory);
        });
        assertThat(classes.contain(ConflictRehearsal.class)).isTrue();
        noClasses().should().dependOnClassesThat().resideInAPackage("org.mockito..").check(classes);
    }

    @Test
    void productionsHostAndDatabaseAreRefusedWithNoOverride() {
        for (String host : List.of("datarun-prod-db.lab", "DATARUN-PROD-DB.LAB", "datarun-prod-db", "192.168.1.111")) {
            assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql://" + host + ":5432/clone", "clone")))
                .as(host).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("production");
        }
        assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql://localhost:5432/nmcpdb", "nmcpdb")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("production");
    }

    @Test
    void aDatabaseMustBeNamedAndTheHostReadable() {
        assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql://localhost:5432/clone", " ")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Name the database");
        assertThatThrownBy(() -> requireAClone(settings("jdbc:mysql://localhost:3306/clone", "clone")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PostgreSQL");
        assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql:clone", "clone")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("host");
        assertThatCode(() -> requireAClone(settings("jdbc:postgresql://localhost:5432/clone", "clone")))
            .doesNotThrowAnyException();
    }

    private static ConflictRehearsal.Settings settings(String url, String database) {
        return new ConflictRehearsal.Settings(url, "user", "password", database, null, 200, 4, "s2", null);
    }
}
