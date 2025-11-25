package com.example.tms;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        // Trigger DB initialization
        new Database();

        port(4567);

        // We'll put HTML/JS later in src/main/resources/public
        staticFiles.location("/public");

        get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });
    }
}
