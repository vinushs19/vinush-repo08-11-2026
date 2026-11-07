/**
 * Academic Resource Repository JavaScript Client
 */

let currentUser = null;
let currentPage = 0;
let totalPages = 0;
let currentResources = [];
let allSubjects = [];
let allCategories = [];

document.addEventListener('DOMContentLoaded', async () => {
    await checkAuth();
    await loadInitialMasterData();
    setupEventListeners();
    await loadResources(0);
});

// ---- 1. Authentication & Role Inspection ----

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

        // Show Upload button for Faculty and Admin
        if (currentUser.role === 'FACULTY' || currentUser.role === 'ADMIN') {
            const btnUpload = document.getElementById('btnOpenUploadModal');
            if (btnUpload) btnUpload.classList.remove('d-none');
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

// ---- 2. Master Data Loading for Filters and Modals ----

async function loadInitialMasterData() {
    try {
        const [deptRes, catRes, subjRes] = await Promise.all([
            fetch('/api/v1/master/departments/active'),
            fetch('/api/v1/master/categories/active'),
            fetch('/api/v1/master/subjects?size=200&activeOnly=true')
        ]);

        if (deptRes.ok) {
            const depts = (await deptRes.json()).data;
            populateDeptDropdowns(depts);
        }

        if (catRes.ok) {
            allCategories = (await catRes.json()).data;
            populateCategoryDropdowns(allCategories);
        }

        if (subjRes.ok) {
            const pageData = (await subjRes.json()).data;
            allSubjects = pageData.content || pageData;
            populateSubjectDropdowns(allSubjects);
        }
    } catch (e) {
        showAlert('Failed to load filter taxonomies: ' + e.message, 'danger');
    }
}

function populateDeptDropdowns(depts) {
    const filterDept = document.getElementById('filterDept');
    const uploadDept = document.getElementById('uploadDept');

    depts.forEach(d => {
        const opt = new Option(`${d.departmentCode} — ${d.departmentName}`, d.departmentId);
        filterDept?.add(opt);
        uploadDept?.add(new Option(`${d.departmentCode} — ${d.departmentName}`, d.departmentId));
    });
}

function populateCategoryDropdowns(categories) {
    const filterCat = document.getElementById('filterCategory');
    const uploadCat = document.getElementById('uploadCategory');
    const editCat = document.getElementById('editCategory');

    categories.forEach(c => {
        filterCat?.add(new Option(c.categoryName, c.categoryId));
        uploadCat?.add(new Option(c.categoryName, c.categoryId));
        editCat?.add(new Option(c.categoryName, c.categoryId));
    });
}

function populateSubjectDropdowns(subjects) {
    const filterSubj = document.getElementById('filterSubject');
    const uploadSubj = document.getElementById('uploadSubject');
    const editSubj = document.getElementById('editSubject');

    subjects.forEach(s => {
        const label = `${s.subjectCode} — ${s.subjectName} (Sem ${s.semester})`;
        filterSubj?.add(new Option(label, s.subjectId));
        uploadSubj?.add(new Option(label, s.subjectId));
        editSubj?.add(new Option(label, s.subjectId));
    });
}

// ---- 3. Dynamic Unit & Subject Filtering ----

async function loadUnitsForSubject(subjectId, targetSelectId) {
    const targetSelect = document.getElementById(targetSelectId);
    if (!targetSelect) return;

    targetSelect.innerHTML = '<option value="">None / Complete Subject</option>';
    if (!subjectId) return;

    try {
        const resp = await fetch(`/api/v1/master/subjects/${subjectId}/units?activeOnly=true`);
        if (resp.ok) {
            const units = (await resp.json()).data;
            units.forEach(u => {
                targetSelect.add(new Option(`Unit ${u.unitNumber}: ${u.unitTitle}`, u.unitId));
            });
        }
    } catch (e) {
        console.warn('Failed to load units for subject', e);
    }
}

// ---- 4. Resource Listing & Rendering ----

async function loadResources(page = 0) {
    currentPage = page;
    const cardsContainer = document.getElementById('resourceCardsContainer');
    const countEl = document.getElementById('resultsCount');
    const emptyEl = document.getElementById('emptyStateContainer');

    cardsContainer.innerHTML = `
        <div class="col-12 text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="text-muted small mt-2">Loading academic resources...</p>
        </div>
    `;

    const dept = document.getElementById('filterDept')?.value;
    const sem = document.getElementById('filterSemester')?.value;
    const subj = document.getElementById('filterSubject')?.value;
    const unit = document.getElementById('filterUnit')?.value;
    const cat = document.getElementById('filterCategory')?.value;
    const resType = document.getElementById('filterResourceType')?.value;
    const search = document.getElementById('filterSearch')?.value;
    const sort = document.getElementById('sortSelect')?.value || 'uploadDate,desc';

    const params = new URLSearchParams({
        page: page,
        size: 9,
        sort: sort
    });

    if (dept) params.append('departmentId', dept);
    if (sem) params.append('semester', sem);
    if (subj) params.append('subjectId', subj);
    if (unit) params.append('unitId', unit);
    if (cat) params.append('categoryId', cat);
    if (resType) params.append('resourceType', resType);
    if (search && search.trim()) params.append('search', search.trim());

    try {
        const resp = await fetch(`/api/v1/resources?${params.toString()}`);
        if (!resp.ok) {
            throw new Error(`Server returned HTTP ${resp.status}`);
        }
        const json = await resp.json();
        const pageData = json.data;
        currentResources = pageData.content || [];
        totalPages = pageData.totalPages || 0;

        countEl.textContent = `Showing ${currentResources.length} of ${pageData.totalElements} resources`;

        if (currentResources.length === 0) {
            cardsContainer.innerHTML = '';
            emptyEl.classList.remove('d-none');
        } else {
            emptyEl.classList.add('d-none');
            renderResourceCards(currentResources);
        }

        renderPagination(pageData);
    } catch (e) {
        cardsContainer.innerHTML = '';
        showAlert('Error loading resources: ' + e.message, 'danger');
    }
}

function renderResourceCards(resources) {
    const container = document.getElementById('resourceCardsContainer');
    container.innerHTML = '';

    resources.forEach(r => {
        const cardCol = document.createElement('div');
        cardCol.className = 'col-md-6 col-lg-4';

        const fileExt = (r.fileType || 'file').toUpperCase();
        const badgeClass = getFileTypeBadgeClass(r.fileType);
        const canManage = canModifyResource(r);

        cardCol.innerHTML = `
            <div class="resource-card shadow-sm">
                <div class="resource-card-body">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <span class="badge ${badgeClass} file-icon-badge">${escapeHtml(fileExt)}</span>
                        <span class="badge bg-light text-secondary border small">${escapeHtml(r.resourceType || 'Resource')}</span>
                    </div>
                    <h5 class="fw-bold mb-1 text-truncate" title="${escapeHtml(r.title)}">
                        <a href="/resource-detail.html?id=${r.resourceId}" class="text-dark text-decoration-none">${escapeHtml(r.title)}</a>
                    </h5>
                    <p class="text-muted small mb-3" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 2.4rem;">
                        ${escapeHtml(r.description || 'No description provided.')}
                    </p>
                    <div class="d-flex flex-wrap gap-1 mb-2">
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle small">${escapeHtml(r.subjectCode || 'SUBJ')}</span>
                        <span class="badge bg-secondary-subtle text-secondary border small">Sem ${r.semester || '-'}</span>
                        ${r.unitNumber ? `<span class="badge bg-info-subtle text-info-emphasis border small">Unit ${r.unitNumber}</span>` : ''}
                        <span class="badge bg-light text-dark border small">${escapeHtml(r.categoryName || 'General')}</span>
                    </div>
                    <div class="small text-muted mt-2">
                        <div>📁 <span class="fw-semibold text-break">${escapeHtml(r.originalFilename || r.fileName)}</span></div>
                        <div class="d-flex justify-content-between mt-1">
                            <span>📦 ${escapeHtml(r.fileSizeFormatted)}</span>
                            <span>👤 ${escapeHtml(r.uploadedByName || 'Faculty')}</span>
                        </div>
                    </div>
                </div>
                <div class="resource-card-footer d-flex justify-content-between align-items-center">
                    <div class="d-flex gap-1">
                        <a href="/resource-detail.html?id=${r.resourceId}" class="btn btn-outline-primary btn-sm">Details</a>
                        <a href="/api/v1/resources/${r.resourceId}/download" class="btn btn-primary btn-sm">Download</a>
                    </div>
                    ${canManage ? `
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-secondary btn-edit-res" data-id="${r.resourceId}" title="Edit metadata">✏️</button>
                            <button class="btn btn-outline-danger btn-delete-res" data-id="${r.resourceId}" title="Delete resource">🗑️</button>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;

        container.appendChild(cardCol);
    });

    // Attach listeners for Edit and Delete
    container.querySelectorAll('.btn-edit-res').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = parseInt(e.currentTarget.getAttribute('data-id'), 10);
            openEditModal(id);
        });
    });

    container.querySelectorAll('.btn-delete-res').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = parseInt(e.currentTarget.getAttribute('data-id'), 10);
            deleteResource(id);
        });
    });
}

