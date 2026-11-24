package org.nmcpye.datarun.common.exceptions;

import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What a database failure says about itself without its message: the SQLState and the name of the violated
 * constraint. Both are schema facts, never data, while the message of a database error can quote the failing row or
 * an input value.
 */
public final class DatabaseFailures {

    private DatabaseFailures() {
    }

    /** The first SQLState in the failure, its causes and chained SQL exceptions. */
    public static Optional<String> sqlState(Throwable failure) {
        for (Throwable part : parts(failure)) {
            if (part instanceof SQLException sql && sql.getSQLState() != null) {
                return Optional.of(sql.getSQLState());
            }
        }
        return Optional.empty();
    }

    /**
     * The violated constraint's name: from the server's structured error (any locale), else the name Hibernate read
     * from the error's text.
     */
    public static Optional<String> constraint(Throwable failure) {
        String parsed = null;
        for (Throwable part : parts(failure)) {
            if (part instanceof PSQLException psql) {
                ServerErrorMessage server = psql.getServerErrorMessage();
                if (server != null && server.getConstraint() != null) {
                    return Optional.of(server.getConstraint());
                }
            }
            if (parsed == null && part instanceof ConstraintViolationException hibernate) {
                parsed = hibernate.getConstraintName();
            }
        }
        return Optional.ofNullable(parsed);
    }

    /** The failure, its causes and the SQL exceptions chained to any of them, each once. */
    private static List<Throwable> parts(Throwable failure) {
        List<Throwable> parts = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(failure, parts, seen);
        return parts;
    }

    private static void collect(Throwable failure, List<Throwable> parts, Set<Throwable> seen) {
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            parts.add(cause);
            if (cause instanceof SQLException sql && sql.getNextException() != null) {
                collect(sql.getNextException(), parts, seen);
            }
        }
    }
}
