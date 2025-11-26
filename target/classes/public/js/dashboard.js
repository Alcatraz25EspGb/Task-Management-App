// public/js/dashboard.js

let currentUser = null;
let allTasks = [];
let currentView = "list";
let userMap = {};
let allUsers = [];
let calendarState = {
  year: null,
  month: null
};
let commentsModalTaskId = null;

// Assignee picker state (by username)
let selectedAssignees = [];

// Edit-task state
let editingTaskId = null;
let editingTaskAssigneeId = null;
let editingTaskStatus = null;

// Delete-task state
let deleteTaskId = null;

// Comment counts per task (taskId -> number)
let commentCounts = {};

// Cached modal refs
let createTaskModalEl = null;
let createTaskFormEl = null;
let assigneePickerEl = null;

document.addEventListener("DOMContentLoaded", () => {
  initDashboard();
});

async function initDashboard() {
  try {
    const meRes = await fetch("/api/auth/me");
    if (!meRes.ok) {
      window.location.href = "login.html";
      return;
    }
    currentUser = await meRes.json();
    setupHeader();
    await loadUserMap();
    setupAssigneePicker();
    setupToolbar();
    setupFilters();
    setupCommentsModal();
    setupDeleteModal();
    setupDueDateMin();
    await loadTasks();
    await loadNotifications();
  } catch (err) {
    console.error("Failed to initialise dashboard", err);
  }
}

function setupHeader() {
  const subtitleEl = document.getElementById("header-subtitle");
  const roleBadge = document.getElementById("role-badge");
  subtitleEl.textContent = `Welcome back, ${currentUser.username}`;
  roleBadge.textContent = currentUser.role;

  document.getElementById("logout-button").addEventListener("click", () => {
    window.location.href = "login.html";
  });

  const notifBtn = document.getElementById("notifications-button");
  const notifDropdown = document.getElementById("notifications-dropdown");

  if (notifBtn && notifDropdown) {
    notifBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      notifDropdown.classList.toggle("hidden");
    });

    document.addEventListener("click", (e) => {
      if (!notifDropdown.classList.contains("hidden")) {
        if (!notifDropdown.contains(e.target) && e.target !== notifBtn) {
          notifDropdown.classList.add("hidden");
        }
      }
    });
  }
}

// ================================
// USERS MAP & ASSIGNEE PICKER
// ================================

async function loadUserMap() {
  try {
    const res = await fetch("/api/users");
    if (!res.ok) return;
    const users = await res.json();
    allUsers = users;
    userMap = {};
    users.forEach((u) => {
      userMap[u.id] = u.username;
    });
  } catch (err) {
    console.error("Failed to load user map", err);
  }
}

function setupAssigneePicker() {
  const select = document.getElementById("task-assignee-select");
  const chipsContainer = document.getElementById("assignee-chips");
  assigneePickerEl = document.querySelector(".assignee-picker");

  if (!select || !chipsContainer || !assigneePickerEl) return;

  selectedAssignees = [];
  updateAssigneeChips();
  populateAssigneeSelect();

  select.addEventListener("change", () => {
    const value = select.value;
    if (!value) return;
    if (!selectedAssignees.includes(value)) {
      selectedAssignees.push(value);
      updateAssigneeChips();
      populateAssigneeSelect();
    }
    select.value = "";
  });
}

function populateAssigneeSelect() {
  const select = document.getElementById("task-assignee-select");
  if (!select) return;

  select.innerHTML = "";
  const placeholder = document.createElement("option");
  placeholder.value = "";
  placeholder.textContent = "Select assignee";
  select.appendChild(placeholder);

  const remaining = allUsers.filter(
    (u) => u.id !== currentUser.id && !selectedAssignees.includes(u.username)
  );

  remaining.forEach((u) => {
    const opt = document.createElement("option");
    opt.value = u.username;
    opt.textContent = `${u.username} (${u.role})`;
    select.appendChild(opt);
  });
}