function getFileTypeBadgeClass(fileType) {
    if (!fileType) return 'badge-txt';
    const ext = fileType.toLowerCase();
    switch (ext) {
        case 'pdf': return 'badge-pdf';
        case 'docx':
        case 'doc': return 'badge-docx';
        case 'pptx':
        case 'ppt': return 'badge-pptx';
        case 'xlsx':
        case 'xls':
        case 'csv': return 'badge-xlsx';
        default: return 'badge-txt';
    }
}

function canModifyResource(resource) {
    if (!currentUser) return false;
    if (currentUser.role === 'ADMIN') return true;
    if (currentUser.role === 'FACULTY' && resource.uploadedById === currentUser.userId) return true;
    return false;
}

// ---- 5. Pagination Rendering ----

function renderPagination(pageData) {
    const nav = document.getElementById('paginationNav');
    const ul = document.getElementById('paginationList');
    ul.innerHTML = '';

    if (pageData.totalPages <= 1) {
        nav.classList.add('d-none');
        return;
    }
    nav.classList.remove('d-none');

    // Prev
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${pageData.first ? 'disabled' : ''}`;
    prevLi.innerHTML = `<button class="page-link">&laquo; Prev</button>`;
    prevLi.addEventListener('click', () => { if (!pageData.first) loadResources(currentPage - 1); });
    ul.appendChild(prevLi);

    // Page Numbers
    for (let i = 0; i < pageData.totalPages; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === currentPage ? 'active' : ''}`;
        li.innerHTML = `<button class="page-link">${i + 1}</button>`;
        const targetPage = i;
        li.addEventListener('click', () => loadResources(targetPage));
        ul.appendChild(li);
    }

    // Next
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${pageData.last ? 'disabled' : ''}`;
    nextLi.innerHTML = `<button class="page-link">Next &raquo;</button>`;
    nextLi.addEventListener('click', () => { if (!pageData.last) loadResources(currentPage + 1); });
    ul.appendChild(nextLi);
}

// ---- 6. Event Listeners & Upload / Edit Handlers ----

function setupEventListeners() {
    // Subject filter change -> update unit filter
    document.getElementById('filterSubject')?.addEventListener('change', (e) => {
        loadUnitsForSubject(e.target.value, 'filterUnit');
    });

    // Upload Subject change -> update upload unit
    document.getElementById('uploadSubject')?.addEventListener('change', (e) => {
        loadUnitsForSubject(e.target.value, 'uploadUnit');
    });

    // Edit Subject change -> update edit unit
    document.getElementById('editSubject')?.addEventListener('change', (e) => {
        loadUnitsForSubject(e.target.value, 'editUnit');
    });

    // Filter Apply & Reset
    document.getElementById('btnApplyFilters')?.addEventListener('click', () => loadResources(0));
    document.getElementById('btnResetFilters')?.addEventListener('click', () => {
        document.getElementById('filterDept').value = '';
        document.getElementById('filterSemester').value = '';
        document.getElementById('filterSubject').value = '';
        document.getElementById('filterUnit').value = '';
        document.getElementById('filterCategory').value = '';
        document.getElementById('filterResourceType').value = '';
        document.getElementById('filterSearch').value = '';
        loadResources(0);
    });

    document.getElementById('sortSelect')?.addEventListener('change', () => loadResources(0));

    // Upload Form Submit
    document.getElementById('uploadForm')?.addEventListener('submit', handleUploadSubmit);

    // Edit Form Submit
    document.getElementById('editForm')?.addEventListener('submit', handleEditSubmit);
}

async function handleUploadSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitUpload');
    const spinner = document.getElementById('uploadSpinner');
    btn.disabled = true;
    spinner.classList.remove('d-none');

    const form = e.target;
    const formData = new FormData(form);

    const csrfToken = getCsrfToken();

    try {
        const resp = await fetch('/api/v1/resources', {
            method: 'POST',
            headers: {
                'X-XSRF-TOKEN': csrfToken
            },
            body: formData
        });

        const json = await resp.json();
        if (!resp.ok) {
            throw new Error(json.message || 'Failed to upload resource');
        }

        showAlert('Resource uploaded successfully!', 'success');
        form.reset();
        bootstrap.Modal.getInstance(document.getElementById('uploadModal'))?.hide();
        await loadResources(0);
    } catch (err) {
        showAlert(err.message, 'danger');
    } finally {
        btn.disabled = false;
        spinner.classList.add('d-none');
    }
}

function openEditModal(resourceId) {
    const res = currentResources.find(r => r.resourceId === resourceId);
    if (!res) return;

    document.getElementById('editResourceId').value = res.resourceId;
    document.getElementById('editTitle').value = res.title;
    document.getElementById('editSubject').value = res.subjectId;
    document.getElementById('editCategory').value = res.categoryId;
    document.getElementById('editResourceType').value = res.resourceType;
    document.getElementById('editDescription').value = res.description || '';

    loadUnitsForSubject(res.subjectId, 'editUnit').then(() => {
        if (res.unitId) {
            document.getElementById('editUnit').value = res.unitId;
        }
    });

    const modal = new bootstrap.Modal(document.getElementById('editModal'));
    modal.show();
}

async function handleEditSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('editResourceId').value;
    const payload = {
        title: document.getElementById('editTitle').value,
        subjectId: parseInt(document.getElementById('editSubject').value, 10),
        unitId: document.getElementById('editUnit').value ? parseInt(document.getElementById('editUnit').value, 10) : null,
        categoryId: parseInt(document.getElementById('editCategory').value, 10),
        resourceType: document.getElementById('editResourceType').value,
        description: document.getElementById('editDescription').value
    };

    const csrfToken = getCsrfToken();

    try {
        const resp = await fetch(`/api/v1/resources/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken
            },
            body: JSON.stringify(payload)
        });

        const json = await resp.json();
        if (!resp.ok) {
            throw new Error(json.message || 'Failed to update resource');
        }

        showAlert('Resource metadata updated successfully!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('editModal'))?.hide();
        await loadResources(currentPage);
    } catch (err) {
        showAlert(err.message, 'danger');
    }
}

async function deleteResource(resourceId) {
    if (!confirm('Are you sure you want to deactivate and remove this academic resource?')) {
        return;
    }

    const csrfToken = getCsrfToken();

    try {
        const resp = await fetch(`/api/v1/resources/${resourceId}`, {
            method: 'DELETE',
            headers: {
                'X-XSRF-TOKEN': csrfToken
            }
        });

        const json = await resp.json();
        if (!resp.ok) {
            throw new Error(json.message || 'Failed to delete resource');
        }

        showAlert('Resource successfully deactivated.', 'success');
        await loadResources(currentPage);
    } catch (err) {
        showAlert(err.message, 'danger');
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
