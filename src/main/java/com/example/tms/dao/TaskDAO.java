package com.example.tms.dao;

import com.example.tms.Database;
import com.example.tms.model.Task;
import com.example.tms.model.TaskStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    // ------------------------
    // Internal helpers
    // ------------------------

    private Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));

        String statusStr = rs.getString("status");
        TaskStatus status;
        try {
            status = TaskStatus.valueOf(statusStr);
        } catch (IllegalArgumentException | NullPointerException ex) {
            status = TaskStatus.TODO;
        }
        t.setStatus(status);

        t.setCategory(rs.getString("category"));
        t.setPriority(rs.getInt("priority"));
        t.setCreatedAt(rs.getString("created_at"));
        t.setDueAt(rs.getString("due_at"));
        t.setCreatedByUserId(rs.getInt("created_by_user_id"));

        int assigneeId = rs.getInt("assignee_id");
        if (rs.wasNull()) {
            t.setAssigneeId(null);
        } else {
            t.setAssigneeId(assigneeId);
        }

        int pending = rs.getInt("pending_review");
        t.setPendingReview(pending == 1);

        t.setCompletedAt(rs.getString("completed_at"));

        return t;
    }

    private List<Integer> loadAssigneesForTask(Connection conn, int taskId) throws SQLException {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT user_id FROM task_assignees WHERE task_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt("user_id"));
                }
            }
        }
        return list;
    }

    private void saveAssigneesForTask(Connection conn, int taskId, Task task) throws SQLException {
        // Determine final assignee list: combine assigneeId + assigneeIds
        List<Integer> finalAssignees = new ArrayList<>();

        if (task.getAssigneeIds() != null) {
            for (Integer id : task.getAssigneeIds()) {
                if (id != null && !finalAssignees.contains(id)) {
                    finalAssignees.add(id);
                }
            }
        }

        if (task.getAssigneeId() != null && task.getAssigneeId() > 0 && !finalAssignees.contains(task.getAssigneeId())) {
            finalAssignees.add(task.getAssigneeId());
        }

        // Clear existing
        try (PreparedStatement del = conn.prepareStatement(
            "DELETE FROM task_assignees WHERE task_id = ?"
        )) {
            del.setInt(1, taskId);
            del.executeUpdate();
        }

        // Insert new ones
        String insertSql = "INSERT INTO task_assignees (task_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
            for (Integer userId : finalAssignees) {
                ins.setInt(1, taskId);
                ins.setInt(2, userId);
                ins.addBatch();
            }
            ins.executeBatch();
        }

        // Keep Task object in sync
        task.setAssigneeIds(finalAssignees);
        // Also set primary assigneeId as the first one, or null
        if (!finalAssignees.isEmpty()) {
            task.setAssigneeId(finalAssignees.get(0));
        } else {
            task.setAssigneeId(null);
        }
    }

    // ------------------------
    // CRUD methods
    // ------------------------

    public Task create(Task task) throws SQLException {
        String sql = """
                INSERT INTO tasks
                (title, description, status, category, priority,
                 created_by_user_id, assignee_id, due_at, pending_review, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String statusStr = task.getStatus() != null ? task.getStatus().name() : TaskStatus.TODO.name();

            // Determine primary assignee from list + single
            List<Integer> finalAssignees = new ArrayList<>();
            if (task.getAssigneeIds() != null) {
                for (Integer id : task.getAssigneeIds()) {
                    if (id != null && !finalAssignees.contains(id)) {
                        finalAssignees.add(id);
                    }
                }
            }
            if (task.getAssigneeId() != null && task.getAssigneeId() > 0 && !finalAssignees.contains(task.getAssigneeId())) {
                finalAssignees.add(task.getAssigneeId());
            }

            Integer primaryAssignee = finalAssignees.isEmpty() ? null : finalAssignees.get(0);

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, statusStr);
            stmt.setString(4, task.getCategory());
            stmt.setInt(5, task.getPriority());
            stmt.setInt(6, task.getCreatedByUserId());

            if (primaryAssignee == null) {
                stmt.setNull(7, Types.INTEGER);
            } else {
                stmt.setInt(7, primaryAssignee);
            }

            stmt.setString(8, task.getDueAt());
            stmt.setInt(9, task.isPendingReview() ? 1 : 0);
            stmt.setString(10, task.getCompletedAt());

            stmt.executeUpdate();

            int newId;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to get generated task ID");
                }
                newId = keys.getInt(1);
            }

            // Save assignees in link table
            saveAssigneesForTask(conn, newId, task);

            // Reload full task from DB
            return findById(newId);
        }
    }

    public Task findById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Task t = mapRow(rs);
                    List<Integer> assignees = loadAssigneesForTask(conn, id);

                    if (assignees.isEmpty() && t.getAssigneeId() != null) {
                        assignees.add(t.getAssigneeId());
                    }

                    t.setAssigneeIds(assignees);
                    if (!assignees.isEmpty()) {
                        t.setAssigneeId(assignees.get(0));
                    }
                    return t;
                }
                return null;
            }
        }
    }

    public List<Task> findAll() throws SQLException {
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC";
        List<Task> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Task t = mapRow(rs);
                List<Integer> assignees = loadAssigneesForTask(conn, t.getId());

                if (assignees.isEmpty() && t.getAssigneeId() != null) {
                    assignees.add(t.getAssigneeId());
                }

                t.setAssigneeIds(assignees);
                if (!assignees.isEmpty()) {
                    t.setAssigneeId(assignees.get(0));
                }

                list.add(t);
            }
        }

        return list;
    }

    /**
     * Find tasks where user is one of the assignees (used for Staff visibility).
     */
    public List<Task> findByAssignee(int userId) throws SQLException {
        String sql = """
                SELECT t.*
                FROM tasks t
                JOIN task_assignees ta ON ta.task_id = t.id
                WHERE ta.user_id = ?
                ORDER BY t.created_at DESC
                """;

        List<Task> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task t = mapRow(rs);
                    List<Integer> assignees = loadAssigneesForTask(conn, t.getId());

                    if (assignees.isEmpty() && t.getAssigneeId() != null) {
                        assignees.add(t.getAssigneeId());
                    }

                    t.setAssigneeIds(assignees);
                    if (!assignees.isEmpty()) {
                        t.setAssigneeId(assignees.get(0));
                    }

                    list.add(t);
                }
            }
        }
        return list;
    }

    public Task update(Task task) throws SQLException {
        String sql = """
                UPDATE tasks
                SET title = ?, description = ?, status = ?, category = ?, priority = ?,
                    due_at = ?, assignee_id = ?, pending_review = ?, completed_at = ?
                WHERE id = ?
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String statusStr = task.getStatus() != null ? task.getStatus().name() : TaskStatus.TODO.name();

            // Determine primary assignee from list + single
            List<Integer> finalAssignees = new ArrayList<>();
            if (task.getAssigneeIds() != null) {
                for (Integer id : task.getAssigneeIds()) {
                    if (id != null && !finalAssignees.contains(id)) {
                        finalAssignees.add(id);
                    }
                }
            }
            if (task.getAssigneeId() != null && task.getAssigneeId() > 0 && !finalAssignees.contains(task.getAssigneeId())) {
                finalAssignees.add(task.getAssigneeId());
            }
            Integer primaryAssignee = finalAssignees.isEmpty() ? null : finalAssignees.get(0);

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, statusStr);
            stmt.setString(4, task.getCategory());
            stmt.setInt(5, task.getPriority());
            stmt.setString(6, task.getDueAt());

            if (primaryAssignee == null) {
                stmt.setNull(7, Types.INTEGER);
            } else {
                stmt.setInt(7, primaryAssignee);
            }

            stmt.setInt(8, task.isPendingReview() ? 1 : 0);
            stmt.setString(9, task.getCompletedAt());
            stmt.setInt(10, task.getId());

            stmt.executeUpdate();

            // Save assignees link table
            saveAssigneesForTask(conn, task.getId(), task);

            return findById(task.getId());
        }
    }

    public Task updateStatus(int id, TaskStatus newStatus, String completedAt) throws SQLException {
        String sql = "UPDATE tasks SET status = ?, completed_at = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.name());
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