function updateAssigneeChips() {
  const chipsContainer = document.getElementById("assignee-chips");
  if (!chipsContainer) return;
  chipsContainer.innerHTML = "";

  if (!selectedAssignees.length) {
    const span = document.createElement("span");
    span.className = "assignee-chip assignee-chip-empty";
    span.textContent = "No assignees selected";
    chipsContainer.appendChild(span);
    return;
  }

  selectedAssignees.forEach((username, index) => {
    const chip = document.createElement("span");
    chip.className = "assignee-chip";
    chip.textContent = username;

    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.className = "assignee-chip-remove";
    removeBtn.textContent = "×";
    removeBtn.addEventListener("click", () => {
      selectedAssignees = selectedAssignees.filter((u) => u !== username);
      updateAssigneeChips();
      populateAssigneeSelect();
    });

    chip.appendChild(removeBtn);
    chipsContainer.appendChild(chip);

    if ((index + 1) % 3 === 0) {
      const br = document.createElement("span");
      br.className = "assignee-chip-break";
      chipsContainer.appendChild(br);
    }
  });
}

// ================================
// TOOLBAR & VIEW SWITCHING
// ================================

function setupToolbar() {
  const listBtn = document.getElementById("list-tab-button");
  const calBtn = document.getElementById("calendar-tab-button");
  const listContent = document.getElementById("list-tab-content");
  const calContent = document.getElementById("calendar-tab-content");
  const createTaskButton = document.getElementById("create-task-button");

  createTaskModalEl = document.getElementById("create-task-modal");
  createTaskFormEl = document.getElementById("create-task-form");
  const createTaskClose = document.getElementById("create-task-close");

  // Staff cannot create tasks
  if (currentUser.role === "Staff") {
    createTaskButton.classList.add("hidden");
  } else {
    // Open modal in create mode
    createTaskButton.addEventListener("click", () => {
      editingTaskId = null;
      editingTaskAssigneeId = null;
      editingTaskStatus = null;
      createTaskFormEl.reset();
      selectedAssignees = [];
      updateAssigneeChips();
      populateAssigneeSelect();
      if (assigneePickerEl) assigneePickerEl.classList.remove("hidden");
      createTaskModalEl.classList.remove("hidden");
    });

    // Close modal
    createTaskClose.addEventListener("click", () => {
      createTaskModalEl.classList.add("hidden");
    });

    createTaskModalEl.addEventListener("click", (e) => {
      if (e.target === createTaskModalEl) {
        createTaskModalEl.classList.add("hidden");
      }
    });

    // Create/Edit submit
    createTaskFormEl.addEventListener("submit", async (e) => {
      e.preventDefault();
      await handleCreateOrEditTaskSubmit(createTaskFormEl, createTaskModalEl);
    });
  }

  // Default: list view
  listBtn.addEventListener("click", () => {
    currentView = "list";
    listBtn.classList.add("active");
    calBtn.classList.remove("active");
    listBtn.setAttribute("aria-selected", "true");
    calBtn.setAttribute("aria-selected", "false");

    listContent.classList.add("active");
    calContent.classList.remove("active");

    createTaskModalEl.classList.add("hidden");
    renderTaskList();
  });

  calBtn.addEventListener("click", () => {
    currentView = "calendar";
    calBtn.classList.add("active");
    listBtn.classList.remove("active");
    calBtn.setAttribute("aria-selected", "true");
    listBtn.setAttribute("aria-selected", "false");

    calContent.classList.add("active");
    listContent.classList.remove("active");

    createTaskModalEl.classList.add("hidden");
    renderCalendar();
  });

  const now = new Date();
  calendarState.year = now.getFullYear();
  calendarState.month = now.getMonth();

  document.getElementById("calendar-prev").addEventListener("click", () => {
    calendarState.month -= 1;
    if (calendarState.month < 0) {
      calendarState.month = 11;
      calendarState.year -= 1;
    }
    renderCalendar();
  });

  document.getElementById("calendar-next").addEventListener("click", () => {
    calendarState.month += 1;
    if (calendarState.month > 11) {
      calendarState.month = 0;
      calendarState.year += 1;
    }
    renderCalendar();
  });
}

function setupFilters() {
  ["filter-status", "filter-category", "filter-priority", "filter-mine"].forEach(
    (id) => {
      const el = document.getElementById(id);
      el.addEventListener("change", () => {
        if (currentView === "list") {
          renderTaskList();
        } else {
          renderCalendar();
        }
      });
    }
  );
}

