/**
 * Staff Attendance Management JavaScript Client
 */

let currentUser = null;
let currentPage = 0;
const pageSize = 15;
let departmentsList = [];

document.addEventListener('DOMContentLoaded', async () => {
    await checkAuth();
    if (currentUser && currentUser.role !== 'STUDENT') {
        await loadDepartments();
        await loadAttendanceRecords(0);
        setupEventListeners();
    }
});

async function checkAuth() {
    try {
        const resp = await fetch('/api/v1/auth/me');
        if (!resp.ok) {
            window.location.href = '/login.html';
            return;
        }
        const json = await resp.json();
        currentUser = json.data;
        renderNavUser(currentUser);

        if (currentUser.role === 'STUDENT') {
            document.getElementById('studentAccessNotice').style.display = 'block';
            document.getElementById('staffContentArea').style.display = 'none';
            document.getElementById('headerActionButtons').style.display = 'none';
        }
    } catch (e) {
        window.location.href = '/login.html';
    }
}

function renderNavUser(user) {
    const container = document.getElementById('navAuthContainer');
    if (!container || !user) return;

    let roleBadgeClass = 'bg-info text-dark';
    if (user.role === 'ADMIN') roleBadgeClass = 'bg-danger text-white';
    if (user.role === 'FACULTY') roleBadgeClass = 'bg-warning text-dark';

    container.innerHTML = `
        <span class="badge ${roleBadgeClass} text-uppercase px-2 py-1">${user.role}</span>
        <span class="text-white small fw-semibold me-2">${escapeHtml(user.name)}</span>
        <button class="btn btn-outline-light btn-sm" id="btnNavLogout">Logout</button>
    `;

    document.getElementById('btnNavLogout')?.addEventListener('click', async () => {
        await fetch('/api/v1/auth/logout', { method: 'POST' });
        window.location.href = '/login.html';
    });
}

async function loadDepartments() {
    try {
        const resp = await fetch('/api/v1/master/departments?activeOnly=true');
        if (resp.ok) {
            const json = await resp.json();
            departmentsList = json.data || [];
            populateDepartmentDropdowns(departmentsList);
        }
    } catch (e) {
        console.warn('Failed to load departments', e);
    }
}

function populateDepartmentDropdowns(depts) {
    const filterSelect = document.getElementById('filterDepartment');
    const recSelect = document.getElementById('recDeptId');

    if (filterSelect) {
        filterSelect.innerHTML = '<option value="">All Departments</option>';
        depts.forEach(d => {
            filterSelect.add(new Option(`${d.departmentCode} — ${d.departmentName}`, d.departmentId));
        });
    }

    if (recSelect) {
        recSelect.innerHTML = '<option value="" disabled selected>Select Department</option>';
        depts.forEach(d => {
            recSelect.add(new Option(`${d.departmentCode} — ${d.departmentName}`, d.departmentId));
        });
    }
}

function setupEventListeners() {
    // Summary Calculation Form
    document.getElementById('summaryForm')?.addEventListener('submit', handleCalculateSummary);

    // Search Filter Form
    document.getElementById('searchFilterForm')?.addEventListener('submit', (e) => {
        e.preventDefault();
        loadAttendanceRecords(0);
    });

    // Reset Filter Button
    document.getElementById('btnResetFilters')?.addEventListener('click', () => {
        document.getElementById('searchFilterForm').reset();
        loadAttendanceRecords(0);
    });

    // Single Record Form
    document.getElementById('recordForm')?.addEventListener('submit', handleRecordSubmit);

    // Batch Import Form
    document.getElementById('importForm')?.addEventListener('submit', handleBatchImportSubmit);
}

async function handleCalculateSummary(e) {
    e.preventDefault();
    const staffId = document.getElementById('calcStaffId').value.trim();
    if (!staffId) return;

    const startDate = document.getElementById('calcStartDate').value;
    const endDate = document.getElementById('calcEndDate').value;

    let url = `/api/v1/attendance/staff/${encodeURIComponent(staffId)}/summary?`;
    if (startDate) url += `startDate=${encodeURIComponent(startDate)}&`;
    if (endDate) url += `endDate=${encodeURIComponent(endDate)}&`;

    try {
        const resp = await fetch(url);
        const json = await resp.json();

        if (!resp.ok) {
            showAlert(json.message || 'Failed to calculate attendance percentage', 'danger');
            document.getElementById('summaryResult').style.display = 'none';
            return;
        }

        const data = json.data;
        renderSummaryResult(data);
    } catch (err) {
        showAlert('Error calculating attendance summary: ' + err.message, 'danger');
    }
}

