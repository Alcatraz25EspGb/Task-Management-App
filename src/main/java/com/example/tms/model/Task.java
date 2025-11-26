package com.example.tms.model;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private int id;
    private String title;
    private String description;
    private TaskStatus status;
    private String category;
    private int priority;
    private String createdAt;
    private String dueAt;
    private int createdByUserId;

    // Primary assignee (for backwards compatibility)
    private Integer assigneeId;

    // Multi-assignee support
    private List<Integer> assigneeIds = new ArrayList<>();

    private boolean pendingReview;
    private String completedAt;

    public Task() {
    }

    // ----------- ID -----------
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // ----------- TITLE -----------
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // ----------- DESCRIPTION -----------
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ----------- STATUS -----------
    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    // ----------- CATEGORY -----------
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // ----------- PRIORITY -----------
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    // ----------- CREATED AT -----------
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // ----------- DUE AT -----------
    public String getDueAt() {
        return dueAt;
    }

    public void setDueAt(String dueAt) {
        this.dueAt = dueAt;
    }

    // ----------- CREATED BY -----------
    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    // ----------- PRIMARY ASSIGNEE -----------
    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    // ----------- MULTI ASSIGNEES -----------
    public List<Integer> getAssigneeIds() {
        return assigneeIds;
    }

    public void setAssigneeIds(List<Integer> assigneeIds) {
        this.assigneeIds = assigneeIds != null ? assigneeIds : new ArrayList<>();
    }

    public void addAssigneeId(Integer userId) {
        if (userId == null) return;
        if (this.assigneeIds == null) {
            this.assigneeIds = new ArrayList<>();
        }
        if (!this.assigneeIds.contains(userId)) {
            this.assigneeIds.add(userId);
        }
    }

    public void removeAssigneeId(Integer userId) {
        if (this.assigneeIds != null) {
            this.assigneeIds.remove(userId);
        }
    }

    public boolean hasAssignee(int userId) {
        return assigneeIds != null && assigneeIds.contains(userId);
    }

    // ----------- PENDING REVIEW -----------
    public boolean isPendingReview() {
        return pendingReview;
    }

    public void setPendingReview(boolean pendingReview) {
        this.pendingReview = pendingReview;
    }

    // ----------- COMPLETED AT -----------
    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}
