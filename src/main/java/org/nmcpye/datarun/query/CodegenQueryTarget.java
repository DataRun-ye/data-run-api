package org.nmcpye.datarun.query;

import org.jooq.Record;
import org.jooq.Table;

import java.util.Objects;

/**
 * CodegenQueryTarget - wraps a concrete jOOQ-generated table
 *
 * @author Hamza Assada
 * @since 14/09/2025
 */
public final class CodegenQueryTarget implements QueryTarget {
    private final Table<? extends org.jooq.Record> table;
    private final String id;

    public CodegenQueryTarget(Table<? extends org.jooq.Record> table, String id) {
        this.table = Objects.requireNonNull(table);
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public Table<? extends Record> table() {
        return table;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean supportsDimension(String dimensionName) {
        // Trivial default. If you have metadata about table columns, consult it here.
        return true;
    }
}
