package edu.csci4420.commander;

import io.grpc.Status;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

final class Database {
    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());

    private final String url;
    private final String user;
    private final String password;

    Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    static Status failure(String operation, SQLException error) {
        String sqlState = error.getSQLState();
        // JDBC error text can include submitted values. Log only the diagnostic codes.
        LOGGER.warning(operation + " failed (SQL state " + sqlState + ", vendor code " + error.getErrorCode() + ")");
        Status status = sqlState != null && sqlState.startsWith("08") ? Status.UNAVAILABLE : Status.INTERNAL;
        return status.withDescription(operation + " could not be completed.");
    }
}
