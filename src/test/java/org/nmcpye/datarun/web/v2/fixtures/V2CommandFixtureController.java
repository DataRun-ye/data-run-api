package org.nmcpye.datarun.web.v2.fixtures;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.web.v2.idempotency.CommandOutcome;
import org.nmcpye.datarun.web.v2.idempotency.CommandRequest;
import org.nmcpye.datarun.web.v2.idempotency.IdempotentCommands;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A test-only v2 command over a versioned counter, built as a module's command must be: its first read locks the
 * row, it checks If-Match through its context, writes the change and a history row, and its renderer reads the
 * counter at the outcome's version from that history, as a module renders a past version from its journal.
 * Hidden from the OpenAPI document; it exists only on the test classpath, with tables its test creates.
 */
@Hidden
@RestController
@RequestMapping("/api/v2/fixtures")
public class V2CommandFixtureController {

    /** How many times the command body ran, to tell a replay from a second application. */
    public static final AtomicInteger RUNS = new AtomicInteger();

    public record Increment(@NotNull Integer by) {
    }

    public record Counter(String id, long version, long value) {
    }

    private final IdempotentCommands commands;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate reads;

    public V2CommandFixtureController(IdempotentCommands commands, NamedParameterJdbcTemplate jdbc,
                                      PlatformTransactionManager transactions) {
        this.commands = commands;
        this.jdbc = jdbc;
        this.reads = new TransactionTemplate(transactions);
        this.reads.setReadOnly(true);
    }

    @PostMapping(path = "/counters/{id}:increment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Counter> increment(@PathVariable String id, CommandRequest command,
                                             @Valid @RequestBody Increment increment) {
        return add(id, command, increment.by());
    }

    /** A command that reads no body: one sent anyway must not be hashed as empty. */
    @PostMapping("/counters/{id}:bump")
    public ResponseEntity<Counter> bump(@PathVariable String id, CommandRequest command) {
        return add(id, command, 1);
    }

    private ResponseEntity<Counter> add(String id, CommandRequest command, int by) {
        return commands.execute(command, context -> {
            RUNS.incrementAndGet();
            Map<String, Object> row;
            try {
                row = jdbc.queryForMap("SELECT version, value FROM v2_fixture_counter WHERE id = :id FOR UPDATE",
                    Map.of("id", id));
            } catch (EmptyResultDataAccessException e) {
                throw ApiProblemException.of(ProblemCode.NOT_FOUND);
            }
            long version = ((Number) row.get("version")).longValue();
            context.checkVersion(version, "/api/v2/fixtures/counters/" + id + "/versions");
            long value = ((Number) row.get("value")).longValue() + by;
            MapSqlParameterSource next = new MapSqlParameterSource()
                .addValue("id", id).addValue("version", version + 1).addValue("value", value);
            jdbc.update("UPDATE v2_fixture_counter SET version = :version, value = :value WHERE id = :id", next);
            jdbc.update("INSERT INTO v2_fixture_counter_version (id, version, value) VALUES (:id, :version, :value)",
                next);
            return CommandOutcome.ok("fixture-counter", id, version + 1);
        }, outcome -> reads.execute(status -> jdbc.queryForObject(
            "SELECT value FROM v2_fixture_counter_version WHERE id = :id AND version = :version",
            new MapSqlParameterSource().addValue("id", outcome.resourceId())
                .addValue("version", outcome.resourceVersion()),
            (rs, n) -> new Counter(outcome.resourceId(), outcome.resourceVersion(), rs.getLong(1)))));
    }
}
