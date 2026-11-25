package org.nmcpye.datarun.submission.feed;

import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Converges the grants on schema {@code feed} to downstream's reader role ({@link FeedReaderRole}) at every start
 * (contract 10 and 13: USAGE on the schema, SELECT on its tables, and default privileges for tables created later).
 *
 * <ul>
 *   <li>A role is named and exists: it gets USAGE on the schema, SELECT on every table and view, and SELECT on the
 *       tables the application's role creates there later, and keeps nothing more: no other privilege, nothing on a
 *       sequence. Every other grantee except an object's owner, PUBLIC included, loses what it holds on the schema,
 *       its tables, views and sequences, and in the schema's default privileges. Naming another role moves the grant
 *       to it.</li>
 *   <li>A role is named that does not exist, or a name that is not a plain role name: nothing changes, and a WARN says
 *       so.</li>
 *   <li>No role is named: nothing changes, and a WARN says so. Nothing is revoked then, so a lost setting cannot cut
 *       downstream off.</li>
 * </ul>
 * <p>Schema {@code feed_ops} is never touched. The database builds every statement, quoting each name with
 * {@code format('%I')}, and all of them run in one transaction, which a failure rolls back whole; a failure is logged
 * and never fails a start.</p>
 */
@Component
public class FeedReaderGrants {

    private static final Logger log = LoggerFactory.getLogger(FeedReaderGrants.class);

    /** The privilege names the catalog gives, which statements take as written. */
    private static final Pattern PRIVILEGE = Pattern.compile("[A-Z]+");

    /** What non-owners hold on the schema. */
    private static final String SCHEMA_GRANTS = """
        SELECT a.grantee AS grantee_oid, r.rolname AS grantee, a.privilege_type
        FROM pg_namespace n
        CROSS JOIN LATERAL aclexplode(n.nspacl) a
        LEFT JOIN pg_roles r ON r.oid = a.grantee
        WHERE n.nspname = 'feed' AND a.grantee <> n.nspowner
        ORDER BY 2, 3""";

    /** What non-owners hold on the schema's tables, views and sequences. */
    private static final String RELATION_GRANTS = """
        SELECT c.relname, c.relkind = 'S' AS sequence, a.grantee AS grantee_oid, r.rolname AS grantee,
               a.privilege_type
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        CROSS JOIN LATERAL aclexplode(c.relacl) a
        LEFT JOIN pg_roles r ON r.oid = a.grantee
        WHERE n.nspname = 'feed' AND c.relkind IN ('r', 'p', 'v', 'm', 'f', 'S') AND a.grantee <> c.relowner
        ORDER BY 1, 4, 5""";

    /** The schema's tables and views. */
    private static final String TABLES = """
        SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'feed' AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
        ORDER BY 1""";

    /** The application role's default privileges in the schema, for others, by kind of object. */
    private static final String DEFAULT_GRANTS = """
        SELECT d.defaclobjtype AS kind, a.grantee AS grantee_oid, r.rolname AS grantee, a.privilege_type
        FROM pg_default_acl d
        JOIN pg_namespace n ON n.oid = d.defaclnamespace
        CROSS JOIN LATERAL aclexplode(d.defaclacl) a
        LEFT JOIN pg_roles r ON r.oid = a.grantee
        WHERE n.nspname = 'feed' AND d.defaclrole = (SELECT oid FROM pg_roles WHERE rolname = current_user)
          AND a.grantee <> d.defaclrole
        ORDER BY 1, 3, 4""";

    private static final Map<String, String> DEFAULT_KINDS = Map.of(
        "r", "TABLES", "S", "SEQUENCES", "f", "FUNCTIONS", "T", "TYPES");

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Environment environment;

    public FeedReaderGrants(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                            Environment environment) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void convergeAtStart() {
        converge(FeedReaderRole.configured(environment));
    }

    /** Converges the grants on schema feed to the role named, if one is; never throws. */
    void converge(Optional<String> configured) {
        if (configured.isEmpty()) {
            log.warn("Downstream's grant on schema feed is not made: no reader role is set ({}); grants on feed are "
                + "left as they are", FeedReaderRole.PROPERTY);
            return;
        }
        String role = configured.get();
        if (!FeedReaderRole.isPlain(role)) {
            log.warn("Downstream's grant on schema feed is not made: {} is not a plain role name (letters, digits, "
                + "_ and -, at most 63 characters); grants on feed are left as they are", FeedReaderRole.PROPERTY);
            return;
        }
        try {
            transactions.executeWithoutResult(status -> converge(role));
        } catch (RuntimeException e) {
            log.warn("Could not converge the grants on schema feed to reader role {}: {}", role, FailureShape.of(e));
        }
    }