function setupCommentsModal() {
  const modal = document.getElementById("comments-modal");
  const closeBtn = document.getElementById("comments-close-btn");
  const form = document.getElementById("comments-form");

  closeBtn.addEventListener("click", () => {
    modal.classList.add("hidden");
    commentsModalTaskId = null;
  });

  modal.addEventListener("click", (e) => {
    if (e.target === modal) {
      modal.classList.add("hidden");
      commentsModalTaskId = null;
    }
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const input = document.getElementById("comments-input");
    const text = input.value.trim();
    if (!text || !commentsModalTaskId) return;

    try {
      const res = await fetch(`/api/tasks/${commentsModalTaskId}/comments`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text })
      });
      if (res.ok) {
        input.value = "";
        await loadCommentsForTask(commentsModalTaskId);
        await loadCommentCounts();
        renderTaskList();
      }
    } catch (err) {
      console.error("Failed to post comment", err);
    }
  });
}

function setupDeleteModal() {
  const modal = document.getElementById("delete-modal");
  if (!modal) return;

  const closeBtn = document.getElementById("delete-close-btn");
  const cancelBtn = document.getElementById("delete-cancel-btn");
  const confirmBtn = document.getElementById("delete-confirm-btn");

  const hide = () => {
    modal.classList.add("hidden");
    deleteTaskId = null;
  };

  closeBtn.addEventListener("click", hide);
  cancelBtn.addEventListener("click", hide);

  modal.addEventListener("click", (e) => {
    if (e.target === modal) hide();
  });

  confirmBtn.addEventListener("click", async () => {
    if (!deleteTaskId) {
      hide();
      return;
    }
    try {
      const res = await fetch(`/api/tasks/${deleteTaskId}`, {
        method: "DELETE"
      });
      if (!res.ok && res.status !== 204) {
        console.error("Delete failed", await res.text());
        alert("Could not delete task.");
      }
    } catch (err) {
      console.error("Delete failed", err);
    }
    hide();
    await loadTasks();
    await loadNotifications();
  });
}

function setupDueDateMin() {
  const input = document.getElementById("task-due");
  if (!input) return;
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const dd = String(now.getDate()).padStart(2, "0");
  const hh = String(now.getHours()).padStart(2, "0");
  const mi = String(now.getMinutes()).padStart(2, "0");
  input.min = `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
}

// ================================
// DATA LOADERS
// ================================

async function loadTasks() {
  try {
    const res = await fetch("/api/tasks");
    if (!res.ok) {
      throw new Error("Failed to load tasks");
    }
    let tasks = await res.json();

    // Staff only sees tasks assigned to them
    if (currentUser.role === "Staff") {
      tasks = tasks.filter((t) => t.assigneeId === currentUser.id);
    }

    allTasks = tasks;
    await loadCommentCounts();
    recomputeSummary();
    if (currentView === "list") {
      renderTaskList();
    } else {
      renderCalendar();
    }
  } catch (err) {
    console.error("Error loading tasks", err);
  }
}

async function loadNotifications() {
  try {
    const res = await fetch("/api/notifications");
    if (!res.ok) {
      if (res.status !== 401) {
        console.error("Failed to load notifications");
      }
      return;
    }
    const list = await res.json();
    const container = document.getElementById("notifications-list");
    const emptyState = document.getElementById("notifications-empty");
    const badge = document.getElementById("notifications-count");

    let unreadCount = 0;

    if (!list || list.length === 0) {
      emptyState.classList.remove("hidden");
      container.classList.add("hidden");
      container.innerHTML = "";
      badge.classList.add("hidden");
      return;
    }

    emptyState.classList.add("hidden");
    container.classList.remove("hidden");
    container.innerHTML = "";

    list.forEach((n) => {
      if (!n.read) unreadCount++;

      const item = document.createElement("div");
      item.className = "notification-item" + (n.read ? "" : " unread");
      item.innerHTML = `
        <div>${n.message}</div>
        <div class="notification-meta">
          <span>${n.type}</span> • <span>${n.createdAt}</span>
        </div>
      `;
      item.addEventListener("click", async () => {
        try {
          if (!n.read) {
            const resp = await fetch(`/api/notifications/${n.id}/read`, {
              method: "PATCH"
            });
            if (resp.ok) {
              await loadNotifications();
            }
          }
        } catch (err) {
          console.error("Failed to mark notification read", err);
        }
      });
      container.appendChild(item);
    });

    if (unreadCount > 0) {
      badge.textContent = unreadCount;
      badge.classList.remove("hidden");
    } else {
      badge.classList.add("hidden");
    }
  } catch (err) {
    console.error("Error loading notifications", err);
  }
}

// ================================
// CREATE / EDIT TASK HANDLER
// ================================

async function handleCreateOrEditTaskSubmit(form, modal) {
  const formData = new FormData(form);
  const title = formData.get("title");
  const description = formData.get("description") || "";
  const category = formData.get("category") || "one-time";
  const priority = Number(formData.get("priority") || "3");
  const dueAtRaw = formData.get("dueAt");

  let dueAt = null;
  if (dueAtRaw && dueAtRaw.length > 0) {
    const chosen = new Date(dueAtRaw);
    const now = new Date();
    if (!editingTaskId && chosen < now) {
      alert("You cannot create tasks in the past. Please pick a future time.");
      return;
    }
    dueAt = dueAtRaw.replace("T", " ") + ":00";
  }

  // EDIT MODE
  if (editingTaskId) {
    try {
      const payload = {
        title,
        description,
        category,
        priority,
        dueAt,
        assigneeId: editingTaskAssigneeId,
        status: editingTaskStatus
      };

      const res = await fetch(`/api/tasks/${editingTaskId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        console.error("Failed to update task", res.status, await res.text());
        alert("Failed to update task.");
        return;
      }

      editingTaskId = null;
      editingTaskAssigneeId = null;
      editingTaskStatus = null;

      form.reset();
      selectedAssignees = [];
      updateAssigneeChips();
      populateAssigneeSelect();
      modal.classList.add("hidden");
      await loadTasks();
      await loadNotifications();
      return;
    } catch (err) {
      console.error("Error updating task", err);
      alert("Failed to update task.");
      return;
    }
  }

  // CREATE MODE
  const assignees = [...selectedAssignees];
  if (!assignees.length) {
    alert("Please select at least one assignee.");
    return;
  }

  try {
    for (const username of assignees) {
      const payload = {
        title,
        description,
        category,
        priority,
        dueAt,
        assigneeUsername: username
      };

      const res = await fetch("/api/tasks", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        const text = await res.text();
        console.error("Failed to create task", res.status, text);
        alert(
          `Failed to create task for ${username}. Check assignee username and permissions.`
        );
      }
    }

    form.reset();
    selectedAssignees = [];
    updateAssigneeChips();
    populateAssigneeSelect();
    modal.classList.add("hidden");
    await loadTasks();
    await loadNotifications();
  } catch (err) {
    console.error("Error creating tasks", err);
  }
}

