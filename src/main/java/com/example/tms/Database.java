package com.example.tms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Database {

    private static final String DB_FILE = "taskmanager.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }

        try {
            initDatabase();
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static void initDatabase() throws IOException, SQLException {
        Path schemaPath = Path.of("database", "schema.sql");
        if (!Files.exists(schemaPath)) {
            throw new IOException("schema.sql not found at " + schemaPath.toAbsolutePath());
        }

        String sql = Files.readString(schemaPath, StandardCharsets.UTF_8);

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
