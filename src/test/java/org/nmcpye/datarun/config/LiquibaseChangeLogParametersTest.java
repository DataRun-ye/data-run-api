package org.nmcpye.datarun.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Liquibase gets the configured changelog parameters, and the feed's reader role from datarun.feed.reader-role: the
 * name when it is a plain role name, otherwise empty, so the grant's changeset waits.
 */
class LiquibaseChangeLogParametersTest {

    @Test
    void theReaderRoleIsAlwaysPassedEmptyWhenUnsetOrUnusable() {
        assertThat(LiquibaseConfiguration.changeLogParameters(null, new MockEnvironment()))
            .containsExactly(Map.entry("feedReaderRole", ""));
        assertThat(LiquibaseConfiguration.changeLogParameters(Map.of("outboxIndexLockTimeout", "10s"),
            new MockEnvironment().withProperty("datarun.feed.reader-role", "airbyte")))
            .containsOnly(Map.entry("outboxIndexLockTimeout", "10s"), Map.entry("feedReaderRole", "airbyte"));
        assertThat(LiquibaseConfiguration.changeLogParameters(Map.of("feedReaderRole", "set\"elsewhere"),
            new MockEnvironment().withProperty("datarun.feed.reader-role", "not plain")))
            .as("only the checked setting names the role").containsExactly(Map.entry("feedReaderRole", ""));
    }
}
