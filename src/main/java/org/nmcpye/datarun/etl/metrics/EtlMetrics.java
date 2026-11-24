package org.nmcpye.datarun.etl.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * What the ETL exposes through Micrometer (at {@code /management/prometheus} where Prometheus export is on):
 * <ul>
 *   <li>{@code datarun.etl.outbox.rows{status}}: outbox rows still pending (including leased ones), failed and
 *   waiting to be tried again, or dead;</li>
 *   <li>{@code datarun.etl.outbox.oldest.age}: seconds since the oldest change not yet projected (pending or
 *   failed) was written, 0 when there is none;</li>
 *   <li>{@code datarun.etl.last.success}: when the ETL last finished a poll without error or stop, in seconds
 *   since the epoch; empty until the first one since the application started;</li>
 *   <li>{@code datarun.etl.polls{outcome}}: polls that succeeded, stopped on a lost connection or an interrupt,
 *   or failed;</li>
 *   <li>{@code datarun.etl.outbox.processed{outcome}}: rows projected ({@code success}), failed to be tried
 *   again ({@code failed}) or given up ({@code dead});</li>
 *   <li>{@code datarun.etl.outbox.processing}: the time each row took.</li>
 * </ul>
 * The backlog gauges show a reading of the outbox that a scheduled task takes every
 * {@code datarun.etl.backlog-refresh}, apart from the ETL's own runs, so they stay true when the ETL itself is
 * stuck, and a scrape never waits on the database: when the pool is exhausted, a scrape still answers with the
 * last reading. Where nothing schedules that task (the test profiles, or an instance with the ETL switched off),
 * the gauges read the outbox when scraped, at most once per {@code datarun.etl.backlog-refresh}.
 */
@Component
public class EtlMetrics implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(EtlMetrics.class);

    /** outbox.created_at is the database's time in UTC, so the age is taken against UTC, whatever the session's zone. */
    private static final String BACKLOG_SQL = """
        SELECT count(*) FILTER (WHERE status = 'pending') AS pending,
               count(*) FILTER (WHERE status = 'failed') AS failed,
               count(*) FILTER (WHERE status = 'dead') AS dead,
               COALESCE(extract(epoch FROM timezone('UTC', now())
                   - min(created_at) FILTER (WHERE status IN ('pending', 'failed'))), 0) AS oldest_age
        FROM outbox
        WHERE status IN ('pending', 'failed', 'dead')
        """;

    public enum Outcome { SUCCESS, FAILED, DEAD }

    private record Backlog(double pending, double failed, double dead, double oldestAgeSeconds) {
        static final Backlog UNKNOWN = new Backlog(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    private final JdbcTemplate jdbc;
    private final EtlProperties properties;
    private final long refreshNanos;
    private final Map<Outcome, Counter> processed;
    private final Counter pollsSucceeded;
    private final Counter pollsStopped;
    private final Counter pollsFailed;
    private final Timer processing;
    private final AtomicLong lastSuccessMillis = new AtomicLong();

    private volatile Backlog backlog = Backlog.UNKNOWN;
    private volatile long readAtNanos;
    private volatile boolean read;
    private volatile boolean refreshedInBackground;

    public EtlMetrics(MeterRegistry registry, DataSource dataSource, EtlProperties properties) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(5);
        this.properties = properties;
        this.refreshNanos = properties.getBacklogRefresh().toNanos();

        for (String status : new String[]{"pending", "failed", "dead"}) {
            Gauge.builder("datarun.etl.outbox.rows", this, metrics -> metrics.backlog(status))
                .description("Outbox rows by status, among those the ETL has not finished with")
                .tag("status", status)
                .strongReference(true)
                .register(registry);
        }
        Gauge.builder("datarun.etl.outbox.oldest.age", this, metrics -> metrics.currentBacklog().oldestAgeSeconds())
            .description("Seconds since the oldest change the ETL has not projected yet was written")
            .baseUnit("seconds")
            .strongReference(true)
            .register(registry);
        Gauge.builder("datarun.etl.last.success", lastSuccessMillis,
                millis -> millis.get() == 0 ? Double.NaN : millis.get() / 1000.0)
            .description("When the ETL last finished a poll without error, in seconds since the epoch")
            .baseUnit("seconds")
            .strongReference(true)
            .register(registry);
        this.processed = Map.of(
            Outcome.SUCCESS, outcomeCounter(registry, "success"),
            Outcome.FAILED, outcomeCounter(registry, "failed"),
            Outcome.DEAD, outcomeCounter(registry, "dead"));
        this.pollsSucceeded = pollCounter(registry, "succeeded");
        this.pollsStopped = pollCounter(registry, "stopped");
        this.pollsFailed = pollCounter(registry, "failed");
        this.processing = Timer.builder("datarun.etl.outbox.processing")
            .description("Time the ETL took to process one outbox row")
            .register(registry);
    }

    private static Counter outcomeCounter(MeterRegistry registry, String outcome) {
        return Counter.builder("datarun.etl.outbox.processed")
            .description("Outbox rows processed, by outcome")
            .tag("outcome", outcome)
            .register(registry);
    }

    private static Counter pollCounter(MeterRegistry registry, String outcome) {
        return Counter.builder("datarun.etl.polls")
            .description("ETL polls, by outcome")
            .tag("outcome", outcome)
            .register(registry);
    }

    public void rowProcessed(Outcome outcome, Duration took) {
        processed.get(outcome).increment();
        processing.record(took);
    }

    public void pollSucceeded() {
        pollsSucceeded.increment();
        lastSuccessMillis.set(System.currentTimeMillis());
    }

    /** A poll whose batch stopped on a lost connection or an interrupt: not a success. */
    public void pollStopped() {
        pollsStopped.increment();
    }

    public void pollFailed() {
        pollsFailed.increment();
    }

    private double backlog(String status) {
        Backlog current = currentBacklog();
        return switch (status) {
            case "pending" -> current.pending();
            case "failed" -> current.failed();
            default -> current.dead();
        };
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        Duration every = properties.getBacklogRefresh();
        if (!properties.isEnabled() || every.isZero() || every.isNegative()) {
            return;
        }
        refreshedInBackground = true;
        registrar.addFixedDelayTask(new FixedDelayTask(this::refreshBacklog, every, Duration.ZERO));
    }

    /** Reads the outbox backlog now; the gauges show this reading until the next one. */
    public synchronized void refreshBacklog() {
        try {
            backlog = jdbc.queryForObject(BACKLOG_SQL, (rs, rowNum) -> new Backlog(
                rs.getLong("pending"), rs.getLong("failed"), rs.getLong("dead"), rs.getDouble("oldest_age")));
        } catch (RuntimeException e) {
            // Keep the last reading; last.success still shows whether the ETL itself is getting through.
            log.warn("Could not read the outbox backlog: {}", e.getMessage());
        }
        readAtNanos = System.nanoTime();
        read = true;
    }

    private Backlog currentBacklog() {
        if (!refreshedInBackground && (!read || System.nanoTime() - readAtNanos >= refreshNanos)) {
            synchronized (this) {
                if (!read || System.nanoTime() - readAtNanos >= refreshNanos) {
                    refreshBacklog();
                }
            }
        }
        return backlog;
    }
}