// ================================
// SUMMARY & FILTERS
// ================================

function getFilteredTasks() {
  const statusFilter = document.getElementById("filter-status").value;
  const categoryFilter = document.getElementById("filter-category").value;
  const priorityFilter = document.getElementById("filter-priority").value;
  const mineFilter = document.getElementById("filter-mine").value;

  return allTasks.filter((t) => {
    if (statusFilter && t.status !== statusFilter) return false;
    if (categoryFilter && t.category !== categoryFilter) return false;
    if (priorityFilter && String(t.priority) !== priorityFilter) return false;

    if (mineFilter === "assigned") {
      if (t.assigneeId !== currentUser.id) return false;
    } else if (mineFilter === "created") {
      if (t.createdByUserId !== currentUser.id) return false;
    }

    return true;
  });
}

function recomputeSummary() {
  const total = allTasks.length;
  const today = new Date().toISOString().slice(0, 10);

  let dueToday = 0;
  let inProgress = 0;
  let done = 0;
  let overdue = 0;

  allTasks.forEach((t) => {
    if (t.status === "TODO" || t.status === "IN_PROGRESS") inProgress++;
    if (t.status === "DONE") done++;

    if (t.dueAt) {
      const datePart = String(t.dueAt).slice(0, 10);
      if (datePart === today) {
        dueToday++;
      } else if (datePart < today && t.status !== "DONE") {
        overdue++;
      }
    }
  });

  document.getElementById("summary-total").textContent = total;
  document.getElementById("summary-due-today").textContent = dueToday;
  document.getElementById("summary-in-progress").textContent = inProgress;
  document.getElementById("summary-done").textContent = done;
  document.getElementById("summary-overdue").textContent = overdue;
}

// ================================
// LIST VIEW RENDERING
// ================================

