/**
 * Administrator Dashboard & Governance Client Script
 * Handles real database metrics, user management, and security audit logs.
 */

let currentUser = null;
let currentUsersPage = 0;
let currentAuditPage = 0;
const PAGE_SIZE = 15;

let activeTargetUserId = null;
let activeTargetUserEmail = null;
let activeTargetCurrentStatus = null;

let roleModalInstance = null;
let confirmStatusModalInstance = null;

document.addEventListener('DOMContentLoaded', async () => {
    roleModalInstance = new bootstrap.Modal(document.getElementById('modalChangeRole'));
    confirmStatusModalInstance = new bootstrap.Modal(document.getElementById('modalConfirmStatus'));

    await checkAdminAuth();
    if (currentUser && currentUser.role === 'ADMIN') {
        setupEventListeners();
        await loadAllAdminData();
    }
});

/**
 * Validates session credentials and verifies the active user holds ROLE_ADMIN.
 */
async function checkAdminAuth() {
    try {
        const resp = await fetch('/api/v1/auth/me');
        if (!resp.ok) {
            window.location.href = '/login.html';
            return;
        }
        const json = await resp.json();
        currentUser = json.data;
        renderNavUser(currentUser);

        if (currentUser.role !== 'ADMIN') {
            document.getElementById('unauthorizedNotice').style.display = 'block';
            document.getElementById('adminContentArea').style.display = 'none';
        } else {
            document.getElementById('unauthorizedNotice').style.display = 'none';
            document.getElementById('adminContentArea').style.display = 'block';
        }
    } catch (err) {
        window.location.href = '/login.html';
    }
}

function renderNavUser(user) {
    const container = document.getElementById('navAuthContainer');
    if (!container || !user) return;

    container.innerHTML = `
        <span class="badge bg-danger text-uppercase px-2 py-1 me-2">${user.role}</span>
        <span class="text-white small fw-semibold me-2">${escapeHtml(user.name)}</span>
        <button class="btn btn-outline-light btn-sm" id="btnNavLogout">Logout</button>
    `;

    document.getElementById('btnNavLogout')?.addEventListener('click', async () => {
        await fetch('/api/v1/auth/logout', { method: 'POST' });
        window.location.href = '/login.html';
    });
}

function setupEventListeners() {
    document.getElementById('btnRefreshAll')?.addEventListener('click', loadAllAdminData);
    document.getElementById('btnRefreshUsers')?.addEventListener('click', () => loadUsers(currentUsersPage));

    // User pagination
    document.getElementById('btnPrevUsers')?.addEventListener('click', () => {
        if (currentUsersPage > 0) loadUsers(currentUsersPage - 1);
    });
    document.getElementById('btnNextUsers')?.addEventListener('click', () => {
        loadUsers(currentUsersPage + 1);
    });

    // Save User Role button
    document.getElementById('btnSaveUserRole')?.addEventListener('click', saveUserRole);

    // Confirm Status Change button
    document.getElementById('btnExecuteStatusChange')?.addEventListener('click', executeStatusChange);

    // Audit Log Filters & Pagination
    document.getElementById('auditFilterForm')?.addEventListener('submit', (e) => {
        e.preventDefault();
        currentAuditPage = 0;
        loadAuditLogs(0);
    });

    document.getElementById('btnResetAuditFilter')?.addEventListener('click', () => {
        document.getElementById('filterAuditAction').value = '';
        document.getElementById('filterAuditStatus').value = '';
        document.getElementById('filterAuditFrom').value = '';
        document.getElementById('filterAuditTo').value = '';
        currentAuditPage = 0;
        loadAuditLogs(0);
    });

    document.getElementById('btnPrevAudit')?.addEventListener('click', () => {
        if (currentAuditPage > 0) loadAuditLogs(currentAuditPage - 1);
    });
    document.getElementById('btnNextAudit')?.addEventListener('click', () => {
        loadAuditLogs(currentAuditPage + 1);
    });
}

/**
 * Loads all dashboard sections concurrently.
 */
async function loadAllAdminData() {
    await Promise.all([
        loadDashboardMetrics(),
        loadDepartmentDistribution(),
        loadSemesterDistribution(),
        loadCategoryDistribution(),
        loadSearchActivity(),
        loadUsers(0),
        loadAuditLogs(0)
    ]);
}

/**
 * Fetches real aggregate metrics from the database.
 */