    private void converge(String role) {
        jdbc.update("SET LOCAL lock_timeout = '5s'", Map.of());
        jdbc.update("SET LOCAL statement_timeout = '30s'", Map.of());
        if (!Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'feed')", Map.of(), Boolean.class))) {
            log.warn("Downstream's grant on schema feed is not made: schema feed does not exist");
            return;
        }
        Long reader = jdbc.query("SELECT oid FROM pg_roles WHERE rolname = :role", Map.of("role", role),
            rs -> rs.next() ? rs.getLong(1) : null);
        if (reader == null) {
            log.warn("Downstream's grant on schema feed is not made: reader role {} does not exist; grants on feed are "
                + "left as they are", role);
            return;
        }
        Set<String> statements = new LinkedHashSet<>();

        boolean usage = false;
        for (Map<String, Object> grant : jdbc.queryForList(SCHEMA_GRANTS, Map.of())) {
            String privilege = privilege(grant);
            if (isReader(grant, reader)) {
                if (privilege.equals("USAGE")) {
                    usage = true;
                } else {
                    statements.add(statement("REVOKE " + privilege + " ON SCHEMA feed FROM %I", role));
                }
            } else {
                statements.add(fromGrantee("REVOKE ALL ON SCHEMA feed FROM ", grant));
            }
        }
        if (!usage) {
            statements.add(statement("GRANT USAGE ON SCHEMA feed TO %I", role));
        }

        Set<String> readable = new LinkedHashSet<>();
        for (Map<String, Object> grant : jdbc.queryForList(RELATION_GRANTS, Map.of())) {
            String relation = (String) grant.get("relname");
            String on = Boolean.TRUE.equals(grant.get("sequence")) ? "SEQUENCE" : "TABLE";
            String privilege = privilege(grant);
            if (isReader(grant, reader)) {
                if (on.equals("TABLE") && privilege.equals("SELECT")) {
                    readable.add(relation);
                } else {
                    statements.add(statement("REVOKE " + privilege + " ON " + on + " feed.%I FROM %I", relation,
                        role));
                }
            } else {
                statements.add(fromGrantee(statement("REVOKE ALL ON " + on + " feed.%I FROM ", relation), grant));
            }
        }
        for (String table : jdbc.queryForList(TABLES, Map.of(), String.class)) {
            if (!readable.contains(table)) {
                statements.add(statement("GRANT SELECT ON TABLE feed.%I TO %I", table, role));
            }
        }

        boolean futureTables = false;
        for (Map<String, Object> grant : jdbc.queryForList(DEFAULT_GRANTS, Map.of())) {
            String kind = DEFAULT_KINDS.get(String.valueOf(grant.get("kind")));
            if (kind == null) {
                continue;
            }
            String privilege = privilege(grant);
            if (isReader(grant, reader)) {
                if (kind.equals("TABLES") && privilege.equals("SELECT")) {
                    futureTables = true;
                } else {
                    statements.add(statement("ALTER DEFAULT PRIVILEGES IN SCHEMA feed REVOKE " + privilege + " ON "
                        + kind + " FROM %I", role));
                }
            } else {
                statements.add(fromGrantee("ALTER DEFAULT PRIVILEGES IN SCHEMA feed REVOKE ALL ON " + kind + " FROM ",
                    grant));
            }
        }
        if (!futureTables) {
            statements.add(statement("ALTER DEFAULT PRIVILEGES IN SCHEMA feed GRANT SELECT ON TABLES TO %I", role));
        }

        statements.forEach(statement -> jdbc.getJdbcTemplate().execute(statement));
        log.info("Schema feed is readable by reader role {}, and by no other role ({} grants made or revoked)", role,
            statements.size());
    }

    private static boolean isReader(Map<String, Object> grant, long reader) {
        return ((Number) grant.get("grantee_oid")).longValue() == reader;
    }

    private static String privilege(Map<String, Object> grant) {
        String privilege = String.valueOf(grant.get("privilege_type"));
        if (!PRIVILEGE.matcher(privilege).matches()) {
            throw new IllegalStateException("Unexpected privilege name in the catalog");
        }
        return privilege;
    }

    /** A statement ending in its grantee: PUBLIC, or the role, quoted. */
    private String fromGrantee(String beginning, Map<String, Object> grant) {
        return ((Number) grant.get("grantee_oid")).longValue() == 0 ? beginning + "PUBLIC"
            : statement(beginning.replace("%", "%%") + "%I", (String) grant.get("grantee"));
    }

    /** The statement {@code format(template, names...)} builds in the database: each %I a name, quoted. */
    private String statement(String template, String... names) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("template", template);
        StringBuilder call = new StringBuilder("SELECT format(:template");
        for (int i = 0; i < names.length; i++) {
            parameters.addValue("name" + i, names[i]);
            call.append(", CAST(:name").append(i).append(" AS text)");
        }
        return jdbc.queryForObject(call.append(')').toString(), parameters, String.class);
    }
}
