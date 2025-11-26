package com.example.tms.dao;

import com.example.tms.Database;
import com.example.tms.model.Comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getInt("id"));
        c.setTaskId(rs.getInt("task_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setText(rs.getString("text"));
        c.setCreatedAt(rs.getString("created_at"));
        return c;
    }

    public Comment create(int taskId, int userId, String text) throws SQLException {
        String sql = "INSERT INTO comments (task_id, user_id, text) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            stmt.setString(3, text);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return findById(id);
                }
            }
        }

        throw new SQLException("Failed to insert comment");
    }

    public List<Comment> getByTask(int taskId) throws SQLException {
        String sql = "SELECT * FROM comments WHERE task_id = ? ORDER BY created_at ASC";
        List<Comment> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, taskId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    public Comment findById(int id) throws SQLException {
        String sql = "SELECT * FROM comments WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM comments WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int updated = stmt.executeUpdate();
            return updated > 0;
        }
    }
}
