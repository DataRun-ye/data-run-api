package org.nmcpye.datarun.etl;

import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A two-connection "pool" that hands out its connections in turn, so a test decides which physical
 * connection each borrow gets. A real pool makes the same choice by timing and load.
 */
final class AlternatingDataSource extends AbstractDataSource implements AutoCloseable {

    private final SingleConnectionDataSource[] connections = new SingleConnectionDataSource[2];
    private final AtomicInteger next = new AtomicInteger();

    AlternatingDataSource(String url, String user, String password) throws SQLException {
        for (int i = 0; i < connections.length; i++) {
            Connection physical = DriverManager.getConnection(url, user, password);
            physical.setAutoCommit(true);
            connections[i] = new SingleConnectionDataSource(physical, true);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connections[Math.floorMod(next.getAndIncrement(), connections.length)].getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public void close() {
        for (SingleConnectionDataSource connection : connections) {
            connection.destroy();
        }
    }
}
