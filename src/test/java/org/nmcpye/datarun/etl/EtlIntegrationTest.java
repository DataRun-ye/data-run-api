package org.nmcpye.datarun.etl;

import org.nmcpye.datarun.IntegrationTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The application context the ETL integration tests share. The pool has four connections, as production's pool
 * has more than one: with a single connection a second transaction would wait for the pool instead of for
 * PostgreSQL. The backlog gauges read the outbox on every read, and a row may take 3 s. The Prometheus endpoint
 * is exposed where the main configuration exposes it.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@TestPropertySource(properties = {
    "spring.datasource.hikari.maximum-pool-size=4",
    "datarun.etl.backlog-refresh=0s",
    "datarun.etl.row-timeout=3s",
    "management.endpoints.web.base-path=/management",
    "management.endpoints.web.exposure.include=health,prometheus",
    "management.prometheus.metrics.export.enabled=true"
})
@interface EtlIntegrationTest {
}
