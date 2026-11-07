/**
 * Academic Master Data Management — Frontend Logic
 * Interfaces with /api/v1/master/* endpoints with role-based UI adaptations and CSRF support.
 */

let currentUser = null;
let csrfToken = null;
let csrfHeader = "X-XSRF-TOKEN";
let masterModalInstance = null;

document.addEventListener("DOMContentLoaded", async () => {
    masterModalInstance = new bootstrap.Modal(document.getElementById("masterModal"));
    await initAuthAndCsrf();
    await loadInitialData();
});

/**
 * Initializes authentication state and CSRF token.
 */
async function initAuthAndCsrf() {
    // 1. Fetch current user
    try {
        const authRes = await fetch("/api/v1/auth/me", { credentials: "include" });
        if (authRes.ok) {
            const data = await authRes.json();
            currentUser = data.data;
            updateAuthHeaderUI();
        }
    } catch (e) {
        console.warn("User not logged in or unauthenticated:", e);
    }

    // 2. Fetch CSRF token
    try {
        const csrfRes = await fetch("/api/v1/auth/csrf", { credentials: "include" });
        if (csrfRes.ok) {
            const csrfData = await csrfRes.json();
            if (csrfData.data && csrfData.data.token) {
                csrfToken = csrfData.data.token;
                csrfHeader = csrfData.data.headerName || "X-XSRF-TOKEN";
            }
        }
    } catch (e) {
        console.warn("Could not retrieve CSRF token:", e);
    }
}

function updateAuthHeaderUI() {
    const navAuth = document.getElementById("navAuthContainer");
    const adminBadge = document.getElementById("adminNoticeBadge");

    if (currentUser) {
        navAuth.innerHTML = `
            <div class="d-flex align-items-center gap-2">
                <span class="badge bg-primary text-light px-2 py-1">${currentUser.role}</span>
                <span class="text-light small fw-semibold">${currentUser.name}</span>
                <a href="/login.html" class="btn btn-outline-light btn-sm ms-2">Switch</a>
            </div>
        `;

        if (currentUser.role === "ADMIN") {
            adminBadge.innerHTML = `<span class="badge bg-success-subtle text-success border border-success-subtle px-3 py-2">✓ Administrator Mode Enabled</span>`;
            document.querySelectorAll(".admin-only").forEach(el => el.classList.remove("d-none"));
        } else {
            adminBadge.innerHTML = `<span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle px-3 py-2">ℹ️ ${currentUser.role} View (Read-Only)</span>`;
            document.querySelectorAll(".admin-only").forEach(el => el.classList.add("d-none"));
        }
    } else {
        navAuth.innerHTML = `<a href="/login.html" class="btn btn-primary btn-sm px-3">Sign In</a>`;
        adminBadge.innerHTML = `<span class="badge bg-warning-subtle text-warning border border-warning-subtle px-3 py-2">⚠️ Read-Only (Sign In for Admin Actions)</span>`;
        document.querySelectorAll(".admin-only").forEach(el => el.classList.add("d-none"));
    }
}

async function loadInitialData() {
    await loadDepartments();
    await loadSemesters();
    await loadSubjectsDropdown();
    await loadSubjects();
    await loadCategories();
}

