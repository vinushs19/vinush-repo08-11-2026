/**
 * Academic Resource Detail View JavaScript Client
 */

let currentUser = null;
let currentResource = null;
let resourceId = null;

document.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    resourceId = params.get('id');

    if (!resourceId) {
        window.location.href = '/resources.html';
        return;
    }

    await checkAuth();
    await loadResourceDetails();
    loadRecommendations();
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

async function loadResourceDetails() {
    try {
        const resp = await fetch(`/api/v1/resources/${resourceId}`);
        if (!resp.ok) {
            throw new Error(`Resource #${resourceId} could not be loaded.`);
        }
        const json = await resp.json();
        currentResource = json.data;
        renderResource(currentResource);
    } catch (e) {
        showAlert('Error loading resource details: ' + e.message, 'danger');
    }
}

function renderResource(r) {
    document.title = `${r.title} — College Resource Finder`;
    document.getElementById('breadcrumbTitle').textContent = r.title;

    document.getElementById('resTitle').textContent = r.title;
    document.getElementById('resDescription').textContent = r.description || 'No additional description provided.';
    document.getElementById('resCategoryBadge').textContent = r.categoryName || 'General';
    document.getElementById('resTypeBadge').textContent = r.resourceType || 'Academic Resource';

    const statusBadge = document.getElementById('resStatusBadge');
    statusBadge.textContent = r.status;
    if (r.status === 'ACTIVE') {
        statusBadge.className = 'badge bg-success';
    } else if (r.status === 'ARCHIVED') {
        statusBadge.className = 'badge bg-warning text-dark';
    } else {
        statusBadge.className = 'badge bg-danger';
    }

    // Academic Classification
    document.getElementById('resDept').textContent = `${r.departmentCode || '-'} (${r.departmentName || '-'})`;
    document.getElementById('resSemester').textContent = `Semester ${r.semester || '-'}`;
    document.getElementById('resSubject').textContent = `${r.subjectCode || '-'} — ${r.subjectName || '-'}`;
    document.getElementById('resUnit').textContent = r.unitNumber ?
        `Unit ${r.unitNumber}: ${r.unitTitle}` : 'General / Entire Subject';

    // File Details
    document.getElementById('resOrigFilename').textContent = r.originalFilename || r.fileName;
    document.getElementById('resFileType').textContent = (r.fileType || '-').toUpperCase();
    document.getElementById('resFileSize').textContent = `${r.fileSizeFormatted} (${(r.fileSize || 0).toLocaleString()} bytes)`;
    document.getElementById('resStorageFilename').textContent = r.fileName;

    // Provenance
    document.getElementById('resUploader').textContent = r.uploadedByName || 'Unknown';
    document.getElementById('resUploaderRole').textContent = r.uploadedByRole || '-';
    document.getElementById('resUploadDate').textContent = formatDate(r.uploadDate);
    document.getElementById('resUpdateDate').textContent = r.updatedAt ? formatDate(r.updatedAt) : '-';

    // Actions
    const downloadUrl = `/api/v1/resources/${r.resourceId}/download`;
    const previewUrl = `/api/v1/resources/${r.resourceId}/preview`;

    const btnDownload = document.getElementById('btnDownload');
    btnDownload.href = downloadUrl;

    const btnNewTab = document.getElementById('btnOpenNewTab');
    btnNewTab.href = previewUrl;

    // Preview Pane
    const previewFrame = document.getElementById('docPreviewFrame');
    const ext = (r.fileType || '').toLowerCase();
    if (ext === 'pdf' || ext === 'txt' || ext === 'png' || ext === 'jpg' || ext === 'jpeg') {
        previewFrame.src = previewUrl;
    } else {
        document.getElementById('previewContainer').innerHTML = `
            <div class="text-center py-5 text-muted">
                <div style="font-size: 3rem;">📄</div>
                <h6 class="fw-bold mt-2">Inline preview not supported for .${ext}</h6>
                <p class="small">Use the download button above to inspect this document locally.</p>
                <a href="${downloadUrl}" class="btn btn-outline-primary btn-sm">Download File (${escapeHtml(r.fileSizeFormatted)})</a>
            </div>
        `;
    }

    // Role-based Organize, Edit & Delete buttons
    if (currentUser && (currentUser.role === 'ADMIN' || currentUser.role === 'FACULTY')) {
        const btnOrg = document.getElementById('btnOrganizeResource');
        if (btnOrg) {
            btnOrg.classList.remove('d-none');
            btnOrg.classList.add('d-flex');
            document.getElementById('organizeForm').onsubmit = handleOrganizeSubmit;
        }
    }

    const canManage = canModifyResource(r);
    if (canManage) {
        const btnEdit = document.getElementById('btnEditResource');
        const btnDelete = document.getElementById('btnDeleteResource');

        if (btnEdit) {
            btnEdit.classList.remove('d-none');
            btnEdit.classList.add('d-flex');
            btnEdit.addEventListener('click', openEditModal);
        }

        if (btnDelete) {
            btnDelete.classList.remove('d-none');
            btnDelete.classList.add('d-flex');
            btnDelete.addEventListener('click', deleteCurrentResource);
        }
    }

    // AI Summarization setup
    document.getElementById('btnSummarizeResource')?.addEventListener('click', generateOrLoadSummary);

    // If resource already has cached summary in database, display immediately
    if (r.summary && r.summary.trim()) {
        renderSummaryCard({
            summary: r.summary,
            method: 'cached_database',
            fileHash: r.fileHash,
            cached: true,
            wordCount: r.summary.split(/\s+/).length
        });
    }
}

