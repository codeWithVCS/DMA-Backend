const logEl = document.getElementById('log');
const authStatusEl = document.getElementById('auth-status');
const logoutBtn = document.getElementById('logout-btn');
const viewButtons = document.querySelectorAll('[data-view-target]');
const views = document.querySelectorAll('.view');

let authToken = localStorage.getItem('dma_token');
let lastEmail = localStorage.getItem('dma_email');

function setActiveView(targetId) {
    views.forEach(section => {
        const isActive = section.id === targetId;
        section.classList.toggle('active', isActive);
    });
    viewButtons.forEach(btn => {
        const isActive = btn.dataset.viewTarget === targetId;
        btn.classList.toggle('active', isActive);
    });
}

function log(message, data) {
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    entry.innerHTML = `<strong>${new Date().toLocaleTimeString()}:</strong> ${message}`;
    if (data !== undefined) {
        entry.innerHTML += `<pre>${JSON.stringify(data, null, 2)}</pre>`;
    }
    logEl.prepend(entry);
}

function updateAuthStatus() {
    if (authToken) {
        authStatusEl.className = 'pill pill-success';
        authStatusEl.textContent = `Authenticated${lastEmail ? ` as ${lastEmail}` : ''}`;
        logoutBtn.disabled = false;
    } else {
        authStatusEl.className = 'pill pill-warning';
        authStatusEl.textContent = 'Not authenticated';
        logoutBtn.disabled = true;
    }
}

function parseNumber(value) {
    if (value === '' || value === undefined || value === null) return null;
    const n = Number(value);
    return Number.isFinite(n) ? n : null;
}

async function apiFetch(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    const response = await fetch(path, { ...options, headers });
    let payload = null;
    try { payload = await response.json(); } catch (_) {}

    if (!response.ok) {
        const message = payload?.message || payload?.error || response.statusText;
        log(`Error ${response.status}: ${message}`);
        throw new Error(message);
    }

    return payload;
}

function renderTable(containerId, rows) {
    const container = document.getElementById(containerId);
    if (!rows || rows.length === 0) {
        container.innerHTML = '<p class="muted">No data yet.</p>';
        return;
    }

    const headers = Object.keys(rows[0]);
    const table = document.createElement('table');
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    headers.forEach(h => {
        const th = document.createElement('th');
        th.textContent = h;
        headerRow.appendChild(th);
    });
    thead.appendChild(headerRow);

    const tbody = document.createElement('tbody');
    rows.forEach(row => {
        const tr = document.createElement('tr');
        headers.forEach(h => {
            const td = document.createElement('td');
            td.textContent = row[h] ?? '';
            tr.appendChild(td);
        });
        tbody.appendChild(tr);
    });

    table.appendChild(thead);
    table.appendChild(tbody);
    container.innerHTML = '';
    container.appendChild(table);
}

