package org.nmcpye.datarun.jpa.datasubmission.conflict;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictRehearsal.requireAClone;

/** The rehearsal writes and rolls back, so it runs on a clone only: what it refuses before any connection. */
class ConflictRehearsalGuardTest {

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