function canModifyResource(r) {
    if (!currentUser || !r) return false;
    if (currentUser.role === 'ADMIN') return true;
    if (currentUser.role === 'FACULTY' && r.uploadedById === currentUser.userId) return true;
    return false;
}

async function openEditModal() {
    if (!currentResource) return;

    // Load master data dropdowns if not loaded
    const [subjRes, catRes] = await Promise.all([
        fetch('/api/v1/master/subjects?size=200&activeOnly=true'),
        fetch('/api/v1/master/categories/active')
    ]);

    const subjSelect = document.getElementById('editSubject');
    const catSelect = document.getElementById('editCategory');
    subjSelect.innerHTML = '';
    catSelect.innerHTML = '';

    if (subjRes.ok) {
        const subjects = (await subjRes.json()).data.content || [];
        subjects.forEach(s => {
            subjSelect.add(new Option(`${s.subjectCode} — ${s.subjectName}`, s.subjectId));
        });
    }

    if (catRes.ok) {
        const categories = (await catRes.json()).data || [];
        categories.forEach(c => {
            catSelect.add(new Option(c.categoryName, c.categoryId));
        });
    }

    document.getElementById('editTitle').value = currentResource.title;
    document.getElementById('editSubject').value = currentResource.subjectId;
    document.getElementById('editCategory').value = currentResource.categoryId;
    document.getElementById('editResourceType').value = currentResource.resourceType;
    document.getElementById('editDescription').value = currentResource.description || '';

    // Load units
    await loadUnitsForEditSubject(currentResource.subjectId);
    if (currentResource.unitId) {
        document.getElementById('editUnit').value = currentResource.unitId;
    }

    subjSelect.onchange = (e) => loadUnitsForEditSubject(e.target.value);

    document.getElementById('editForm').onsubmit = handleEditSubmit;
}

async function loadUnitsForEditSubject(subjectId) {
    const unitSelect = document.getElementById('editUnit');
    unitSelect.innerHTML = '<option value="">None / Complete Subject</option>';
    if (!subjectId) return;

    try {
        const resp = await fetch(`/api/v1/master/subjects/${subjectId}/units?activeOnly=true`);
        if (resp.ok) {
            const units = (await resp.json()).data;
            units.forEach(u => {
                unitSelect.add(new Option(`Unit ${u.unitNumber}: ${u.unitTitle}`, u.unitId));
            });
        }
    } catch (e) {
        console.warn('Failed to load units for subject', e);
    }
}