function renderTaskList() {
  const container = document.getElementById("task-list");
  const emptyState = document.getElementById("task-list-empty");
  const tasks = getFilteredTasks();

  container.innerHTML = "";

  if (!tasks.length) {
    emptyState.classList.remove("hidden");
    return;
  }
  emptyState.classList.add("hidden");

  tasks.forEach((t) => {
    const card = document.createElement("article");
    card.className = "task-card";

    const isDone = t.status === "DONE";
    const categoryClass = t.category
      ? `chip-category-${t.category.replace(" ", "-")}`
      : "";
    const statusClass =
      t.status === "IN_PROGRESS"
        ? "chip-status-in-progress"
        : t.status === "DONE"
        ? "chip-status-done"
        : "chip-status-todo";

    const overdue =
      t.dueAt &&
      String(t.dueAt).slice(0, 10) < new Date().toISOString().slice(0, 10) &&
      t.status !== "DONE";

    const assigneeName =
      userMap[t.assigneeId] || (t.assigneeId ? `User ${t.assigneeId}` : "Unassigned");
    const creatorName =
      userMap[t.createdByUserId] ||
      (t.createdByUserId ? `User ${t.createdByUserId}` : "Unknown");

    const commentCount = commentCounts[t.id] || 0;

    let actionsHtml = "";

    // Staff submit button
    if (
      currentUser.role === "Staff" &&
      t.assigneeId === currentUser.id &&
      !t.pendingReview &&
      t.status !== "DONE"
    ) {
      actionsHtml += `<button class="task-action-btn" data-action="submit" data-task-id="${t.id}">Submit for review</button>`;
    }

    // Manager/Admin actions
    if (currentUser.role === "Manager" || currentUser.role === "Admin") {
      const completeLabel =
        t.status === "DONE" ? "Mark in progress" : "Mark complete";
      actionsHtml += `<button class="task-action-btn" data-action="complete" data-task-id="${t.id}">${completeLabel}</button>`;
      actionsHtml += `<button class="task-action-icon" data-action="edit" data-task-id="${t.id}" title="Edit task">✏️</button>`;
      actionsHtml += `<button class="task-action-icon task-delete-btn" data-action="delete" data-task-id="${t.id}" title="Delete task">🗑️</button>`;
      if (t.pendingReview) {
        actionsHtml += `<button class="task-action-btn" data-action="approve" data-task-id="${t.id}">Approve</button>`;
        actionsHtml += `<button class="task-action-btn" data-action="deny" data-task-id="${t.id}">Deny</button>`;
      }
    }

    card.innerHTML = `
      <div class="task-header-row">
        <div>
          <h3 class="task-title ${isDone ? "done" : ""}">${escapeHtml(t.title)}</h3>
          ${
            t.pendingReview
              ? `<p class="pending-review-label">(pending review)</p>`
              : ""
          }
          <p class="task-description">${escapeHtml(t.description || "")}</p>
        </div>
      </div>
      <div class="task-meta-row">
        <span class="chip ${statusClass}">${t.status.replace("_", " ")}</span>
        ${
          t.category
            ? `<span class="chip ${categoryClass}">${t.category}</span>`
            : ""
        }
        <span class="chip chip-priority">Priority ${t.priority || 3}</span>
        ${
          t.dueAt
            ? `<span class="chip chip-due">Due: ${escapeHtml(String(t.dueAt))}</span>`
            : ""
        }
        ${
          overdue
            ? `<span class="chip chip-overdue">Overdue</span>`
            : ""
        }
        <span class="chip chip-assignee">${escapeHtml(assigneeName)}</span>
        <button class="comments-btn" data-task-id="${t.id}">
          💬 ${commentCount} Comments
        </button>
      </div>
      <div class="task-footer-row">
        <span>Created by: ${escapeHtml(creatorName)} · ${t.createdAt}</span>
        <div class="task-actions">
          ${actionsHtml}
        </div>
      </div>
    `;

    const commentsBtn = card.querySelector(".comments-btn");
    commentsBtn.addEventListener("click", () => {
      openCommentsModal(t.id, t.title);
    });

    const actionBtns = card.querySelectorAll(".task-action-btn, .task-action-icon");
    actionBtns.forEach((btn) => {
      const action = btn.getAttribute("data-action");
      const id = Number(btn.getAttribute("data-task-id"));
      btn.addEventListener("click", () => {
        handleTaskAction(action, id);
      });
    });

    container.appendChild(card);
  });
}

