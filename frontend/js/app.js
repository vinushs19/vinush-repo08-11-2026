/**
 * AI-Based College Resource Finder — Frontend Application Logic
 * Handles health checking, session authentication status, and UI state updates.
 */

document.addEventListener("DOMContentLoaded", () => {
    checkSystemHealth();
    checkActuatorHealth();
    checkAuthStatus();

    const refreshBtn = document.getElementById("btnRefreshStatus");
    if (refreshBtn) {
        refreshBtn.addEventListener("click", () => {
            checkSystemHealth();
            checkActuatorHealth();
            checkAuthStatus();
        });
    }

    const logoutBtn = document.getElementById("btnLogoutNav");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", handleLogout);
    }
});

/**
 * Checks authentication status via GET /api/v1/auth/me
 */
async function checkAuthStatus() {
    const authContainer = document.getElementById("navAuthContainer");
    if (!authContainer) return;

    try {
        const response = await fetch("/api/v1/auth/me", { credentials: "include" });
        if (response.ok) {
            const result = await response.json();
            if (result.success && result.data) {
                const user = result.data;
                authContainer.innerHTML = `
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge bg-primary text-light px-2 py-1">
                            ${user.role}
                        </span>
                        <span class="text-light small fw-semibold">${user.name}</span>
                        <button class="btn btn-outline-light btn-sm ms-2" id="btnLogoutNav">Sign Out</button>
                    </div>
                `;
                document.getElementById("btnLogoutNav").addEventListener("click", handleLogout);
                return;
            }
        }
    } catch {
        // Not authenticated or network down
    }

    // Default to unauthenticated state
    authContainer.innerHTML = `
        <a href="/login.html" class="btn btn-primary btn-sm px-3">Sign In</a>
    `;
}

/**
 * Executes secure logout via POST /api/v1/auth/logout
 */
async function handleLogout() {
    try {
        await fetch("/api/v1/auth/logout", {
            method: "POST",
            credentials: "include"
        });
    } catch {
        // Ignore network errors on logout
    }
    window.location.reload();
}

/**
 * Queries the application health endpoint (/api/v1/health)
 */
async function checkSystemHealth() {
    const badge = document.getElementById("systemHealthBadge");
    const terminal = document.getElementById("healthRawOutput");
    const appStatusText = document.getElementById("appStatusText");
    const appVersionText = document.getElementById("appVersionText");

    if (badge) {
        badge.className = "badge bg-warning text-dark badge-live";
        badge.innerHTML = `<span class="pulse-dot"></span> Checking API...`;
    }

    try {
        const response = await fetch("/api/v1/health");
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();

        if (data && data.success && data.data) {
            if (badge) {
                badge.className = "badge bg-success badge-live";
                badge.innerHTML = `<span class="pulse-dot"></span> System Online`;
            }
            if (appStatusText) appStatusText.textContent = data.data.status || "UP";
            if (appVersionText) appVersionText.textContent = data.data.version || "1.0.0";
            if (terminal) terminal.textContent = JSON.stringify(data, null, 2);
        } else {
            throw new Error("Invalid payload format received from health API");
        }
    } catch (err) {
        if (badge) {
            badge.className = "badge bg-danger badge-live";
            badge.innerHTML = `<span class="pulse-dot"></span> Offline / Disconnected`;
        }
        if (appStatusText) appStatusText.textContent = "OFFLINE";
        if (terminal) {
            terminal.textContent = `[Connection Error] Could not reach /api/v1/health\nDetails: ${err.message}\nMake sure Spring Boot backend is running on http://localhost:8080.`;
        }
    }
}

/**
 * Queries the Spring Boot Actuator health endpoint (/actuator/health)
 */
async function checkActuatorHealth() {
    const actuatorText = document.getElementById("actuatorStatusText");
    if (!actuatorText) return;

    try {
        const response = await fetch("/actuator/health");
        if (response.ok) {
            const data = await response.json();
            actuatorText.textContent = data.status || "UP";
            actuatorText.className = "fw-bold text-success";
        } else {
            actuatorText.textContent = `HTTP ${response.status}`;
            actuatorText.className = "fw-bold text-warning";
        }
    } catch {
        actuatorText.textContent = "UNREACHABLE";
        actuatorText.className = "fw-bold text-danger";
    }
}