async function handleEditSubmit(e) {
    e.preventDefault();
    const btnSave = document.getElementById('btnSaveEdit');
    btnSave.disabled = true;

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
        const resp = await fetch(`/api/v1/resources/${resourceId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken
            },
            body: JSON.stringify(payload)
        });

        const json = await resp.json();
        if (!resp.ok) {
            throw new Error(json.message || 'Failed to update resource metadata');
        }

        showAlert('Resource metadata updated successfully!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('editModal'))?.hide();
        await loadResourceDetails();
    } catch (err) {
        showAlert(err.message, 'danger');
    } finally {
        btnSave.disabled = false;
    }
}

async function handleOrganizeSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('btnExecuteOrganize');
    btn.disabled = true;

    const payload = {
        resourceId: parseInt(resourceId, 10),
        destinationCategory: document.getElementById('orgCategory').value,
        subFolderTag: document.getElementById('orgSubFolder').value.trim() || null,
        overwrite: document.getElementById('orgOverwrite').checked,
        remarks: document.getElementById('orgRemarks').value.trim() || null
    };

    const csrfToken = getCsrfToken();

    try {
        const resp = await fetch('/api/v1/files/controlled-save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken
            },
            body: JSON.stringify(payload)
        });

        const json = await resp.json();
        if (!resp.ok) {
            throw new Error(json.message || 'Controlled file-saving operation failed');
        }

        const data = json.data;
        showAlert(`File organized successfully under ${escapeHtml(data.destinationCategory)}! Stored at: ${escapeHtml(data.destinationPath)}`, 'success');
        bootstrap.Modal.getInstance(document.getElementById('organizeModal'))?.hide();
        document.getElementById('organizeForm').reset();
    } catch (err) {
        showAlert(err.message, 'danger');
    } finally {
        btn.disabled = false;
    }
}

async function deleteCurrentResource() {
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

        alert('Resource successfully deactivated. Returning to catalog.');
        window.location.href = '/resources.html';
    } catch (err) {
        showAlert(err.message, 'danger');
    }
}

// ---- Recommendations ----

async function loadRecommendations() {
    const section = document.getElementById('recommendationsSection');
    const loading = document.getElementById('recLoading');
    const empty   = document.getElementById('recEmpty');
    const list    = document.getElementById('recList');
    const count   = document.getElementById('recCount');

    if (!section || !resourceId) return;

    section.style.display = '';

    try {
        const resp = await fetch(`/api/v1/resources/${resourceId}/recommendations?limit=10`);
        if (!resp.ok) {
            section.style.display = 'none';
            return;
        }

        const json = await resp.json();
        const recs = json.data || [];

        loading.style.display = 'none';

        if (recs.length === 0) {
            empty.style.display = '';
            count.textContent = '0';
            return;
        }

        count.textContent = recs.length;
        list.style.display = '';
        list.innerHTML = recs.map(rec => renderRecCard(rec)).join('');
    } catch (e) {
        console.warn('Failed to load recommendations:', e);
        section.style.display = 'none';
    }
}

function renderRecCard(rec) {
    const r = rec.resource;
    const score = rec.relevanceScore;
    const criteria = rec.matchedCriteria || [];

    const ext = (r.fileType || '').toUpperCase();
    const typeLabel = r.resourceType || 'Resource';
    const subjectLabel = r.subjectCode ? `${escapeHtml(r.subjectCode)}` : '';
    const semLabel = r.semester ? `Sem ${r.semester}` : '';
    const unitLabel = r.unitNumber ? `Unit ${r.unitNumber}` : '';
    const sizeLabel = r.fileSizeFormatted || '';

    const criteriaBadges = criteria.map(c =>
        `<span class="badge bg-light text-dark border rec-badge">${escapeHtml(c)}</span>`
    ).join(' ');

    // Score color
    let scoreColor = 'bg-secondary';
    if (score >= 60) scoreColor = 'bg-success';
    else if (score >= 30) scoreColor = 'bg-primary';
    else if (score >= 15) scoreColor = 'bg-info text-dark';

    return `
        <div class="col-md-6 col-lg-4">
            <a href="/resource-detail.html?id=${r.resourceId}" class="text-decoration-none">
                <div class="card rec-card h-100 border">
                    <div class="card-body p-3">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <span class="badge bg-primary rec-badge">${escapeHtml(ext)}</span>
                            <span class="badge ${scoreColor} rec-badge" title="Relevance score">${score}</span>
                        </div>
                        <h6 class="card-title fw-bold mb-1 text-dark" style="font-size: 0.9rem; line-height: 1.3;">
                            ${escapeHtml(r.title)}
                        </h6>
                        <p class="card-text text-muted mb-2" style="font-size: 0.75rem; line-height: 1.3;">
                            ${escapeHtml(typeLabel)}
                            ${subjectLabel ? ' · ' + subjectLabel : ''}
                            ${semLabel ? ' · ' + semLabel : ''}
                            ${unitLabel ? ' · ' + unitLabel : ''}
                            ${sizeLabel ? ' · ' + sizeLabel : ''}
                        </p>
                        <div class="d-flex flex-wrap gap-1">
                            ${criteriaBadges}
                        </div>
                    </div>
                </div>
            </a>
        </div>
    `;
}

// ---- Helpers ----

function getCsrfToken() {
    const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : '';
}

function formatDate(dt) {
    if (!dt) return '-';
    try {
        const d = new Date(dt);
        return d.toLocaleString();
    } catch {
        return dt;
    }
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

async function generateOrLoadSummary() {
    const card = document.getElementById('aiSummaryCard');
    const loading = document.getElementById('aiSummaryLoading');
    const content = document.getElementById('aiSummaryContent');
    const badge = document.getElementById('aiSummaryMethodBadge');
    const meta = document.getElementById('aiSummaryMeta');
    const btn = document.getElementById('btnSummarizeResource');

    card.classList.remove('d-none');
    loading.classList.remove('d-none');
    content.textContent = '';
    btn.disabled = true;

    const csrf = getCsrfToken();
    try {
        const resp = await fetch(`/api/v1/ai/resources/${resourceId}/summarize`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrf
            },
            body: JSON.stringify({ maxSentences: 4, provider: 'qwen3' })
        });

        const json = await resp.json();
        if (!resp.ok || !json.success) {
            throw new Error(json.message || 'Summarization failed');
        }

        renderSummaryCard(json.data);
    } catch (e) {
        content.innerHTML = `<span class="text-danger small">⚠️ ${escapeHtml(e.message)}</span>`;
        badge.textContent = 'Failed';
        badge.className = 'badge bg-danger';
    } finally {
        loading.classList.add('d-none');
        btn.disabled = false;
    }
}

function renderSummaryCard(data) {
    const card = document.getElementById('aiSummaryCard');
    const content = document.getElementById('aiSummaryContent');
    const badge = document.getElementById('aiSummaryMethodBadge');
    const meta = document.getElementById('aiSummaryMeta');

    card.classList.remove('d-none');
    content.textContent = data.summary;

    if (data.cached) {
        badge.textContent = 'Cached (MySQL)';
        badge.className = 'badge bg-success';
    } else if (data.method === 'qwen3_ollama_abstractive') {
        badge.textContent = 'Qwen3-0.6B LLM';
        badge.className = 'badge bg-primary';
    } else {
        badge.textContent = 'Extractive Baseline';
        badge.className = 'badge bg-secondary';
    }

    const words = data.wordCount ? `${data.wordCount} words` : '';
    const hash = data.fileHash ? `SHA-256: ${data.fileHash.substring(0, 12)}...` : '';
    meta.textContent = [words, hash].filter(Boolean).join(' | ');
}
