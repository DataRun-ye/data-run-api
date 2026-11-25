package org.nmcpye.datarun.common.exceptions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * A failure described for a log line without its message, which can quote a submitted value, a request body or the
 * database's row: its exception types, its SQLState when there is one, and the application frames it was thrown
 * through, which is what a diagnosis starts from.
 */
public final class FailureShape {

    private static final String APPLICATION = "org.nmcpye.datarun.";
    private static final int FRAMES = 3;

    private FailureShape() {
    }

    /**
     * The exception types from the outermost to the root cause, the first SQLState among them, and the root cause's
     * first application frames, for example {@code DataIntegrityViolationException < PSQLException, SQLState 23505,
     * at OutboxWriter.batchInsert:67 < DefaultDataSubmissionService.upsert:113}.
     */
    public static String of(Throwable failure) {
        List<String> types = new ArrayList<>();
        String sqlState = null;
        Throwable root = failure;
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            types.add(cause.getClass().getSimpleName());
            if (sqlState == null && cause instanceof SQLException sql) {
                sqlState = sql.getSQLState();
            }
            root = cause;
        }
        StringBuilder shape = new StringBuilder(String.join(" < ", types));
        if (sqlState != null) {
            shape.append(", SQLState ").append(sqlState);
        }
        List<String> frames = applicationFrames(root);
        if (frames.isEmpty() && root != failure) {
            frames = applicationFrames(failure);
        }
        if (!frames.isEmpty()) {
            shape.append(", at ").append(String.join(" < ", frames));
        }
        return shape.toString();
    }

    private static List<String> applicationFrames(Throwable thrown) {
        List<String> frames = new ArrayList<>();
        for (StackTraceElement frame : thrown.getStackTrace()) {
            if (frames.size() == FRAMES) {
                break;
            }
            String type = frame.getClassName();
            if (type.startsWith(APPLICATION) && !type.contains("$$")) {
                frames.add(type.substring(type.lastIndexOf('.') + 1) + "." + frame.getMethodName()
                    + ":" + frame.getLineNumber());
            }
        }
        return frames;
    }
}
