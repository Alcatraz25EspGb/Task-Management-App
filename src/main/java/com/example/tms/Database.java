package com.example.tms;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class Database {

    private static final String DB_URL = "jdbc:sqlite:taskmanager.db";

    static {
        try {
            initDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void initDatabase() throws Exception {
        // Load schema.sql from resources
        InputStream inputStream = Database.class.getClassLoader().getResourceAsStream("schema.sql");

        if (inputStream == null) {
            throw new Exception("schema.sql not found in resources folder!");
        }

        String schemaSQL;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            schemaSQL = reader.lines().collect(Collectors.joining("\n"));
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(schemaSQL);
        }
    }
}
