package org.nmcpye.datarun.config;

import io.sentry.IScopes;
import io.sentry.Sentry;
import io.sentry.spring.boot.jakarta.SentryProperties;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Without a DSN, as in tests, development and staging, the application starts with error reporting off. */
@IntegrationTest
class ErrorReportingOffIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void theApplicationStartsWithErrorReportingOff() {
        String dsn = System.getenv("SENTRY_DSN");
        assumeTrue(dsn == null || dsn.isBlank(), "SENTRY_DSN is set in this environment");

        assertThat(context.getBeanNamesForType(SentryProperties.class)).isEmpty();
        assertThat(context.getBeanNamesForType(IScopes.class)).isEmpty();
        assertThat(Sentry.isEnabled()).isFalse();
    }
}