function fillLoanInputs(loanId, nextEmiId) {
    const loanInputs = document.querySelectorAll('input[name="loanId"]');
    loanInputs.forEach(input => input.value = loanId);
    if (nextEmiId) {
        const emiInputs = document.querySelectorAll('input[name="emiId"]');
        emiInputs.forEach(input => input.value = nextEmiId);
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const form = event.target;
    const payload = {
        name: form.name.value,
        email: form.email.value,
        password: form.password.value,
    };
    const data = await apiFetch('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) });
    log('Registered user', data);
    alert('Registration successful! Please log in.');
    form.reset();
}

async function handleLogin(event) {
    event.preventDefault();
    const form = event.target;
    const payload = {
        email: form.email.value,
        password: form.password.value,
    };
    const data = await apiFetch('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) });
    authToken = data.token;
    lastEmail = data.email;
    localStorage.setItem('dma_token', authToken);
    localStorage.setItem('dma_email', lastEmail);
    updateAuthStatus();
    log('Logged in successfully', data);
    setActiveView('summary-view');
    refreshSummary();
}

async function refreshSummary() {
    if (!authToken) {
        log('Please log in to view loan summaries');
        return;
    }
    const rows = await apiFetch('/api/loans/summary');
    renderTable('summary-table', rows);
    rows.forEach(row => {
        // allow clicking to populate IDs
        // defer binding to next tick to ensure render
    });
    const table = document.querySelector('#summary-table table');
    if (table) {
        Array.from(table.querySelectorAll('tbody tr')).forEach((tr, idx) => {
            tr.addEventListener('click', () => {
                const loanId = rows[idx].loanId;
                fillLoanInputs(loanId, rows[idx].nextEmiId);
                log(`Selected loan ${loanId} from summary`);
            });
        });
    }
}

function collectLoanPayload(form) {
    return {
        loanName: form.loanName.value,
        category: form.category.value,
        lender: form.lender.value,
        principal: parseNumber(form.principal.value),
        annualInterestRate: parseNumber(form.annualInterestRate.value),
        tenureMonths: parseNumber(form.tenureMonths.value),
        emiAmount: parseNumber(form.emiAmount?.value),
        startDate: form.startDate.value || null,
        emiStartDate: form.emiStartDate.value || null,
        emiDayOfMonth: parseNumber(form.emiDayOfMonth.value),
        foreclosureAllowed: form.foreclosureAllowed.value === 'true',
        foreclosurePenaltyPercent: parseNumber(form.foreclosurePenaltyPercent.value),
        partPaymentAllowed: form.partPaymentAllowed.value === 'true',
    };
}

async function handleNewLoan(event) {
    event.preventDefault();
    const payload = collectLoanPayload(event.target);
    delete payload.emiAmount;
    const data = await apiFetch('/api/loans/new', { method: 'POST', body: JSON.stringify(payload) });
    log('Created new loan', data);
    refreshSummary();
}

async function handleExistingLoan(event) {
    event.preventDefault();
    const payload = collectLoanPayload(event.target);
    const data = await apiFetch('/api/loans/existing', { method: 'POST', body: JSON.stringify(payload) });
    log('Added existing loan', data);
    refreshSummary();
}

async function handleLoanHealth(event) {
    event.preventDefault();
    const id = event.target.loanId.value;
    const data = await apiFetch(`/api/loans/${id}/health`);
    renderTable('loan-health', [data]);
    log(`Fetched health for loan ${id}`, data);
}

async function handleSchedule(event) {
    event.preventDefault();
    const id = event.target.loanId.value;
    const data = await apiFetch(`/api/loans/${id}/schedule`);
    renderTable('schedule-table', data);
    log(`Loaded schedule for loan ${id}`, data);
}

async function handleHistory(event) {
    event.preventDefault();
    const id = event.target.loanId.value;
    const data = await apiFetch(`/api/repayment/history/${id}`);
    renderTable('history-table', data);
    log(`Loaded repayment history for loan ${id}`, data);
}

async function handlePayEmi(event) {
    event.preventDefault();
    const { emiId, amountPaid } = event.target;
    const payload = { amountPaid: parseNumber(amountPaid.value) };
    const data = await apiFetch(`/api/repayment/emi/${emiId.value}`, { method: 'POST', body: JSON.stringify(payload) });
    log(`Paid EMI ${emiId.value}`, data);
    refreshSummary();
}

async function handlePartPayment(event) {
    event.preventDefault();
    const { loanId, amountPaid } = event.target;
    const payload = { amountPaid: parseNumber(amountPaid.value) };
    const data = await apiFetch(`/api/repayment/part-payment/${loanId.value}`, { method: 'POST', body: JSON.stringify(payload) });
    log(`Part payment for loan ${loanId.value}`, data);
    refreshSummary();
}

async function handleForeclose(event) {
    event.preventDefault();
    const { loanId, amountPaid } = event.target;
    const payload = { amountPaid: parseNumber(amountPaid.value) };
    const data = await apiFetch(`/api/repayment/foreclose/${loanId.value}`, { method: 'POST', body: JSON.stringify(payload) });
    log(`Foreclosed loan ${loanId.value}`, data);
    refreshSummary();
}

async function handleMarkPaid(event) {
    event.preventDefault();
    const { emiId, actualPaymentDate } = event.target;
    const payload = { actualPaymentDate: actualPaymentDate.value };
    const data = await apiFetch(`/api/emi/${emiId.value}/mark-paid`, { method: 'POST', body: JSON.stringify(payload) });
    log(`Marked EMI ${emiId.value} as paid`, data);
    refreshSummary();
}

async function handleMarkMissed(event) {
    event.preventDefault();
    const { emiId } = event.target;
    const data = await apiFetch(`/api/emi/${emiId.value}/mark-missed`, { method: 'POST' });
    log(`Marked EMI ${emiId.value} as missed`, data);
    refreshSummary();
}

function bindForms() {
    document.getElementById('register-form').addEventListener('submit', wrapHandler(handleRegister));
    document.getElementById('login-form').addEventListener('submit', wrapHandler(handleLogin));
    document.getElementById('new-loan-form').addEventListener('submit', wrapHandler(handleNewLoan));
    document.getElementById('existing-loan-form').addEventListener('submit', wrapHandler(handleExistingLoan));
    document.getElementById('loan-health-form').addEventListener('submit', wrapHandler(handleLoanHealth));
    document.getElementById('schedule-form').addEventListener('submit', wrapHandler(handleSchedule));
    document.getElementById('history-form').addEventListener('submit', wrapHandler(handleHistory));
    document.getElementById('pay-emi-form').addEventListener('submit', wrapHandler(handlePayEmi));
    document.getElementById('part-payment-form').addEventListener('submit', wrapHandler(handlePartPayment));
    document.getElementById('foreclose-form').addEventListener('submit', wrapHandler(handleForeclose));
    document.getElementById('mark-paid-form').addEventListener('submit', wrapHandler(handleMarkPaid));
    document.getElementById('mark-missed-form').addEventListener('submit', wrapHandler(handleMarkMissed));
    document.getElementById('refresh-summary').addEventListener('click', wrapHandler(refreshSummary));
    document.getElementById('clear-log').addEventListener('click', () => logEl.innerHTML = '');
    viewButtons.forEach(btn => btn.addEventListener('click', () => setActiveView(btn.dataset.viewTarget)));
    logoutBtn.addEventListener('click', () => {
        authToken = null;
        lastEmail = null;
        localStorage.removeItem('dma_token');
        localStorage.removeItem('dma_email');
        updateAuthStatus();
        log('Logged out');
        setActiveView('auth-view');
    });
}

function wrapHandler(fn) {
    return async (event) => {
        try {
            await fn(event);
        } catch (err) {
            alert(err.message || 'Something went wrong');
        }
    };
}

(function init() {
    bindForms();
    updateAuthStatus();
    setActiveView(authToken ? 'summary-view' : 'auth-view');
    if (authToken) refreshSummary();
})();
