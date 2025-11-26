const loginTab = document.getElementById("login-tab");
const registerTab = document.getElementById("register-tab");
const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const authMessage = document.getElementById("auth-message");

function showLogin() {
  loginForm.classList.remove("hidden");
  registerForm.classList.add("hidden");
  loginTab.classList.add("active");
  registerTab.classList.remove("active");
  authMessage.textContent = "";
}

function showRegister() {
  registerForm.classList.remove("hidden");
  loginForm.classList.add("hidden");
  registerTab.classList.add("active");
  loginTab.classList.remove("active");
  authMessage.textContent = "";
}

if (loginTab) loginTab.addEventListener("click", showLogin);
if (registerTab) registerTab.addEventListener("click", showRegister);

// LOGIN
if (loginForm) {
  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    authMessage.textContent = "Logging in...";
    authMessage.classList.remove("error");

    const username = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value;

    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ username, password }),
      });

      if (!res.ok) {
        let errText = "Login failed.";
        try {
          const err = await res.json();
          if (err && err.error) errText = err.error;
        } catch (_) {}
        authMessage.textContent = errText;
        authMessage.classList.add("error");
        return;
      }

      // ✅ SUCCESS → go to dashboard
      window.location.href = "/dashboard.html";
    } catch (err) {
      console.error(err);
      authMessage.textContent = "Network error.";
      authMessage.classList.add("error");
    }
  });
}

// REGISTER
if (registerForm) {
  registerForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    authMessage.textContent = "Registering...";
    authMessage.classList.remove("error");

    const username = document.getElementById("register-username").value.trim();
    const email = document.getElementById("register-email").value.trim();
    const password = document.getElementById("register-password").value;
    const role = document.getElementById("register-role").value;

    try {
      const res = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ username, email, password, role }),
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        authMessage.textContent = data.error || "Registration failed.";
        authMessage.classList.add("error");
        return;
      }

      authMessage.textContent = "Registration successful! You can now log in.";
      authMessage.classList.remove("error");
      showLogin();
    } catch (err) {
      console.error(err);
      authMessage.textContent = "Network error.";
      authMessage.classList.add("error");
    }
  });
}