function showAlert(type, message) {
    const alertBox = document.getElementById("statusAlert");
    const alertMsg = document.getElementById("statusAlertMsg");
    alertBox.className = `alert alert-${type} alert-dismissible fade show`;
    alertMsg.textContent = message;
    alertBox.classList.remove("d-none");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function hideAlert() {
    document.getElementById("statusAlert").classList.add("d-none");
}

function getAuthHeaders() {
    const headers = { "Content-Type": "application/json" };
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

// =========================================================================
// 1. DEPARTMENTS
// =========================================================================

async function loadDepartments() {
    const tbody = document.getElementById("departmentsTableBody");
    const search = document.getElementById("deptSearchInput") ? document.getElementById("deptSearchInput").value : "";
    const activeOnly = document.getElementById("deptActiveOnly") ? document.getElementById("deptActiveOnly").checked : false;

    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div> Loading departments...</td></tr>`;

    let url = `/api/v1/master/departments?activeOnly=${activeOnly}`;
    if (search.trim()) url += `&search=${encodeURIComponent(search.trim())}`;

    try {
        const res = await fetch(url, { credentials: "include" });
        const json = await res.json();
        const list = json.data && json.data.content ? json.data.content : [];

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No departments found matching your criteria.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(d => `
            <tr>
                <td class="text-muted small">#${d.departmentId}</td>
                <td><span class="badge bg-light text-dark border fw-bold">${d.departmentCode}</span></td>
                <td class="fw-semibold">${d.departmentName}</td>
                <td>
                    <span class="badge ${d.isActive ? 'bg-success' : 'bg-secondary'}">
                        ${d.isActive ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td class="text-muted small">${d.createdAt ? d.createdAt.substring(0, 10) : '-'}</td>
                <td class="text-end admin-only ${currentUser && currentUser.role === 'ADMIN' ? '' : 'd-none'}">
                    <button class="btn btn-outline-primary btn-sm me-1" onclick="openEditDepartmentModal(${d.departmentId}, '${escapeHtml(d.departmentCode)}', '${escapeHtml(d.departmentName)}', ${d.isActive})">Edit</button>
                    <button class="btn btn-outline-${d.isActive ? 'danger' : 'success'} btn-sm" onclick="toggleDepartmentStatus(${d.departmentId}, ${!d.isActive})">
                        ${d.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger">Failed to load departments.</td></tr>`;
    }
}

function openAddDepartmentModal() {
    document.getElementById("masterModalTitle").textContent = "Add Academic Department";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Department Code</label>
            <input type="text" id="mDeptCode" class="form-control" placeholder="e.g. MCA, MBA, CSE" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Department Full Name</label>
            <input type="text" id="mDeptName" class="form-control" placeholder="e.g. Department of Computer Applications (PG)" required>
        </div>
    `;
    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = submitCreateDepartment;
    masterModalInstance.show();
}

async function submitCreateDepartment() {
    const code = document.getElementById("mDeptCode").value.trim();
    const name = document.getElementById("mDeptName").value.trim();
    if (!code || !name) {
        alert("Please enter both department code and full name.");
        return;
    }

    try {
        const res = await fetch("/api/v1/master/departments", {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ departmentCode: code, departmentName: name, isActive: true }),
            credentials: "include"
        });
        const json = await res.json();
        if (res.ok && json.success) {
            masterModalInstance.hide();
            showAlert("success", "Department created successfully.");
            await loadDepartments();
            await loadSubjectsDropdown();
        } else {
            alert(json.message || json.error || "Failed to create department.");
        }
    } catch (err) {
        alert("Network error. Please try again.");
    }
}

function openEditDepartmentModal(id, code, name, isActive) {
    document.getElementById("masterModalTitle").textContent = "Edit Department";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Department Code</label>
            <input type="text" id="mDeptCode" class="form-control" value="${code}" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Department Full Name</label>
            <input type="text" id="mDeptName" class="form-control" value="${name}" required>
        </div>
    `;
    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const newCode = document.getElementById("mDeptCode").value.trim();
        const newName = document.getElementById("mDeptName").value.trim();
        try {
            const res = await fetch(`/api/v1/master/departments/${id}`, {
                method: "PUT",
                headers: getAuthHeaders(),
                body: JSON.stringify({ departmentCode: newCode, departmentName: newName, isActive }),
                credentials: "include"
            });
            const json = await res.json();
            if (res.ok && json.success) {
                masterModalInstance.hide();
                showAlert("success", "Department updated successfully.");
                await loadDepartments();
                await loadSubjectsDropdown();
            } else {
                alert(json.message || json.error || "Failed to update department.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

async function toggleDepartmentStatus(id, newStatus) {
    if (!confirm(`Are you sure you want to ${newStatus ? 'activate' : 'deactivate'} this department?`)) return;

    try {
        const res = await fetch(`/api/v1/master/departments/${id}/status`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ isActive: newStatus }),
            credentials: "include"
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showAlert("info", `Department status changed to ${newStatus ? 'Active' : 'Inactive'}.`);
            await loadDepartments();
        } else {
            alert(json.message || json.error || "Failed to update status.");
        }
    } catch {
        alert("Network error.");
    }
}

// =========================================================================
// 2. SEMESTERS
// =========================================================================

async function loadSemesters() {
    const tbody = document.getElementById("semestersTableBody");
    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div> Loading semesters...</td></tr>`;

    try {
        const res = await fetch("/api/v1/master/semesters", { credentials: "include" });
        const json = await res.json();
        const list = json.data || [];

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">No semesters configured.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(s => `
            <tr>
                <td class="fw-bold">Semester ${s.semesterNumber}</td>
                <td>${s.semesterName}</td>
                <td><span class="badge ${s.term === 'ODD' ? 'bg-info text-dark' : 'bg-primary'}">${s.term}</span></td>
                <td>
                    <span class="badge ${s.isActive ? 'bg-success' : 'bg-secondary'}">
                        ${s.isActive ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td class="text-end admin-only ${currentUser && currentUser.role === 'ADMIN' ? '' : 'd-none'}">
                    <button class="btn btn-outline-${s.isActive ? 'danger' : 'success'} btn-sm" onclick="toggleSemesterStatus(${s.semesterId}, ${!s.isActive})">
                        ${s.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>
        `).join("");
    } catch {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-danger">Failed to load semesters.</td></tr>`;
    }
}

function openAddSemesterModal() {
    document.getElementById("masterModalTitle").textContent = "Add Academic Semester";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Semester Number (1-8)</label>
            <input type="number" id="mSemNumber" class="form-control" min="1" max="8" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Semester Name</label>
            <input type="text" id="mSemName" class="form-control" placeholder="e.g. Ninth Semester" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Term</label>
            <select id="mSemTerm" class="form-select">
                <option value="ODD">ODD (Monsoon / Autumn)</option>
                <option value="EVEN">EVEN (Spring / Summer)</option>
            </select>
        </div>
    `;
    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const num = parseInt(document.getElementById("mSemNumber").value, 10);
        const name = document.getElementById("mSemName").value.trim();
        const term = document.getElementById("mSemTerm").value;

        try {
            const res = await fetch("/api/v1/master/semesters", {
                method: "POST",
                headers: getAuthHeaders(),
                body: JSON.stringify({ semesterNumber: num, semesterName: name, term, isActive: true }),
                credentials: "include"
            });
            const json = await res.json();
            if (res.ok && json.success) {
                masterModalInstance.hide();
                showAlert("success", "Semester created.");
                await loadSemesters();
            } else {
                alert(json.message || json.error || "Failed to create semester.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

async function toggleSemesterStatus(id, newStatus) {
    try {
        const res = await fetch(`/api/v1/master/semesters/${id}/status`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ isActive: newStatus }),
            credentials: "include"
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showAlert("info", `Semester status changed to ${newStatus ? 'Active' : 'Inactive'}.`);
            await loadSemesters();
        } else {
            alert(json.message || json.error || "Failed to update semester status.");
        }
    } catch {
        alert("Network error.");
    }
}

// =========================================================================
// 3. SUBJECTS
// =========================================================================

async function loadSubjectsDropdown() {
    try {
        const res = await fetch("/api/v1/master/departments/active", { credentials: "include" });
        const json = await res.json();
        const depts = json.data || [];

        const select = document.getElementById("subjectDeptFilter");
        if (select) {
            select.innerHTML = `<option value="">All Departments</option>` +
                depts.map(d => `<option value="${d.departmentId}">${d.departmentCode} - ${d.departmentName}</option>`).join("");
        }
    } catch (e) {
        console.error("Failed to load departments dropdown:", e);
    }
}

async function loadSubjects() {
    const tbody = document.getElementById("subjectsTableBody");
    const deptId = document.getElementById("subjectDeptFilter").value;
    const sem = document.getElementById("subjectSemFilter").value;
    const search = document.getElementById("subjectSearchInput").value;

    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div> Loading subjects...</td></tr>`;

    let url = `/api/v1/master/subjects?`;
    if (deptId) url += `departmentId=${deptId}&`;
    if (sem) url += `semester=${sem}&`;
    if (search.trim()) url += `search=${encodeURIComponent(search.trim())}&`;

    try {
        const res = await fetch(url, { credentials: "include" });
        const json = await res.json();
        const list = json.data && json.data.content ? json.data.content : [];

        // Also update the unit subject selector dropdown
        const unitSubSelect = document.getElementById("unitSubjectSelect");
        if (unitSubSelect && (!unitSubSelect.value || unitSubSelect.options.length <= 1)) {
            unitSubSelect.innerHTML = `<option value="">-- Choose Subject --</option>` +
                list.map(s => `<option value="${s.subjectId}">${s.subjectCode} - ${s.subjectName} (Sem ${s.semester})</option>`).join("");
        }

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No subjects found matching your filter.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(s => `
            <tr>
                <td><span class="badge bg-light text-dark border fw-bold">${s.subjectCode}</span></td>
                <td>
                    <div class="fw-semibold">${s.subjectName}</div>
                    <div class="text-muted small text-truncate" style="max-width: 320px;">${s.description || 'No description provided.'}</div>
                </td>
                <td><span class="badge bg-secondary-subtle text-secondary">${s.departmentCode || s.departmentName || '-'}</span></td>
                <td><span class="badge bg-info-subtle text-info">Sem ${s.semester}</span></td>
                <td>
                    <span class="badge ${s.isActive ? 'bg-success' : 'bg-secondary'}">
                        ${s.isActive ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td class="text-end admin-only ${currentUser && currentUser.role === 'ADMIN' ? '' : 'd-none'}">
                    <button class="btn btn-outline-primary btn-sm me-1" onclick="openEditSubjectModal(${s.subjectId}, '${escapeHtml(s.subjectCode)}', '${escapeHtml(s.subjectName)}', ${s.semester}, ${s.departmentId}, '${escapeHtml(s.description || '')}', ${s.isActive})">Edit</button>
                    <button class="btn btn-outline-${s.isActive ? 'danger' : 'success'} btn-sm" onclick="toggleSubjectStatus(${s.subjectId}, ${!s.isActive})">
                        ${s.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>
        `).join("");
    } catch {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger">Failed to load subjects.</td></tr>`;
    }
}

async function openAddSubjectModal() {
    // Fetch departments for dropdown
    const res = await fetch("/api/v1/master/departments/active", { credentials: "include" });
    const json = await res.json();
    const depts = json.data || [];

    document.getElementById("masterModalTitle").textContent = "Add Academic Subject";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Subject Code</label>
            <input type="text" id="mSubCode" class="form-control" placeholder="e.g. 26MCA201" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Subject Full Name</label>
            <input type="text" id="mSubName" class="form-control" placeholder="e.g. Database Management Systems" required>
        </div>
        <div class="row g-2 mb-3">
            <div class="col-md-6">
                <label class="form-label small fw-semibold">Department</label>
                <select id="mSubDept" class="form-select" required>
                    ${depts.map(d => `<option value="${d.departmentId}">${d.departmentCode} - ${d.departmentName}</option>`).join("")}
                </select>
            </div>
            <div class="col-md-6">
                <label class="form-label small fw-semibold">Semester (1-8)</label>
                <select id="mSubSem" class="form-select" required>
                    <option value="1">Semester 1</option>
                    <option value="2">Semester 2</option>
                    <option value="3">Semester 3</option>
                    <option value="4">Semester 4</option>
                    <option value="5">Semester 5</option>
                    <option value="6">Semester 6</option>
                    <option value="7">Semester 7</option>
                    <option value="8">Semester 8</option>
                </select>
            </div>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Syllabus Overview / Description</label>
            <textarea id="mSubDesc" class="form-control" rows="3" placeholder="Brief curriculum description..."></textarea>
        </div>
    `;

    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const code = document.getElementById("mSubCode").value.trim();
        const name = document.getElementById("mSubName").value.trim();
        const deptId = parseInt(document.getElementById("mSubDept").value, 10);
        const sem = parseInt(document.getElementById("mSubSem").value, 10);
        const desc = document.getElementById("mSubDesc").value.trim();

        if (!code || !name) {
            alert("Subject code and name are required.");
            return;
        }

        try {
            const resp = await fetch("/api/v1/master/subjects", {
                method: "POST",
                headers: getAuthHeaders(),
                body: JSON.stringify({ subjectCode: code, subjectName: name, semester: sem, departmentId: deptId, description: desc, isActive: true }),
                credentials: "include"
            });
            const resJson = await resp.json();
            if (resp.ok && resJson.success) {
                masterModalInstance.hide();
                showAlert("success", "Subject created successfully.");
                await loadSubjects();
            } else {
                alert(resJson.message || resJson.error || "Failed to create subject.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

async function openEditSubjectModal(id, code, name, sem, deptId, desc, isActive) {
    const res = await fetch("/api/v1/master/departments/active", { credentials: "include" });
    const json = await res.json();
    const depts = json.data || [];

    document.getElementById("masterModalTitle").textContent = "Edit Subject";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Subject Code</label>
            <input type="text" id="mSubCode" class="form-control" value="${code}" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Subject Full Name</label>
            <input type="text" id="mSubName" class="form-control" value="${name}" required>
        </div>
        <div class="row g-2 mb-3">
            <div class="col-md-6">
                <label class="form-label small fw-semibold">Department</label>
                <select id="mSubDept" class="form-select" required>
                    ${depts.map(d => `<option value="${d.departmentId}" ${d.departmentId === deptId ? 'selected' : ''}>${d.departmentCode} - ${d.departmentName}</option>`).join("")}
                </select>
            </div>
            <div class="col-md-6">
                <label class="form-label small fw-semibold">Semester (1-8)</label>
                <select id="mSubSem" class="form-select" required>
                    ${[1,2,3,4,5,6,7,8].map(s => `<option value="${s}" ${s === sem ? 'selected' : ''}>Semester ${s}</option>`).join("")}
                </select>
            </div>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Syllabus Overview / Description</label>
            <textarea id="mSubDesc" class="form-control" rows="3">${desc}</textarea>
        </div>
    `;

    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const newCode = document.getElementById("mSubCode").value.trim();
        const newName = document.getElementById("mSubName").value.trim();
        const newDeptId = parseInt(document.getElementById("mSubDept").value, 10);
        const newSem = parseInt(document.getElementById("mSubSem").value, 10);
        const newDesc = document.getElementById("mSubDesc").value.trim();

        try {
            const resp = await fetch(`/api/v1/master/subjects/${id}`, {
                method: "PUT",
                headers: getAuthHeaders(),
                body: JSON.stringify({ subjectCode: newCode, subjectName: newName, semester: newSem, departmentId: newDeptId, description: newDesc, isActive }),
                credentials: "include"
            });
            const resJson = await resp.json();
            if (resp.ok && resJson.success) {
                masterModalInstance.hide();
                showAlert("success", "Subject updated successfully.");
                await loadSubjects();
            } else {
                alert(resJson.message || resJson.error || "Failed to update subject.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

async function toggleSubjectStatus(id, newStatus) {
    if (!confirm(`Are you sure you want to ${newStatus ? 'activate' : 'deactivate'} this subject?`)) return;

    try {
        const res = await fetch(`/api/v1/master/subjects/${id}/status`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ isActive: newStatus }),
            credentials: "include"
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showAlert("info", `Subject status changed to ${newStatus ? 'Active' : 'Inactive'}.`);
            await loadSubjects();
        } else {
            alert(json.message || json.error || "Failed to update status.");
        }
    } catch {
        alert("Network error.");
    }
}

// =========================================================================
// 4. SYLLABUS UNITS
// =========================================================================

async function loadUnits() {
    const subjectId = document.getElementById("unitSubjectSelect").value;
    const addBtn = document.getElementById("btnAddUnitBtn");
    const tbody = document.getElementById("unitsTableBody");

    if (!subjectId) {
        addBtn.disabled = true;
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Select a subject above to inspect its syllabus units.</td></tr>`;
        return;
    }

    addBtn.disabled = false;
    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div> Loading syllabus units...</td></tr>`;

    try {
        const res = await fetch(`/api/v1/master/subjects/${subjectId}/units`, { credentials: "include" });
        const json = await res.json();
        const list = json.data || [];

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">No syllabus units registered for this subject yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(u => `
            <tr>
                <td><span class="badge bg-primary">Unit ${u.unitNumber}</span></td>
                <td class="fw-semibold">${u.unitTitle}</td>
                <td class="text-muted small">${u.description || '-'}</td>
                <td>
                    <span class="badge ${u.isActive ? 'bg-success' : 'bg-secondary'}">
                        ${u.isActive ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td class="text-end admin-only ${currentUser && currentUser.role === 'ADMIN' ? '' : 'd-none'}">
                    <button class="btn btn-outline-primary btn-sm me-1" onclick="openEditUnitModal(${u.unitId}, ${u.unitNumber}, '${escapeHtml(u.unitTitle)}', '${escapeHtml(u.description || '')}', ${u.isActive})">Edit</button>
                    <button class="btn btn-outline-${u.isActive ? 'danger' : 'success'} btn-sm" onclick="toggleUnitStatus(${u.unitId}, ${!u.isActive})">
                        ${u.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>
        `).join("");
    } catch {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-danger">Failed to load syllabus units.</td></tr>`;
    }
}

function openAddUnitModal() {
    const subjectId = document.getElementById("unitSubjectSelect").value;
    if (!subjectId) return;

    document.getElementById("masterModalTitle").textContent = "Add Syllabus Unit";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Unit Number (1 to 10)</label>
            <input type="number" id="mUnitNum" class="form-control" min="1" max="10" placeholder="e.g. 1" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Unit Title</label>
            <input type="text" id="mUnitTitle" class="form-control" placeholder="e.g. Unit 1: Introduction to DBMS and Data Models" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Syllabus Coverage / Topics</label>
            <textarea id="mUnitDesc" class="form-control" rows="3" placeholder="Key topics covered in this unit..."></textarea>
        </div>
    `;

    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const num = parseInt(document.getElementById("mUnitNum").value, 10);
        const title = document.getElementById("mUnitTitle").value.trim();
        const desc = document.getElementById("mUnitDesc").value.trim();

        if (!num || !title) {
            alert("Unit number and title are required.");
            return;
        }

        try {
            const res = await fetch(`/api/v1/master/subjects/${subjectId}/units`, {
                method: "POST",
                headers: getAuthHeaders(),
                body: JSON.stringify({ unitNumber: num, unitTitle: title, description: desc, isActive: true }),
                credentials: "include"
            });
            const json = await res.json();
            if (res.ok && json.success) {
                masterModalInstance.hide();
                showAlert("success", "Syllabus unit created successfully.");
                await loadUnits();
            } else {
                alert(json.message || json.error || "Failed to create unit.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

function openEditUnitModal(unitId, num, title, desc, isActive) {
    document.getElementById("masterModalTitle").textContent = "Edit Syllabus Unit";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Unit Number (1 to 10)</label>
            <input type="number" id="mUnitNum" class="form-control" value="${num}" min="1" max="10" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Unit Title</label>
            <input type="text" id="mUnitTitle" class="form-control" value="${title}" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Syllabus Coverage / Topics</label>
            <textarea id="mUnitDesc" class="form-control" rows="3">${desc}</textarea>
        </div>
    `;

    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const newNum = parseInt(document.getElementById("mUnitNum").value, 10);
        const newTitle = document.getElementById("mUnitTitle").value.trim();
        const newDesc = document.getElementById("mUnitDesc").value.trim();

        try {
            const res = await fetch(`/api/v1/master/units/${unitId}`, {
                method: "PUT",
                headers: getAuthHeaders(),
                body: JSON.stringify({ unitNumber: newNum, unitTitle: newTitle, description: newDesc, isActive }),
                credentials: "include"
            });
            const json = await res.json();
            if (res.ok && json.success) {
                masterModalInstance.hide();
                showAlert("success", "Syllabus unit updated successfully.");
                await loadUnits();
            } else {
                alert(json.message || json.error || "Failed to update unit.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

async function toggleUnitStatus(unitId, newStatus) {
    try {
        const res = await fetch(`/api/v1/master/units/${unitId}/status`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ isActive: newStatus }),
            credentials: "include"
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showAlert("info", `Unit status updated.`);
            await loadUnits();
        } else {
            alert(json.message || json.error || "Failed to update unit status.");
        }
    } catch {
        alert("Network error.");
    }
}

// =========================================================================
// 5. CATEGORIES
// =========================================================================

async function loadCategories() {
    const tbody = document.getElementById("categoriesTableBody");
    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div> Loading categories...</td></tr>`;

    try {
        const res = await fetch("/api/v1/master/categories", { credentials: "include" });
        const json = await res.json();
        const list = json.data && json.data.content ? json.data.content : [];

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">No resource categories registered.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(c => `
            <tr>
                <td class="text-muted small">#${c.categoryId}</td>
                <td class="fw-bold">${c.categoryName}</td>
                <td class="text-muted small">${c.description || '-'}</td>
                <td>
                    <span class="badge ${c.isActive ? 'bg-success' : 'bg-secondary'}">
                        ${c.isActive ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td class="text-end admin-only ${currentUser && currentUser.role === 'ADMIN' ? '' : 'd-none'}">
                    <button class="btn btn-outline-primary btn-sm me-1" onclick="openEditCategoryModal(${c.categoryId}, '${escapeHtml(c.categoryName)}', '${escapeHtml(c.description || '')}', ${c.isActive})">Edit</button>
                    <button class="btn btn-outline-${c.isActive ? 'danger' : 'success'} btn-sm" onclick="toggleCategoryStatus(${c.categoryId}, ${!c.isActive})">
                        ${c.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>
        `).join("");
    } catch {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-danger">Failed to load categories.</td></tr>`;
    }
}

function openAddCategoryModal() {
    document.getElementById("masterModalTitle").textContent = "Add Resource Category";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Category Name</label>
            <input type="text" id="mCatName" class="form-control" placeholder="e.g. Lab Manuals, Syllabus" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Description</label>
            <input type="text" id="mCatDesc" class="form-control" placeholder="Brief description of this resource type...">
        </div>
    `;

    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const name = document.getElementById("mCatName").value.trim();
        const desc = document.getElementById("mCatDesc").value.trim();
        if (!name) {
            alert("Category name is required.");
            return;
        }

        try {
            const res = await fetch("/api/v1/master/categories", {
                method: "POST",
                headers: getAuthHeaders(),
                body: JSON.stringify({ categoryName: name, description: desc, isActive: true }),
                credentials: "include"
            });
            const json = await res.json();
            if (res.ok && json.success) {
                masterModalInstance.hide();
                showAlert("success", "Category created successfully.");
                await loadCategories();
            } else {
                alert(json.message || json.error || "Failed to create category.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

function openEditCategoryModal(id, name, desc, isActive) {
    document.getElementById("masterModalTitle").textContent = "Edit Category";
    document.getElementById("masterModalBody").innerHTML = `
        <div class="mb-3">
            <label class="form-label small fw-semibold">Category Name</label>
            <input type="text" id="mCatName" class="form-control" value="${name}" required>
        </div>
        <div class="mb-3">
            <label class="form-label small fw-semibold">Description</label>
            <input type="text" id="mCatDesc" class="form-control" value="${desc}">
        </div>
    `;

    const saveBtn = document.getElementById("masterModalSaveBtn");
    saveBtn.onclick = async () => {
        const newName = document.getElementById("mCatName").value.trim();
        const newDesc = document.getElementById("mCatDesc").value.trim();

        try {
            const res = await fetch(`/api/v1/master/categories/${id}`, {
                method: "PUT",
                headers: getAuthHeaders(),
                body: JSON.stringify({ categoryName: newName, description: newDesc, isActive }),
                credentials: "include"
            });
            const json = await res.json();
            if (res.ok && json.success) {
                masterModalInstance.hide();
                showAlert("success", "Category updated successfully.");
                await loadCategories();
            } else {
                alert(json.message || json.error || "Failed to update category.");
            }
        } catch {
            alert("Network error.");
        }
    };
    masterModalInstance.show();
}

async function toggleCategoryStatus(id, newStatus) {
    try {
        const res = await fetch(`/api/v1/master/categories/${id}/status`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ isActive: newStatus }),
            credentials: "include"
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showAlert("info", `Category status updated.`);
            await loadCategories();
        } else {
            alert(json.message || json.error || "Failed to update category status.");
        }
    } catch {
        alert("Network error.");
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/'/g, "\\'").replace(/"/g, "&quot;");
}