async function loadDashboardMetrics() {
    try {
        const resp = await fetch('/api/v1/admin/reports/dashboard');
        if (!resp.ok) throw new Error('Failed to fetch dashboard metrics');
        const json = await resp.json();
        const data = json.data;

        document.getElementById('kpiTotalUsers').textContent = data.totalUsers;
        document.getElementById('kpiUserBreakdown').textContent =
            `Students: ${data.studentCount} | Faculty: ${data.facultyCount} | Admin: ${data.adminCount}`;

        document.getElementById('kpiActiveResources').textContent = data.activeResources;
        document.getElementById('kpiTotalResources').textContent = `Total in repository: ${data.totalResources}`;

        document.getElementById('kpiStorageUsed').textContent = data.storageUsedFormatted || '0 B';
        document.getElementById('kpiSearchQueries').textContent = data.totalSearchQueries;
        document.getElementById('kpiAttendanceRecords').textContent = data.totalAttendanceRecords;
        document.getElementById('kpiFileOperations').textContent = data.totalFileOperations;
    } catch (err) {
        console.error('Error loading dashboard metrics:', err);
    }
}

/**
 * Loads departmental distribution table.
 */
async function loadDepartmentDistribution() {
    const tbody = document.getElementById('tblDeptDistribution');
    try {
        const resp = await fetch('/api/v1/admin/reports/departments');
        if (!resp.ok) throw new Error('Failed to fetch department metrics');
        const json = await resp.json();
        const list = json.data || [];

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center py-3 text-muted">No department resources found.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(item => `
            <tr>
                <td><span class="badge bg-light text-dark border font-monospace">${escapeHtml(item.departmentCode)}</span></td>
                <td class="fw-semibold">${escapeHtml(item.departmentName)}</td>
                <td class="text-center"><span class="badge bg-primary rounded-pill px-3">${item.resourceCount}</span></td>
                <td class="text-end text-muted small">${item.storageFormatted}</td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center py-3 text-danger">Failed to load department metrics.</td></tr>`;
    }
}

/**
 * Loads semester allocation table.
 */