// Handle actions: submit, complete(toggle), approve, deny, delete, edit
async function handleTaskAction(action, taskId) {
  try {
    if (action === "submit") {
      const res = await fetch(`/api/tasks/${taskId}/submit`, {
        method: "PATCH"
      });
      if (!res.ok) {
        console.error("Submit failed", await res.text());
        alert("Could not submit task for review.");
        return;
      }
    } else if (action === "complete") {
      const t = allTasks.find((task) => task.id === taskId);
      const newStatus =
        t && t.status === "DONE" ? "IN_PROGRESS" : "DONE";

      const res = await fetch(`/api/tasks/${taskId}/status`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: newStatus })
      });
      if (!res.ok) {
        console.error("Status update failed", await res.text());
        alert("Could not update task status.");
        return;
      }
    } else if (action === "approve") {
      const res = await fetch(`/api/tasks/${taskId}/approve`, {
        method: "PATCH"
      });
      if (!res.ok) {
        console.error("Approve failed", await res.text());
        alert("Could not approve task.");
        return;
      }
    } else if (action === "deny") {
      const res = await fetch(`/api/tasks/${taskId}/deny`, {
        method: "PATCH"
      });
      if (!res.ok) {
        console.error("Deny failed", await res.text());
        alert("Could not deny task.");
        return;
      }
    } else if (action === "delete") {
      deleteTaskId = taskId;
      const modal = document.getElementById("delete-modal");
      if (modal) modal.classList.remove("hidden");
      return; // actual delete handled in modal confirm
    } else if (action === "edit") {
      const t = allTasks.find((task) => task.id === taskId);
      if (!t || !createTaskModalEl || !createTaskFormEl) return;

      editingTaskId = t.id;
      editingTaskAssigneeId = t.assigneeId;
      editingTaskStatus = t.status;

      createTaskFormEl.querySelector("#task-title").value = t.title || "";
      createTaskFormEl.querySelector("#task-description").value =
        t.description || "";
      createTaskFormEl.querySelector("#task-category").value =
        t.category || "one-time";
      createTaskFormEl.querySelector("#task-priority").value =
        String(t.priority || 3);

      const dueInput = createTaskFormEl.querySelector("#task-due");
      if (t.dueAt && t.dueAt.length >= 16) {
        const parts = String(t.dueAt).slice(0, 16).replace(" ", "T");
        dueInput.value = parts;
      } else {
        dueInput.value = "";
      }

      if (assigneePickerEl) assigneePickerEl.classList.add("hidden");

      createTaskModalEl.classList.remove("hidden");
      return;
    }

    await loadTasks();
    await loadNotifications();
  } catch (err) {
    console.error("Task action failed", err);
  }
}

// ================================
// CALENDAR VIEW RENDERING
// ================================

