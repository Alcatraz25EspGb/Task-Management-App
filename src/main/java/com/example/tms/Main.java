package com.example.tms;

import static spark.Spark.*;

import com.example.tms.dao.CommentDAO;
import com.example.tms.dao.NotificationDAO;
import com.example.tms.dao.TaskDAO;
import com.example.tms.dao.UserDAO;
import com.example.tms.model.Comment;
import com.example.tms.model.Notification;
import com.example.tms.model.Task;
import com.example.tms.model.TaskStatus;
import com.example.tms.model.User;
import com.example.tms.model.UserRole;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        // Initialize DB
        new Database();

        port(4567);
        staticFiles.location("/public");

        // Simple CORS
        options("/*", (req, res) -> {
            String requestHeaders = req.headers("Access-Control-Request-Headers");
            if (requestHeaders != null) {
                res.header("Access-Control-Allow-Headers", requestHeaders);
            }

            String requestMethod = req.headers("Access-Control-Request-Method");
            if (requestMethod != null) {
                res.header("Access-Control-Allow-Methods", requestMethod);
            }

            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.type("application/json");
        });

        // Health check
        get("/api/health", (req, res) -> "{\"status\":\"ok\"}");

        // DAOs
        UserDAO userDAO = new UserDAO();
        TaskDAO taskDAO = new TaskDAO();
        CommentDAO commentDAO = new CommentDAO();
        NotificationDAO notificationDAO = new NotificationDAO();

        // ---------------------------------------------
        // AUTH ROUTES
        // ---------------------------------------------

        // Register
        post("/api/auth/register", (req, res) -> {
            RegisterRequest body = gson.fromJson(req.body(), RegisterRequest.class);

            if (body == null || body.username == null || body.email == null ||
                body.password == null || body.role == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Missing fields"));
            }

            UserRole role;
            try {
                role = UserRole.valueOf(body.role);
            } catch (IllegalArgumentException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid role"));
            }

            String passwordHash = BCrypt.hashpw(body.password, BCrypt.gensalt());

            try {
                User existing = userDAO.findByUsername(body.username);
                if (existing != null) {
                    res.status(409);
                    return gson.toJson(new ErrorResponse("Username already taken"));
                }

                User user = userDAO.createUser(body.username, body.email, passwordHash, role);
                res.status(201);
                return gson.toJson(new UserResponse(user));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Login
        post("/api/auth/login", (req, res) -> {
            LoginRequest body = gson.fromJson(req.body(), LoginRequest.class);

            if (body == null || body.username == null || body.password == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Missing username or password"));
            }

            try {
                User user = userDAO.findByUsername(body.username);
                if (user == null) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Invalid credentials"));
                }

                if (!BCrypt.checkpw(body.password, user.getPasswordHash())) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Invalid credentials"));
                }

                req.session(true).attribute("userId", user.getId());

                return gson.toJson(new UserResponse(user));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Get current user
        get("/api/auth/me", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }
            try {
                User user = userDAO.findById(userId);
                if (user == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }
                return gson.toJson(new UserResponse(user));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // ---------------------------------------------
        // USERS LIST (for username mapping)
        // ---------------------------------------------

        get("/api/users", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            String sql = "SELECT id, username, role FROM users";
            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                ArrayList<UserSummary> list = new ArrayList<>();
                while (rs.next()) {
                    UserSummary u = new UserSummary();
                    u.id = rs.getInt("id");
                    u.username = rs.getString("username");
                    u.role = rs.getString("role");
                    list.add(u);
                }
                return gson.toJson(list);
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // ---------------------------------------------
        // TASK ROUTES
        // ---------------------------------------------

        // Get all tasks
        get("/api/tasks", (req, res) -> {
            try {
                List<Task> tasks = taskDAO.findAll();
                return gson.toJson(tasks);
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Get one task by ID
        get("/api/tasks/:id", (req, res) -> {
            try {
                int id = Integer.parseInt(req.params(":id"));
                Task task = taskDAO.findById(id);
                if (task == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }
                return gson.toJson(task);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Create new task (Manager/Admin only)
        post("/api/tasks", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                User currentUser = userDAO.findById(userId);
                if (currentUser == null) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("User not found"));
                }

                if (currentUser.getRole() == UserRole.Staff) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Staff users cannot create tasks"));
                }

                TaskCreateRequest body = gson.fromJson(req.body(), TaskCreateRequest.class);
                if (body == null || body.title == null || body.title.isBlank()) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Missing title"));
                }

                Task task = new Task();
                task.setTitle(body.title);
                task.setDescription(body.description != null ? body.description : "");

                TaskStatus status;
                try {
                    status = body.status != null ? TaskStatus.valueOf(body.status) : TaskStatus.TODO;
                } catch (IllegalArgumentException e) {
                    status = TaskStatus.TODO;
                }
                task.setStatus(status);

                task.setCategory(body.category != null ? body.category : "one-time");
                int priority = (body.priority != null && body.priority > 0) ? body.priority : 3;
                task.setPriority(priority);
                task.setCreatedByUserId(userId);
                task.setDueAt(body.dueAt);

                int assigneeId = userId;
                if (body.assigneeUsername != null && !body.assigneeUsername.isBlank()) {
                    User assignee = userDAO.findByUsername(body.assigneeUsername);
                    if (assignee == null) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("Unknown assignee username"));
                    }
                    assigneeId = assignee.getId();
                } else if (body.assigneeId != null && body.assigneeId > 0) {
                    assigneeId = body.assigneeId;
                }
                task.setAssigneeId(assigneeId);
                task.setPendingReview(false);

                Task created = taskDAO.create(task);

                if (created.getAssigneeId() != created.getCreatedByUserId()) {
                    notificationDAO.create(
                        created.getAssigneeId(),
                        created.getId(),
                        "assigned",
                        "You have been assigned a new task: " + created.getTitle()
                    );
                }

                res.status(201);
                return gson.toJson(created);
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Update a task (full update)
        put("/api/tasks/:id", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int id = Integer.parseInt(req.params(":id"));
                Task existing = taskDAO.findById(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                User currentUser = userDAO.findById(userId);
                boolean isOwner = existing.getCreatedByUserId() == userId;
                boolean isManager = currentUser.getRole() == UserRole.Manager || currentUser.getRole() == UserRole.Admin;

                if (!isOwner && !isManager) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Forbidden"));
                }

                Task body = gson.fromJson(req.body(), Task.class);

                if (body.getTitle() != null && !body.getTitle().isBlank()) {
                    existing.setTitle(body.getTitle());
                }
                existing.setDescription(body.getDescription());
                if (body.getCategory() != null) {
                    existing.setCategory(body.getCategory());
                }
                if (body.getPriority() != 0) {
                    existing.setPriority(body.getPriority());
                }
                existing.setDueAt(body.getDueAt());
                existing.setAssigneeId(body.getAssigneeId());
                if (body.getStatus() != null) {
                    existing.setStatus(body.getStatus());
                }

                Task updated = taskDAO.update(existing);
                return gson.toJson(updated);

            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Update only the status
        patch("/api/tasks/:id/status", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int id = Integer.parseInt(req.params(":id"));
                Task existing = taskDAO.findById(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                StatusUpdateRequest body = gson.fromJson(req.body(), StatusUpdateRequest.class);
                if (body == null || body.status == null) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Missing status"));
                }

                TaskStatus newStatus;
                try {
                    newStatus = TaskStatus.valueOf(body.status);
                } catch (IllegalArgumentException e) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Invalid status"));
                }

                String completedAt = null;
                if (newStatus == TaskStatus.DONE) {
                    completedAt = java.time.LocalDateTime.now().toString();
                }

                Task updated = taskDAO.updateStatus(id, newStatus, completedAt);

                if (updated.getAssigneeId() != 0) {
                    notificationDAO.create(
                        updated.getAssigneeId(),
                        updated.getId(),
                        "status-changed",
                        "Task status changed to " + newStatus.name()
                    );
                }
                if (updated.getCreatedByUserId() != userId) {
                    notificationDAO.create(
                        updated.getCreatedByUserId(),
                        updated.getId(),
                        "status-changed",
                        "Task status changed to " + newStatus.name()
                    );
                }

                return gson.toJson(updated);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Staff submit task for review
        patch("/api/tasks/:id/submit", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int id = Integer.parseInt(req.params(":id"));
                Task existing = taskDAO.findById(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                User currentUser = userDAO.findById(userId);
                if (currentUser.getRole() != UserRole.Staff || existing.getAssigneeId() != userId) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Only assigned staff can submit tasks for review"));
                }

                if (existing.isPendingReview()) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Task is already pending review"));
                }

                existing.setPendingReview(true);
                Task updated = taskDAO.update(existing);

                notificationDAO.create(
                    updated.getCreatedByUserId(),
                    updated.getId(),
                    "review-requested",
                    "Task submitted for review: " + updated.getTitle()
                );

                return gson.toJson(updated);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Manager/Admin approve task
        patch("/api/tasks/:id/approve", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int id = Integer.parseInt(req.params(":id"));
                Task existing = taskDAO.findById(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                User currentUser = userDAO.findById(userId);
                boolean isOwner = existing.getCreatedByUserId() == userId;
                boolean isManager = currentUser.getRole() == UserRole.Manager || currentUser.getRole() == UserRole.Admin;

                if (!isOwner && !isManager) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Forbidden"));
                }

                existing.setPendingReview(false);
                Task updated = taskDAO.updateStatus(
                    id,
                    TaskStatus.DONE,
                    java.time.LocalDateTime.now().toString()
                );

                if (updated.getAssigneeId() != 0) {
                    notificationDAO.create(
                        updated.getAssigneeId(),
                        updated.getId(),
                        "review-approved",
                        "Your submitted task was approved: " + updated.getTitle()
                    );
                }

                return gson.toJson(updated);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Manager/Admin deny task
        patch("/api/tasks/:id/deny", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int id = Integer.parseInt(req.params(":id"));
                Task existing = taskDAO.findById(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                User currentUser = userDAO.findById(userId);
                boolean isOwner = existing.getCreatedByUserId() == userId;
                boolean isManager = currentUser.getRole() == UserRole.Manager || currentUser.getRole() == UserRole.Admin;

                if (!isOwner && !isManager) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Forbidden"));
                }

                existing.setPendingReview(false);
                Task updated = taskDAO.update(existing);

                if (updated.getAssigneeId() != 0) {
                    notificationDAO.create(
                        updated.getAssigneeId(),
                        updated.getId(),
                        "review-denied",
                        "Your submitted task was not approved: " + updated.getTitle()
                    );
                }

                return gson.toJson(updated);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // Delete task
        delete("/api/tasks/:id", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int id = Integer.parseInt(req.params(":id"));
                Task existing = taskDAO.findById(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                User currentUser = userDAO.findById(userId);
                boolean isOwner = existing.getCreatedByUserId() == userId;
                boolean isManager = currentUser.getRole() == UserRole.Manager || currentUser.getRole() == UserRole.Admin;

                if (!isOwner && !isManager) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Forbidden"));
                }

                taskDAO.delete(id);
                res.status(204);
                return "";
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // ---------------------------------------------
        // COMMENT ROUTES
        // ---------------------------------------------

        get("/api/tasks/:id/comments", (req, res) -> {
            try {
                int taskId = Integer.parseInt(req.params(":id"));
                Task task = taskDAO.findById(taskId);
                if (task == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                List<Comment> comments = commentDAO.getByTask(taskId);
                return gson.toJson(comments);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        post("/api/tasks/:id/comments", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int taskId = Integer.parseInt(req.params(":id"));
                Task task = taskDAO.findById(taskId);
                if (task == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Task not found"));
                }

                CommentRequest body = gson.fromJson(req.body(), CommentRequest.class);
                if (body == null || body.text == null || body.text.isBlank()) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Missing text"));
                }

                Comment created = commentDAO.create(taskId, userId, body.text);

                if (task.getCreatedByUserId() != userId) {
                    notificationDAO.create(
                        task.getCreatedByUserId(),
                        task.getId(),
                        "comment",
                        "New comment on task: " + task.getTitle()
                    );
                }
                if (task.getAssigneeId() != 0 && task.getAssigneeId() != userId) {
                    notificationDAO.create(
                        task.getAssigneeId(),
                        task.getId(),
                        "comment",
                        "New comment on task: " + task.getTitle()
                    );
                }

                res.status(201);
                return gson.toJson(created);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid task ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        // ---------------------------------------------
        // NOTIFICATION ROUTES
        // ---------------------------------------------

        get("/api/notifications", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                List<Notification> list = notificationDAO.findByUserId(userId);
                return gson.toJson(list);
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });

        patch("/api/notifications/:id/read", (req, res) -> {
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Not logged in"));
            }

            try {
                int id = Integer.parseInt(req.params(":id"));
                boolean ok = notificationDAO.markRead(id, userId);
                if (!ok) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Notification not found"));
                }
                return "{\"success\":true}";
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid notification ID"));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Server error"));
            }
        });
    }

    // DTOs

    static class ErrorResponse {
        String error;
        ErrorResponse(String error) {
            this.error = error;
        }
    }

    static class RegisterRequest {
        String username;
        String email;
        String password;
        String role;
    }

    static class LoginRequest {
        String username;
        String password;
    }

    static class CommentRequest {
        String text;
    }

    static class UserResponse {
        int id;
        String username;
        String email;
        String role;

        UserResponse(User u) {
            this.id = u.getId();
            this.username = u.getUsername();
            this.email = u.getEmail();
            this.role = u.getRole().name();
        }
    }

    static class StatusUpdateRequest {
        String status; // "TODO", "IN_PROGRESS", "DONE"
    }

    static class TaskCreateRequest {
        String title;
        String description;
        String category;
        Integer priority;
        String status;
        String dueAt;
        Integer assigneeId;
        String assigneeUsername;
    }

    static class UserSummary {
        int id;
        String username;
        String role;
    }
}
