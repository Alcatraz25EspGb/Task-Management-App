package com.example.tms.dao;

import com.example.tms.Database;
import com.example.tms.model.Task;
import com.example.tms.model.TaskStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    private Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            t.setStatus(TaskStatus.valueOf(statusStr));
        }

        t.setCategory(rs.getString("category"));
        t.setPriority(rs.getInt("priority"));
        t.setCreatedAt(rs.getString("created_at"));
        t.setUpdatedAt(rs.getString("updated_at"));
        t.setCompletedAt(rs.getString("completed_at"));
        t.setDueAt(rs.getString("due_at"));
        t.setCreatedByUserId(rs.getInt("created_by_user_id"));
        t.setAssigneeId(rs.getInt("assignee_id"));
        t.setPendingReview(rs.getInt("pending_review") == 1);

        return t;
    }

    public List<Task> findAll() throws SQLException {
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC";
        List<Task> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Task findById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";

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

    public Task create(Task task) throws SQLException {
        String sql = """
            INSERT INTO tasks
              (title, description, status, category, priority,
               due_at, created_by_user_id, assignee_id, pending_review)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus() != null ? task.getStatus().name() : TaskStatus.TODO.name());
            stmt.setString(4, task.getCategory());
            stmt.setInt(5, task.getPriority());
            stmt.setString(6, task.getDueAt());
            stmt.setInt(7, task.getCreatedByUserId());
            stmt.setInt(8, task.getAssigneeId());
            stmt.setInt(9, task.isPendingReview() ? 1 : 0);

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return findById(id);
                }
            }
        }

        throw new SQLException("Failed to insert task");
    }

    public Task update(Task task) throws SQLException {
        String sql = """
            UPDATE tasks
               SET title = ?,
                   description = ?,
                   status = ?,
                   category = ?,
                   priority = ?,
                   due_at = ?,
                   assignee_id = ?,
                   pending_review = ?,
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus() != null ? task.getStatus().name() : TaskStatus.TODO.name());
            stmt.setString(4, task.getCategory());
            stmt.setInt(5, task.getPriority());
            stmt.setString(6, task.getDueAt());
            stmt.setInt(7, task.getAssigneeId());
            stmt.setInt(8, task.isPendingReview() ? 1 : 0);
            stmt.setInt(9, task.getId());

            stmt.executeUpdate();
        }

        return findById(task.getId());
    }

    public Task updateStatus(int id, TaskStatus status, String completedAt) throws SQLException {
        String sql = """
            UPDATE tasks
               SET status = ?,
                   completed_at = ?,
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setString(2, completedAt);
            stmt.setInt(3, id);

            stmt.executeUpdate();
        }

        return findById(id);
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}
