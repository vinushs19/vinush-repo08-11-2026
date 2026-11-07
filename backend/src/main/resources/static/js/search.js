/**
 * Academic Resource Search & History JavaScript Client
 */

let currentUser = null;
let currentPage = 0;
let totalPages = 0;

document.addEventListener('DOMContentLoaded', async () => {
    await checkAuth();
    await loadMasterTaxonomies();
    await loadSearchHistory();
    setupEventListeners();

    // Check URL params for deep-linked query
    const params = new URLSearchParams(window.location.search);
    const initialQ = params.get('q') || params.get('query');
    if (initialQ) {
        document.getElementById('searchInput').value = initialQ;
        await executeSearch(0);
    } else {
        await executeSearch(0);
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
    } catch {
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

// ---- Master Data for Filters ----

async function loadMasterTaxonomies() {
    try {
        const [deptRes, catRes, subjRes] = await Promise.all([
            fetch('/api/v1/master/departments/active'),
            fetch('/api/v1/master/categories/active'),
            fetch('/api/v1/master/subjects?size=200&activeOnly=true')
        ]);

        if (deptRes.ok) {
            const depts = (await deptRes.json()).data;
            const filterDept = document.getElementById('filterDept');
            depts.forEach(d => filterDept?.add(new Option(`${d.departmentCode} — ${d.departmentName}`, d.departmentId)));
        }

        if (catRes.ok) {
            const cats = (await catRes.json()).data;
            const filterCat = document.getElementById('filterCategory');
            cats.forEach(c => filterCat?.add(new Option(c.categoryName, c.categoryId)));
        }

        if (subjRes.ok) {
            const pageData = (await subjRes.json()).data;
            const subjects = pageData.content || pageData;
            const filterSubj = document.getElementById('filterSubject');
            subjects.forEach(s => filterSubj?.add(new Option(`${s.subjectCode} — ${s.subjectName}`, s.subjectId)));
        }
    } catch (e) {
        console.warn('Failed to load master taxonomies', e);
    }
}

async function loadUnitsForSubject(subjectId) {
    const filterUnit = document.getElementById('filterUnit');
    if (!filterUnit) return;
    filterUnit.innerHTML = '<option value="">All Units</option>';
    if (!subjectId) return;

    try {
        const resp = await fetch(`/api/v1/master/subjects/${subjectId}/units?activeOnly=true`);
        if (resp.ok) {
            const units = (await resp.json()).data;
            units.forEach(u => filterUnit.add(new Option(`Unit ${u.unitNumber}: ${u.unitTitle}`, u.unitId)));
        }
    } catch (e) {
        console.warn('Failed to load units', e);
    }
}

// ---- Search History ----

async function loadSearchHistory() {
    const container = document.getElementById('historyChipsContainer');
    const noHist = document.getElementById('noHistoryText');
    const btnClearAll = document.getElementById('btnClearAllHistory');

    try {
        const resp = await fetch('/api/v1/search/history?size=8');
        if (!resp.ok) return;

        const page = (await resp.json()).data;
        const items = page.content || [];

        // Remove old chips
        container.querySelectorAll('.history-chip').forEach(el => el.remove());

        if (items.length === 0) {
            noHist?.classList.remove('d-none');
            btnClearAll?.classList.add('d-none');
            return;
        }

        noHist?.classList.add('d-none');
        btnClearAll?.classList.remove('d-none');

        items.forEach(item => {
            const chip = document.createElement('span');
            chip.className = 'history-chip';
            chip.innerHTML = `
                <span class="hist-text" title="Re-run search">${escapeHtml(item.searchQuery)}</span>
                <span class="btn-del-hist" data-id="${item.searchId}" title="Remove entry">&times;</span>
            `;

            chip.querySelector('.hist-text').addEventListener('click', () => {
                document.getElementById('searchInput').value = item.searchQuery;
                executeSearch(0);
            });

            chip.querySelector('.btn-del-hist').addEventListener('click', async (e) => {
                e.stopPropagation();
                await deleteHistoryItem(item.searchId);
            });

            container.appendChild(chip);
        });
    } catch (e) {
        console.warn('Failed to load search history', e);
    }
}

async function deleteHistoryItem(id) {
    const csrf = getCsrfToken();
    try {
        await fetch(`/api/v1/search/history/${id}`, {
            method: 'DELETE',
            headers: { 'X-XSRF-TOKEN': csrf }
        });
        await loadSearchHistory();
    } catch (e) {
        console.warn('Failed to delete history item', e);
    }
}

async function clearAllHistory() {
    if (!confirm('Clear all search history?')) return;
    const csrf = getCsrfToken();
    try {
        await fetch('/api/v1/search/history', {
            method: 'DELETE',
            headers: { 'X-XSRF-TOKEN': csrf }
        });
        await loadSearchHistory();
    } catch (e) {
        console.warn('Failed to clear search history', e);
    }
}

// ---- Search Execution ----

async function executeSearch(page = 0) {
    currentPage = page;
    const container = document.getElementById('searchResultsContainer');
    const statsEl = document.getElementById('searchResultStats');
    const emptyEl = document.getElementById('emptySearchState');

    container.innerHTML = `
        <div class="col-12 text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="text-muted small mt-2">Searching academic repository...</p>
        </div>
    `;

    const query = document.getElementById('searchInput')?.value.trim() || '';
    const dept = document.getElementById('filterDept')?.value;
    const sem = document.getElementById('filterSemester')?.value;
    const subj = document.getElementById('filterSubject')?.value;
    const unit = document.getElementById('filterUnit')?.value;
    const cat = document.getElementById('filterCategory')?.value;
    const resType = document.getElementById('filterResourceType')?.value;
    const fileType = document.getElementById('filterFileType')?.value;
    const sort = document.getElementById('filterSort')?.value || 'relevance';

    const params = new URLSearchParams({
        page: page,
        size: 12,
        sortBy: sort
    });

    if (query) params.append('query', query);
    if (dept) params.append('departmentId', dept);
    if (sem) params.append('semester', sem);
    if (subj) params.append('subjectId', subj);
    if (unit) params.append('unitId', unit);
    if (cat) params.append('categoryId', cat);
    if (resType) params.append('resourceType', resType);
    if (fileType) params.append('fileType', fileType);

    try {
        const resp = await fetch(`/api/v1/search?${params.toString()}`);
        if (!resp.ok) throw new Error(`Search failed: HTTP ${resp.status}`);

        const json = await resp.json();
        const searchData = json.data;
        const results = searchData.results || [];
        totalPages = searchData.totalPages || 0;

        // Render detected criteria banner
        renderDetectedBanner(searchData.detectedCriteria);

        statsEl.textContent = query ?
            `Found ${searchData.totalResults} result(s) for "${query}"` :
            `Showing ${searchData.totalResults} academic resource(s)`;

        if (results.length === 0) {
            container.innerHTML = '';
            emptyEl.classList.remove('d-none');
        } else {
            emptyEl.classList.add('d-none');
            renderResultsGrid(results);
        }

        renderPagination(searchData);

        // Refresh search history pills if query was searched
        if (query) {
            await loadSearchHistory();
        }
    } catch (e) {
        container.innerHTML = '';
        showAlert(e.message, 'danger');
    }
}

function renderDetectedBanner(detected) {
    const banner = document.getElementById('detectedBanner');
    const badgesContainer = document.getElementById('detectedBadges');
    badgesContainer.innerHTML = '';

    if (!detected || !detected.originalQuery || detected.originalQuery.trim() === '') {
        banner.classList.add('d-none');
        return;
    }

    const badges = [];

    if (detected.extractedSubjectToken) {
        badges.push(`<span class="badge bg-primary">Subject: ${escapeHtml(detected.extractedSubjectToken)}</span>`);
    }
    if (detected.extractedSemester) {
        badges.push(`<span class="badge bg-secondary">Semester ${detected.extractedSemester}</span>`);
    }
    if (detected.extractedUnitNumber) {
        badges.push(`<span class="badge bg-info text-dark">Unit ${detected.extractedUnitNumber}</span>`);
    }
    if (detected.extractedCategory) {
        badges.push(`<span class="badge bg-success">Category: ${escapeHtml(detected.extractedCategory)}</span>`);
    }
    if (detected.departmentScopeApplied) {
        badges.push(`<span class="badge bg-warning text-dark">Scope: My Department</span>`);
    }
    if (detected.normalizedKeywords && detected.normalizedKeywords.trim() !== '') {
        badges.push(`<span class="badge bg-light text-dark border">Keywords: "${escapeHtml(detected.normalizedKeywords)}"</span>`);
    }

    if (badges.length > 0) {
        badgesContainer.innerHTML = badges.join(' ');
        banner.classList.remove('d-none');
    } else {
        banner.classList.add('d-none');
    }
}

function renderResultsGrid(resources) {
    const container = document.getElementById('searchResultsContainer');
    container.innerHTML = '';

    resources.forEach(r => {
        const col = document.createElement('div');
        col.className = 'col-md-6 col-lg-4';

        const fileExt = (r.fileType || 'file').toUpperCase();
        const badgeClass = getFileTypeBadgeClass(r.fileType);

        col.innerHTML = `
            <div class="resource-card shadow-sm p-3">
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
                <div class="small text-muted mt-auto pt-2 border-top d-flex justify-content-between align-items-center">
                    <div>
                        <span>📦 ${escapeHtml(r.fileSizeFormatted)}</span> &bull; 
                        <span>👤 ${escapeHtml(r.uploadedByName || 'Faculty')}</span>
                    </div>
                    <div class="d-flex gap-1">
                        <a href="/resource-detail.html?id=${r.resourceId}" class="btn btn-outline-primary btn-sm py-0 px-2">View</a>
                        <a href="/api/v1/resources/${r.resourceId}/download" class="btn btn-primary btn-sm py-0 px-2">Download</a>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(col);
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

function renderPagination(searchData) {
    const nav = document.getElementById('searchPaginationNav');
    const ul = document.getElementById('searchPaginationList');
    ul.innerHTML = '';

    if (searchData.totalPages <= 1) {
        nav.classList.add('d-none');
        return;
    }
    nav.classList.remove('d-none');

    // Prev
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${searchData.page === 0 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<button class="page-link">&laquo; Prev</button>`;
    prevLi.addEventListener('click', () => { if (searchData.page > 0) executeSearch(currentPage - 1); });
    ul.appendChild(prevLi);

    // Page Numbers
    for (let i = 0; i < searchData.totalPages; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === searchData.page ? 'active' : ''}`;
        li.innerHTML = `<button class="page-link">${i + 1}</button>`;
        const targetPage = i;
        li.addEventListener('click', () => executeSearch(targetPage));
        ul.appendChild(li);
    }

    // Next
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${searchData.page >= searchData.totalPages - 1 ? 'disabled' : ''}`;
    nextLi.innerHTML = `<button class="page-link">Next &raquo;</button>`;
    nextLi.addEventListener('click', () => { if (searchData.page < searchData.totalPages - 1) executeSearch(currentPage + 1); });
    ul.appendChild(nextLi);
}

// ---- Event Listeners ----

function setupEventListeners() {
    document.getElementById('searchForm')?.addEventListener('submit', (e) => {
        e.preventDefault();
        executeSearch(0);
    });

    document.querySelectorAll('.example-chip').forEach(chip => {
        chip.addEventListener('click', (e) => {
            e.preventDefault();
            const q = chip.getAttribute('data-query');
            document.getElementById('searchInput').value = q;
            executeSearch(0);
        });
    });

    document.getElementById('btnClearAllHistory')?.addEventListener('click', clearAllHistory);

    document.getElementById('filterSubject')?.addEventListener('change', (e) => {
        loadUnitsForSubject(e.target.value);
    });

    document.getElementById('btnApplyDrawerFilters')?.addEventListener('click', () => executeSearch(0));
    document.getElementById('btnResetDrawerFilters')?.addEventListener('click', () => {
        document.getElementById('filterDept').value = '';
        document.getElementById('filterSemester').value = '';
        document.getElementById('filterSubject').value = '';
        document.getElementById('filterUnit').value = '';
        document.getElementById('filterCategory').value = '';
        document.getElementById('filterResourceType').value = '';
        document.getElementById('filterFileType').value = '';
        document.getElementById('filterSort').value = 'relevance';
        executeSearch(0);
    });

    document.getElementById('btnCloseDetected')?.addEventListener('click', () => {
        document.getElementById('detectedBanner').classList.add('d-none');
    });

    setupAiChatAssistant();
}

// ---- AI Academic Assistant Client ----

let aiConversationHistory = [];
const aiSessionId = 'sess_' + Math.random().toString(36).substring(2, 11);

function setupAiChatAssistant() {
    const form = document.getElementById('aiChatForm');
    const input = document.getElementById('aiChatInput');

    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = input.value.trim();
        if (!msg) return;
        input.value = '';
        await sendAiChatMessage(msg);
    });

    document.querySelectorAll('.ai-chip').forEach(chip => {
        chip.addEventListener('click', async () => {
            const msg = chip.getAttribute('data-msg');
            if (msg) {
                await sendAiChatMessage(msg);
            }
        });
    });
}

async function sendAiChatMessage(userMessage) {
    const container = document.getElementById('aiChatMessages');
    const sendBtn = document.getElementById('btnAiChatSend');

    // 1. Render User Bubble
    const userBubble = document.createElement('div');
    userBubble.className = 'd-flex justify-content-end mb-3';
    userBubble.innerHTML = `
        <div class="bg-primary text-white p-2 px-3 rounded-3 shadow-sm small" style="max-width: 85%;">
            ${escapeHtml(userMessage)}
        </div>
    `;
    container.appendChild(userBubble);
    container.scrollTop = container.scrollHeight;

    // 2. Render Loading Indicator
    const typingBubble = document.createElement('div');
    typingBubble.className = 'd-flex align-items-start gap-2 mb-3'
    typingBubble.id = 'aiTypingIndicator';
    typingBubble.innerHTML = `
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width: 32px; height: 32px; font-size: 0.85rem;">🤖</div>
        <div class="bg-white p-2 px-3 rounded-3 shadow-sm border small text-muted">
            <div class="spinner-grow spinner-grow-sm text-primary" role="status"></div>
            <span class="ms-1">Thinking...</span>
        </div>
    `;
    container.appendChild(typingBubble);
    container.scrollTop = container.scrollHeight;

    sendBtn.disabled = true;
    const csrf = getCsrfToken();

    try {
        const resp = await fetch('/api/v1/ai/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrf
            },
            body: JSON.stringify({
                message: userMessage,
                conversationHistory: aiConversationHistory,
                sessionId: aiSessionId
            })
        });

        const json = await resp.json();
        typingBubble.remove();

        if (!resp.ok || !json.success) {
            throw new Error(json.message || 'Chat service failed');
        }

        const data = json.data;
        aiConversationHistory.push({ role: 'user', content: userMessage });
        aiConversationHistory.push({ role: 'assistant', content: data.reply });

        // 3. Render Assistant Bubble
        const assistantBubble = document.createElement('div');
        assistantBubble.className = 'd-flex align-items-start gap-2 mb-3';

        let actionButtonHtml = '';
        if (data.actionType === 'SEARCH_RESOURCES') {
            const searchPayload = data.actionData ? (data.actionData.keywords || data.actionData.original_query || userMessage) : userMessage;
            actionButtonHtml = `
                <div class="mt-2">
                    <button class="btn btn-outline-primary btn-sm py-1 px-2 small btn-apply-ai-search" data-query="${escapeHtml(searchPayload)}">
                        <span>🔍 Apply to Search Results</span>
                    </button>
                </div>
            `;
        }

        assistantBubble.innerHTML = `
            <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width: 32px; height: 32px; font-size: 0.85rem;">🤖</div>
            <div class="bg-white p-3 rounded-3 shadow-sm border small text-dark" style="max-width: 85%;">
                <div style="white-space: pre-line;">${escapeHtml(data.reply)}</div>
                ${actionButtonHtml}
                <div class="text-muted text-end mt-1" style="font-size: 0.65rem;">
                    ${data.actionType} &bull; ${data.latencyMs}ms
                </div>
            </div>
        `;

        assistantBubble.querySelector('.btn-apply-ai-search')?.addEventListener('click', (e) => {
            const q = e.currentTarget.getAttribute('data-query');
            document.getElementById('searchInput').value = q;
            executeSearch(0);
            const offcanvasEl = document.getElementById('aiAssistantOffcanvas');
            const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasEl);
            if (offcanvas) offcanvas.hide();
        });

        container.appendChild(assistantBubble);
        container.scrollTop = container.scrollHeight;
    } catch (e) {
        typingBubble.remove();
        const errorBubble = document.createElement('div');
        errorBubble.className = 'd-flex align-items-start gap-2 mb-3';
        errorBubble.innerHTML = `
            <div class="bg-danger text-white rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width: 32px; height: 32px; font-size: 0.85rem;">⚠️</div>
            <div class="bg-white p-2 px-3 rounded-3 shadow-sm border small text-danger" style="max-width: 85%;">
                ${escapeHtml(e.message)}
            </div>
        `;
        container.appendChild(errorBubble);
        container.scrollTop = container.scrollHeight;
    } finally {
        sendBtn.disabled = false;
    }
}

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
