package com.example.tms.model;

public class Task {

    private int id;
    private String title;
    private String description;
    private TaskStatus status;    // TODO, IN_PROGRESS, DONE
    private String category;      // daily, weekly, monthly, one-time
    private int priority;         // 1-5

    private String createdAt;
    private String updatedAt;
    private String completedAt;
    private String dueAt;

    private int createdByUserId;
    private int assigneeId;

    private boolean pendingReview; // NEW: true when staff submitted for review

    public Task() {
    }

    // --- ID ---
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    // --- Title ---
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    // --- Description ---
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    // --- Status ---
    public TaskStatus getStatus() {
        return status;
    }
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    // --- Category ---
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    // --- Priority ---
    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }

    // --- createdAt ---
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // --- updatedAt ---
    public String getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // --- completedAt ---
    public String getCompletedAt() {
        return completedAt;
    }
    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    // --- dueAt ---
    public String getDueAt() {
        return dueAt;
    }
    public void setDueAt(String dueAt) {
        this.dueAt = dueAt;
    }

    // --- createdByUserId ---
    public int getCreatedByUserId() {
        return createdByUserId;
    }
    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    // --- assigneeId ---
    public int getAssigneeId() {
        return assigneeId;
    }
    public void setAssigneeId(int assigneeId) {
        this.assigneeId = assigneeId;
    }

    // --- pendingReview ---
    public boolean isPendingReview() {
        return pendingReview;
    }
    public void setPendingReview(boolean pendingReview) {
        this.pendingReview = pendingReview;
    }
}
