package com.example.tms.dao;

import com.example.tms.Database;
import com.example.tms.model.Comment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    // We'll still use this to format "now" as a string that matches SQLite's default
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get all comments for a given task, ordered oldest first.
     */
    public List<Comment> getByTask(int taskId) throws Exception {
        String sql = "SELECT id, task_id, user_id, text, created_at " +
                     "FROM comments WHERE task_id = ? ORDER BY created_at ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, taskId);
            ResultSet rs = stmt.executeQuery();

            List<Comment> list = new ArrayList<>();

            while (rs.next()) {
                Comment c = new Comment();
                c.setId(rs.getInt("id"));
                c.setTaskId(rs.getInt("task_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setText(rs.getString("text"));

                // created_at as a plain String, matches Comment.setCreatedAt(String)
                String ts = rs.getString("created_at");
                c.setCreatedAt(ts);

                list.add(c);
            }

            return list;
        }
    }

    /**
     * Create a new comment for a task.
     */
    public Comment create(int taskId, int userId, String text) throws Exception {
        String sql = "INSERT INTO comments (task_id, user_id, text, created_at) " +
                     "VALUES (?, ?, ?, datetime('now'))";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            stmt.setString(3, text);

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            Comment c = new Comment();

            if (keys.next()) {
                c.setId(keys.getInt(1));
            }

            c.setTaskId(taskId);
            c.setUserId(userId);
            c.setText(text);

            // Store "now" as a String, matching the model's setCreatedAt(String)
            String nowTs = LocalDateTime.now().format(FORMATTER);
            c.setCreatedAt(nowTs);

            return c;
        }
    }
}