function renderSummaryResult(data) {
    const container = document.getElementById('summaryResult');
    container.style.display = 'block';

    document.getElementById('resTotalSessions').textContent = data.totalApplicableSessions;
    document.getElementById('resAttendedSessions').textContent = data.sessionsAttended;
    document.getElementById('resPercentage').textContent = data.attendancePercentageFormatted;

    const progressBar = document.getElementById('resProgressBar');
    progressBar.style.width = Math.min(100, Math.max(0, data.attendancePercentage)) + '%';
    if (data.attendancePercentage >= 75) {
        progressBar.className = 'progress-bar bg-success';
    } else if (data.attendancePercentage >= 60) {
        progressBar.className = 'progress-bar bg-warning';
    } else {
        progressBar.className = 'progress-bar bg-danger';
    }

    document.getElementById('pillPresent').textContent = `Present: ${data.sessionsPresent}`;
    document.getElementById('pillOnDuty').textContent = `OD: ${data.sessionsOnDuty}`;
    document.getElementById('pillAbsent').textContent = `Absent: ${data.sessionsAbsent}`;
    document.getElementById('pillLeave').textContent = `Leave: ${data.sessionsOnLeave}`;
}

async function loadAttendanceRecords(page = 0) {
    currentPage = page;
    const tbody = document.getElementById('attendanceTableBody');
    tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-muted">Loading attendance records...</td></tr>';

    const staffId = document.getElementById('filterStaffId')?.value.trim();
    const deptId = document.getElementById('filterDepartment')?.value;
    const startDate = document.getElementById('filterStartDate')?.value;
    const endDate = document.getElementById('filterEndDate')?.value;
    const status = document.getElementById('filterStatus')?.value;

    let url = `/api/v1/attendance/staff?page=${page}&size=${pageSize}&sort=attendanceDate,desc&sort=sessionPeriod,asc&`;
    if (staffId) url += `staffIdentifier=${encodeURIComponent(staffId)}&`;
    if (deptId) url += `departmentId=${encodeURIComponent(deptId)}&`;
    if (startDate) url += `startDate=${encodeURIComponent(startDate)}&`;
    if (endDate) url += `endDate=${encodeURIComponent(endDate)}&`;
    if (status) url += `status=${encodeURIComponent(status)}&`;

    try {
        const resp = await fetch(url);
        const json = await resp.json();

        if (!resp.ok) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-danger">${escapeHtml(json.message || 'Failed to load records')}</td></tr>`;
            return;
        }

        const pageData = json.data;
        renderAttendanceTable(pageData);
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-danger">Error: ${escapeHtml(e.message)}</td></tr>`;
    }
}

function renderAttendanceTable(pageData) {
    const tbody = document.getElementById('attendanceTableBody');
    const content = pageData.content || [];
    const total = pageData.totalElements || 0;

    document.getElementById('totalRecordsBadge').textContent = `${total} records`;

    if (content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-muted">No attendance records found matching filters.</td></tr>';
        renderPagination(0, 0);
        return;
    }

    tbody.innerHTML = content.map(r => {
        let badgeClass = 'bg-secondary';
        if (r.status === 'PRESENT') badgeClass = 'bg-success';
        if (r.status === 'ON_DUTY') badgeClass = 'bg-info text-dark';
        if (r.status === 'ABSENT') badgeClass = 'bg-danger';
        if (r.status === 'LEAVE') badgeClass = 'bg-warning text-dark';

        return `
            <tr>
                <td class="font-monospace small">${escapeHtml(r.attendanceDate)}</td>
                <td class="small fw-semibold text-center">P${r.sessionPeriod}</td>
                <td class="font-monospace small fw-bold">${escapeHtml(r.staffIdentifier)}</td>
                <td class="small">${escapeHtml(r.staffName)}</td>
                <td class="small"><span class="badge bg-light text-dark border">${escapeHtml(r.departmentCode || '-')}</span></td>
                <td><span class="badge ${badgeClass}">${escapeHtml(r.status)}</span></td>
                <td><span class="origin-badge rounded">${escapeHtml(r.dataSource)}</span></td>
                <td class="small text-muted">${escapeHtml(r.recordedByName || 'System')}</td>
                <td class="small text-muted">${escapeHtml(r.remarks || '-')}</td>
            </tr>
        `;
    }).join('');

    renderPagination(pageData.number, pageData.totalPages);
}