function renderCalendar() {
  const grid = document.getElementById("calendar-grid");
  const label = document.getElementById("calendar-month-label");
  grid.innerHTML = "";

  const year = calendarState.year;
  const month = calendarState.month;

  const firstOfMonth = new Date(year, month, 1);
  const firstWeekday = (firstOfMonth.getDay() + 6) % 7; // Monday=0
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const prevMonthDays = new Date(year, month, 0).getDate();

  label.textContent = firstOfMonth.toLocaleString("default", {
    month: "long",
    year: "numeric"
  });

  const tasks = getFilteredTasks();
  const tasksByDate = new Map();

  const today = new Date();
  const todayStr = today.toISOString().slice(0, 10);
  const rangeEnd = new Date(today);
  rangeEnd.setDate(rangeEnd.getDate() + 14);

  const calendarStart = new Date(year, month, 1);
  calendarStart.setDate(calendarStart.getDate() - firstWeekday);
  const calendarEnd = new Date(calendarStart);
  calendarEnd.setDate(calendarEnd.getDate() + 41);

  tasks.forEach((t) => {
    if (!t.dueAt) return;

    const base = new Date(String(t.dueAt).replace(" ", "T"));

    if (t.category === "daily" || t.category === "weekly" || t.category === "monthly") {
      const intervalDays =
        t.category === "daily" ? 1 : t.category === "weekly" ? 7 : 30;

      let occurrence = new Date(base);
      const start = calendarStart > today ? calendarStart : today;

      // Move up to start of visible range
      while (occurrence < start) {
        occurrence.setDate(occurrence.getDate() + intervalDays);
      }

      while (occurrence <= rangeEnd && occurrence <= calendarEnd) {
        const key = occurrence.toISOString().slice(0, 10);
        if (!tasksByDate.has(key)) tasksByDate.set(key, []);
        tasksByDate.get(key).push(t);
        occurrence.setDate(occurrence.getDate() + intervalDays);
      }
    } else {
      const key = String(t.dueAt).slice(0, 10);
      if (!tasksByDate.has(key)) tasksByDate.set(key, []);
      tasksByDate.get(key).push(t);
    }
  });

  const totalCells = 42;
  for (let cell = 0; cell < totalCells; cell++) {
    const dayEl = document.createElement("div");
    dayEl.className = "calendar-day";

    let dayNum;
    let cellMonth = month;
    let dateKey;

    if (cell < firstWeekday) {
      dayNum = prevMonthDays - (firstWeekday - cell - 1);
      cellMonth = month - 1;
      if (cellMonth < 0) cellMonth = 11;
      dayEl.classList.add("calendar-day-other-month");
    } else if (cell >= firstWeekday + daysInMonth) {
      dayNum = cell - (firstWeekday + daysInMonth) + 1;
      cellMonth = month + 1;
      if (cellMonth > 11) cellMonth = 0;
      dayEl.classList.add("calendar-day-other-month");
    } else {
      dayNum = cell - firstWeekday + 1;
    }

    const cellYear =
      cell < firstWeekday && month === 0
        ? year - 1
        : cell >= firstWeekday + daysInMonth && month === 11
        ? year + 1
        : year;

    const mm = String(cellMonth + 1).padStart(2, "0");
    const dd = String(dayNum).padStart(2, "0");
    dateKey = `${cellYear}-${mm}-${dd}`;

    if (dateKey === todayStr) {
      dayEl.classList.add("calendar-day-today");
    }

    const header = document.createElement("div");
    header.className = "calendar-day-header";
    header.innerHTML = `<span class="calendar-day-number">${dayNum}</span>`;
    dayEl.appendChild(header);

    const dailyTasks = tasksByDate.get(dateKey) || [];
    dailyTasks.slice(0, 3).forEach((t) => {
      const pill = document.createElement("div");
      let statusClass = "calendar-task-todo";
      if (t.status === "IN_PROGRESS") statusClass = "calendar-task-in-progress";
      if (t.status === "DONE") statusClass = "calendar-task-done";

      pill.className = `calendar-task ${statusClass}`;
      pill.textContent = t.title;
      dayEl.appendChild(pill);
    });

    if (dailyTasks.length > 3) {
      const more = document.createElement("div");
      more.className = "calendar-task calendar-task-more";
      more.textContent = `+${dailyTasks.length - 3} more`;
      dayEl.appendChild(more);
    }

    grid.appendChild(dayEl);
  }
}

// ================================
// COMMENTS MODAL HELPERS
// ================================

async function openCommentsModal(taskId, title) {
  commentsModalTaskId = taskId;
  document.getElementById("comments-modal-title").textContent =
    `Comments · ${title}`;
  document.getElementById("comments-modal").classList.remove("hidden");
  await loadCommentsForTask(taskId);
}

async function loadCommentsForTask(taskId) {
  const listEl = document.getElementById("comments-list");
  listEl.innerHTML = "";

  try {
    const res = await fetch(`/api/tasks/${taskId}/comments`);
    if (!res.ok) {
      listEl.innerHTML = `<div class="comments-empty">Failed to load comments.</div>`;
      return;
    }
    const comments = await res.json();
    if (!comments.length) {
      listEl.innerHTML = `<div class="comments-empty">No comments yet. Be the first to comment.</div>`;
      return;
    }
    comments.forEach((c) => {
      const item = document.createElement("div");
      item.className = "comment-item";
      item.innerHTML = `
        <div class="comment-meta">
          <span>User ${c.userId}</span> • <span>${c.createdAt}</span>
        </div>
        <div class="comment-text">${escapeHtml(c.text)}</div>
      `;
      listEl.appendChild(item);
    });
  } catch (err) {
    console.error("Failed to load comments", err);
    listEl.innerHTML = `<div class="comments-empty">Failed to load comments.</div>`;
  }
}

async function loadCommentCounts() {
  commentCounts = {};
  try {
    await Promise.all(
      allTasks.map(async (t) => {
        try {
          const res = await fetch(`/api/tasks/${t.id}/comments`);
          if (!res.ok) return;
          const comments = await res.json();
          commentCounts[t.id] = comments.length || 0;
        } catch (err) {
          console.error("Failed to load comment count for task", t.id, err);
        }
      })
    );
  } catch (err) {
    console.error("Error loading comment counts", err);
  }
}

// ================================
// UTILS
// ================================

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