async function loadSemesterDistribution() {
    const tbody = document.getElementById('tblSemesterDistribution');
    try {
        const resp = await fetch('/api/v1/admin/reports/semesters');
        if (!resp.ok) throw new Error('Failed to fetch semester metrics');
        const json = await resp.json();
        const list = json.data || [];

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="2" class="text-center py-3 text-muted">No semester data found.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(item => `
            <tr>
                <td>Semester ${item.semesterNumber}</td>
                <td class="text-end"><span class="badge bg-secondary rounded-pill">${item.resourceCount}</span></td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="2" class="text-center py-3 text-danger">Failed to load semester metrics.</td></tr>`;
    }
}

/**
 * Loads category breakdown table.
 */
async function loadCategoryDistribution() {
    const tbody = document.getElementById('tblCategoryDistribution');
    try {
        const resp = await fetch('/api/v1/admin/reports/categories');
        if (!resp.ok) throw new Error('Failed to fetch category metrics');
        const json = await resp.json();
        const list = json.data || [];

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="2" class="text-center py-3 text-muted">No category data found.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(item => `
            <tr>
                <td>${escapeHtml(item.categoryName)}</td>
                <td class="text-end"><span class="badge bg-info text-dark rounded-pill">${item.resourceCount}</span></td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="2" class="text-center py-3 text-danger">Failed to load category metrics.</td></tr>`;
    }
}

/**
 * Loads top search queries report.
 */
async function loadSearchActivity() {
    const tbody = document.getElementById('tblSearchActivity');
    try {
        const resp = await fetch('/api/v1/admin/reports/search-activity?limit=10');
        if (!resp.ok) throw new Error('Failed to fetch search analytics');
        const json = await resp.json();
        const data = json.data;

        document.getElementById('badgeUniqueQueries').textContent = `Unique Queries: ${data.uniqueQueriesCount}`;

        const queries = data.topQueries || [];
        if (queries.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-muted">No search queries recorded yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = queries.map((q, idx) => `
            <tr>
                <td class="fw-bold text-muted" style="width: 60px;">#${idx + 1}</td>
                <td><span class="font-monospace text-dark">"${escapeHtml(q.query)}"</span></td>
                <td class="text-end"><span class="badge bg-warning text-dark px-2">${q.count} searches</span></td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-danger">Failed to load search activity.</td></tr>`;
    }
}

/**
 * Loads registered user accounts with pagination.
 */
async function loadUsers(page) {
    const tbody = document.getElementById('tblUsers');
    try {
        const resp = await fetch(`/api/v1/admin/users?page=${page}&size=${PAGE_SIZE}&sort=userId,asc`);
        if (!resp.ok) throw new Error('Failed to fetch users');
        const json = await resp.json();
        const pageData = json.data;
        const users = pageData.content || [];

        currentUsersPage = pageData.number;

        if (users.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No users found.</td></tr>`;
            return;
        }

        tbody.innerHTML = users.map(u => {
            let roleBadge = 'bg-info text-dark';
            if (u.role === 'ADMIN') roleBadge = 'bg-danger text-white';
            if (u.role === 'FACULTY') roleBadge = 'bg-warning text-dark';

            const statusBadge = u.isActive
                ? `<span class="badge bg-success">Active</span>`
                : `<span class="badge bg-secondary">Deactivated</span>`;

            const isSelf = currentUser && currentUser.userId === u.userId;
            const isPrimaryAdmin = (u.email || '').toLowerCase() === 'admin@college.edu';

            const toggleBtnText = u.isActive ? 'Deactivate' : 'Activate';
            const toggleBtnClass = u.isActive ? 'btn-outline-danger' : 'btn-outline-success';

            const disableToggle = isSelf || (isPrimaryAdmin && u.isActive);

            return `
                <tr>
                    <td class="text-muted small">#${u.userId}</td>
                    <td class="fw-semibold">${escapeHtml(u.name)}</td>
                    <td class="font-monospace small">${escapeHtml(u.email)}</td>
                    <td><span class="badge ${roleBadge} px-2 py-1">${u.role}</span></td>
                    <td class="small text-muted">${u.departmentCode ? escapeHtml(u.departmentCode) : 'General'}</td>
                    <td>${statusBadge}</td>
                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-primary btn-sm" onclick="openRoleModal(${u.userId}, '${escapeJs(u.email)}', '${u.role}')" ${isPrimaryAdmin ? 'disabled' : ''}>
                                Role
                            </button>
                            <button class="btn ${toggleBtnClass} btn-sm" onclick="openConfirmStatusModal(${u.userId}, '${escapeJs(u.email)}', ${u.isActive})" ${disableToggle ? 'disabled title="Cannot deactivate self or primary admin"' : ''}>
                                ${toggleBtnText}
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        // Update pagination controls
        document.getElementById('usersPaginationInfo').textContent =
            `Page ${pageData.number + 1} of ${pageData.totalPages} (${pageData.totalElements} users total)`;
        document.getElementById('btnPrevUsers').disabled = pageData.first;
        document.getElementById('btnNextUsers').disabled = pageData.last;
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">Failed to load user list.</td></tr>`;
    }
}

/**
 * Opens modal to modify user role.
 */
function openRoleModal(userId, email, currentRole) {
    activeTargetUserId = userId;
    activeTargetUserEmail = email;
    document.getElementById('roleModalUserEmail').value = `${email} (#${userId})`;
    document.getElementById('roleModalSelect').value = currentRole;
    roleModalInstance.show();
}

/**
 * Submits updated user role to the backend.
 */
async function saveUserRole() {
    if (!activeTargetUserId) return;
    const newRole = document.getElementById('roleModalSelect').value;

    try {
        const resp = await fetch(`/api/v1/admin/users/${activeTargetUserId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ role: newRole })
        });

        const json = await resp.json();
        if (!resp.ok || !json.success) {
            throw new Error(json.message || json.error || 'Failed to update user role');
        }

        roleModalInstance.hide();
        showAlert('success', `User role for ${activeTargetUserEmail} successfully updated to ${newRole}.`);
        await loadUsers(currentUsersPage);
        await loadDashboardMetrics();
    } catch (err) {
        showAlert('danger', err.message);
    }
}

/**
 * Opens confirmation dialog before toggling account status.
 */
function openConfirmStatusModal(userId, email, currentStatus) {
    activeTargetUserId = userId;
    activeTargetUserEmail = email;
    activeTargetCurrentStatus = currentStatus;

    const actionText = currentStatus ? 'Deactivate' : 'Activate';
    document.getElementById('confirmStatusTitle').textContent = `Confirm Account ${actionText}`;
    document.getElementById('confirmStatusMessage').textContent =
        `Are you sure you want to ${actionText.toLowerCase()} the account for "${email}"? ${currentStatus ? 'The user will immediately be barred from signing into the portal.' : 'The user will regain portal access.'}`;

    const confirmBtn = document.getElementById('btnExecuteStatusChange');
    confirmBtn.className = currentStatus ? 'btn btn-danger btn-sm' : 'btn btn-success btn-sm';
    confirmBtn.textContent = `Yes, ${actionText} Account`;

    confirmStatusModalInstance.show();
}

/**
 * Submits account status modification to the backend.
 */
async function executeStatusChange() {
    if (!activeTargetUserId) return;
    const newStatus = !activeTargetCurrentStatus;

    try {
        const resp = await fetch(`/api/v1/admin/users/${activeTargetUserId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ isActive: newStatus })
        });

        const json = await resp.json();
        if (!resp.ok || !json.success) {
            throw new Error(json.message || json.error || 'Failed to update user status');
        }

        confirmStatusModalInstance.hide();
        showAlert('success', `Account for ${activeTargetUserEmail} was successfully ${newStatus ? 'activated' : 'deactivated'}.`);
        await loadUsers(currentUsersPage);
        await loadDashboardMetrics();
    } catch (err) {
        confirmStatusModalInstance.hide();
        showAlert('danger', err.message);
    }
}

/**
 * Loads security audit logs with pagination and filters.
 */
async function loadAuditLogs(page) {
    const tbody = document.getElementById('tblAuditLogs');
    const action = document.getElementById('filterAuditAction')?.value || '';
    const status = document.getElementById('filterAuditStatus')?.value || '';
    const fromVal = document.getElementById('filterAuditFrom')?.value || '';
    const toVal = document.getElementById('filterAuditTo')?.value || '';

    const params = new URLSearchParams({
        page: page,
        size: PAGE_SIZE,
        sort: 'timestamp,desc'
    });

    if (action.trim()) params.append('action', action.trim());
    if (status.trim()) params.append('status', status.trim());
    if (fromVal) params.append('fromDate', fromVal);
    if (toVal) params.append('toDate', toVal);

    try {
        const resp = await fetch(`/api/v1/admin/audit-logs?${params.toString()}`);
        if (!resp.ok) throw new Error('Failed to fetch audit logs');
        const json = await resp.json();
        const pageData = json.data;
        const logs = pageData.content || [];

        currentAuditPage = pageData.number;

        if (logs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No audit logs matching current criteria.</td></tr>`;
            return;
        }

        tbody.innerHTML = logs.map(l => {
            let statusBadge = 'bg-success';
            if (l.status === 'FAILED' || l.status === 'DENIED') statusBadge = 'bg-danger';

            return `
                <tr>
                    <td class="small text-muted text-nowrap">${formatDate(l.timestamp)}</td>
                    <td class="small font-monospace fw-semibold">${l.userEmail ? escapeHtml(l.userEmail) : 'SYSTEM'}</td>
                    <td><span class="badge bg-light text-dark border font-monospace">${escapeHtml(l.action)}</span></td>
                    <td class="small">${l.resourceType ? escapeHtml(l.resourceType) : '-'}: ${l.resourceIdentifier ? escapeHtml(l.resourceIdentifier) : '-'}</td>
                    <td class="small font-monospace text-muted">${l.ipAddress ? escapeHtml(l.ipAddress) : '-'}</td>
                    <td><span class="badge ${statusBadge}">${l.status}</span></td>
                    <td class="small text-muted" style="max-width: 250px;">${escapeHtml(l.details || '-')}</td>
                </tr>
            `;
        }).join('');

        // Update pagination controls
        document.getElementById('auditPaginationInfo').textContent =
            `Page ${pageData.number + 1} of ${pageData.totalPages} (${pageData.totalElements} records total)`;
        document.getElementById('btnPrevAudit').disabled = pageData.first;
        document.getElementById('btnNextAudit').disabled = pageData.last;
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">Failed to load audit logs.</td></tr>`;
    }
}

/**
 * Extracts the XSRF token from the browser cookie.
 */
function getCsrfToken() {
    const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : '';
}

/**
 * Displays feedback alert banners in the admin panel.
 */
function showAlert(type, message) {
    const container = document.getElementById('adminAlertContainer');
    if (!container) return;
    container.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show shadow-sm" role="alert">
            <span>${escapeHtml(message)}</span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    setTimeout(() => {
        const alertNode = container.querySelector('.alert');
        if (alertNode) bootstrap.Alert.getOrCreateInstance(alertNode).close();
    }, 6000);
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
        const d = new Date(dateStr);
        return d.toLocaleString();
    } catch (e) {
        return dateStr;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeJs(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '\\"');
}
