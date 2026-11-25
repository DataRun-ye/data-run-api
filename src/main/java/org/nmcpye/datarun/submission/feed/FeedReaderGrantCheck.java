package org.nmcpye.datarun.submission.feed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Says at every start whether downstream can read schema {@code feed}: one WARN line naming the grant that is missing,
 * or one INFO line when the reader role has it. The grant itself is Liquibase changeset
 * {@value #GRANT_CHANGESET}, made at the first start where the role named by {@value FeedReaderRole#PROPERTY} exists
 * ({@link FeedReaderRole}). It reads the catalog only, and never fails a start.
 */
@Component
public class FeedReaderGrantCheck {

    static final String GRANT_CHANGESET = "20261125-d02-feed-reader-grant";

    private static final Logger log = LoggerFactory.getLogger(FeedReaderGrantCheck.class);

    /**
     * Whether the role exists, schema feed exists, the role can read the schema (USAGE, and SELECT on every table in
     * it), and the grant's changeset is recorded as run.
     */
    private static final String STATE = """
        SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :role) AS role_exists,
               EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'feed') AS schema_exists,
               coalesce((SELECT has_schema_privilege(r.oid, n.oid, 'USAGE')
                                AND coalesce(bool_and(has_table_privilege(r.oid, c.oid, 'SELECT')), true)
                         FROM pg_roles r
                         CROSS JOIN pg_namespace n
                         LEFT JOIN pg_class c ON c.relnamespace = n.oid AND c.relkind IN ('r', 'p', 'v', 'm')
                         WHERE r.rolname = :role AND n.nspname = 'feed'
                         GROUP BY r.oid, n.oid), false) AS readable,
               EXISTS (SELECT 1 FROM databasechangelog WHERE id = :changeset) AS grant_recorded""";

    private final NamedParameterJdbcTemplate jdbc;
    private final Environment environment;

    public FeedReaderGrantCheck(NamedParameterJdbcTemplate jdbc, Environment environment) {
        this.jdbc = jdbc;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkAtStart() {
        check(FeedReaderRole.configured(environment), this::read);
    }

    /** What the catalog says about a role and schema feed. */
    record State(boolean roleExists, boolean schemaExists, boolean readable, boolean grantRecorded) {
    }

    /** Reads the catalog for one role. */
    interface Catalog {
        State read(String role);
    }

    private State read(String role) {
        return jdbc.queryForObject(STATE, Map.of("role", role, "changeset", GRANT_CHANGESET), (rs, n) -> new State(
            rs.getBoolean("role_exists"),
            rs.getBoolean("schema_exists"),
            rs.getBoolean("readable"),
            rs.getBoolean("grant_recorded")));
    }

    /** Logs one line: what is missing for downstream to read schema feed, or that nothing is. */
    static void check(Optional<String> configured, Catalog catalog) {
        if (configured.isEmpty()) {
            log.warn("Downstream's grant on schema feed is not made: no reader role is set ({})",
                FeedReaderRole.PROPERTY);
            return;
        }
        String role = configured.get();
        if (!FeedReaderRole.isPlain(role)) {
            log.warn("Downstream's grant on schema feed is not made: {} is not a plain role name (letters, digits, "
                + "_ and -, at most 63 characters)", FeedReaderRole.PROPERTY);
            return;
        }
        State state;
        try {
            state = catalog.read(role);
        } catch (DataAccessException e) {
            log.warn("Could not check downstream's grant on schema feed for reader role {}: {}", role,
                e.getClass().getSimpleName());
            return;
        }
        if (!state.schemaExists()) {
            log.warn("Downstream's grant on schema feed is not made: schema feed does not exist");
        } else if (!state.roleExists()) {
            log.warn("Downstream's grant on schema feed is not made: reader role {} does not exist; the first start "
                + "after it is created makes it", role);
        } else if (state.readable()) {
            log.info("Schema feed is readable by reader role {}", role);
        } else if (state.grantRecorded()) {
            log.warn("Reader role {} cannot read schema feed, and changeset {} already ran (for this role or another): "
                + "grant it USAGE on feed, SELECT on its tables and default privileges by hand", role, GRANT_CHANGESET);
        } else {
            log.warn("Downstream's grant on schema feed is not made yet for reader role {}, created after this start's "
                + "migrations: the next start makes it", role);
        }
    }
}