function renderPagination(current, totalPages) {
    const info = document.getElementById('pageInfo');
    const nav = document.getElementById('paginationNav');

    if (totalPages <= 1) {
        info.textContent = totalPages === 1 ? 'Showing all records' : 'No records';
        nav.innerHTML = '';
        return;
    }

    info.textContent = `Page ${current + 1} of ${totalPages}`;

    let html = '';
    html += `<li class="page-item ${current === 0 ? 'disabled' : ''}">
                <button class="page-link" onclick="loadAttendanceRecords(${current - 1})">Prev</button>
             </li>`;

    for (let p = 0; p < totalPages; p++) {
        if (p === 0 || p === totalPages - 1 || (p >= current - 1 && p <= current + 1)) {
            html += `<li class="page-item ${p === current ? 'active' : ''}">
                        <button class="page-link" onclick="loadAttendanceRecords(${p})">${p + 1}</button>
                     </li>`;
        } else if (p === current - 2 || p === current + 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    html += `<li class="page-item ${current === totalPages - 1 ? 'disabled' : ''}">
                <button class="page-link" onclick="loadAttendanceRecords(${current + 1})">Next</button>
             </li>`;
    nav.innerHTML = html;
}

async function handleRecordSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSaveRecord');
    btn.disabled = true;

    const payload = {
        staffIdentifier: document.getElementById('recStaffId').value.trim(),
        staffName: document.getElementById('recStaffName').value.trim(),
        departmentId: parseInt(document.getElementById('recDeptId').value, 10),
        attendanceDate: document.getElementById('recDate').value,
        sessionPeriod: parseInt(document.getElementById('recPeriod').value, 10),
        status: document.getElementById('recStatus').value,
        dataSource: document.getElementById('recOrigin').value,
        remarks: document.getElementById('recRemarks').value.trim() || null
    };

    try {
        const resp = await fetch('/api/v1/attendance/staff', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify(payload)
        });

        const json = await resp.json();
        if (!resp.ok) {
            throw new Error(json.message || 'Failed to save attendance record');
        }

        showAlert('Attendance record saved successfully!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('recordModal'))?.hide();
        document.getElementById('recordForm').reset();
        await loadAttendanceRecords(0);
    } catch (err) {
        showAlert(err.message, 'danger');
    } finally {
        btn.disabled = false;
    }
}

async function handleBatchImportSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitImport');
    btn.disabled = true;

    const origin = document.getElementById('importOrigin').value;
    const rawData = document.getElementById('importData').value.trim();

    if (!rawData) {
        btn.disabled = false;
        return;
    }

    const lines = rawData.split('\n');
    const records = [];

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line || line.startsWith('#')) continue;

        const parts = line.split(',').map(s => s.trim());
        if (parts.length < 5) continue;

        records.push({
            staffIdentifier: parts[0],
            staffName: parts[1],
            departmentId: parseInt(parts[2], 10),
            attendanceDate: parts[3],
            sessionPeriod: parseInt(parts[4], 10),
            status: parts.length > 5 ? parts[5].toUpperCase() : 'PRESENT',
            dataSource: origin,
            remarks: parts.length > 6 ? parts.slice(6).join(',') : null
        });
    }

    if (records.length === 0) {
        showAlert('No valid record lines found to import.', 'warning');
        btn.disabled = false;
        return;
    }

    try {
        const resp = await fetch('/api/v1/attendance/staff/import', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({
                defaultDataSource: origin,
                records: records
            })
        });

        const json = await resp.json();
        if (!resp.ok) {
            throw new Error(json.message || 'Failed to process batch import');
        }

        const data = json.data;
        showAlert(`Batch import completed: ${data.successCount} imported, ${data.duplicateCount} duplicates skipped, ${data.failureCount} errors.`,
                  data.failureCount > 0 ? 'warning' : 'success');

        bootstrap.Modal.getInstance(document.getElementById('importModal'))?.hide();
        document.getElementById('importForm').reset();
        await loadAttendanceRecords(0);
    } catch (err) {
        showAlert(err.message, 'danger');
    } finally {
        btn.disabled = false;
    }
}

// ---- Helpers ----

function getCsrfToken() {
    const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : '';
}

function showAlert(message, type = 'info') {
    const box = document.getElementById('alertBox');
    if (!box) return;
    box.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${escapeHtml(message)}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
