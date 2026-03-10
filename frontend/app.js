const API = 'http://localhost:8080/api'; // ← for local dev
let currentUser = null;
let authToken   = null;
let allHistory  = [];

window.addEventListener('DOMContentLoaded', () => {
  const saved = sessionStorage.getItem('hemoscan_token');
  if (saved) { authToken = saved; fetchCurrentUser(); }
});

function switchPanel(which) {
  document.getElementById('login-panel').classList.toggle('hidden', which !== 'login');
  document.getElementById('register-panel').classList.toggle('hidden', which !== 'register');
}

async function handleLogin() {
  const email = v('login-email').trim(), password = v('login-password');
  if (!email || !password) return showAuthError('login', 'Please fill all fields.');
  try {
    const res = await post('/auth/login', { email, password });
    authToken = res.token;
    sessionStorage.setItem('hemoscan_token', authToken);
    await fetchCurrentUser();
  } catch(e) { showAuthError('login', e.message || 'Invalid credentials.'); }
}

async function handleRegister() {
  const firstName = v('reg-firstname').trim(), lastName = v('reg-lastname').trim();
  const email = v('reg-email').trim(), password = v('reg-password'), role = v('reg-role');
  if (!firstName || !lastName || !email || !password) return showAuthError('register', 'Please fill all fields.');
  if (password.length < 8) return showAuthError('register', 'Password must be at least 8 characters.');
  try {
    await post('/auth/register', { firstName, lastName, email, password, role });
    showToast('Account created! Please sign in.', 'success');
    switchPanel('login');
  } catch(e) { showAuthError('register', e.message || 'Registration failed.'); }
}

async function fetchCurrentUser() {
  try {
    currentUser = await get('/users/me');
    showApp();
  } catch(e) { sessionStorage.removeItem('hemoscan_token'); authToken = null; }
}

function handleLogout() {
  authToken = null; currentUser = null; allHistory = [];
  sessionStorage.removeItem('hemoscan_token');
  document.getElementById('app').classList.add('hidden');
  document.getElementById('auth-overlay').classList.remove('hidden');
}

function showApp() {
  document.getElementById('auth-overlay').classList.add('hidden');
  document.getElementById('app').classList.remove('hidden');
  const initial = (currentUser.firstName || 'U')[0].toUpperCase();
  document.getElementById('sidebar-avatar').textContent = initial;
  document.getElementById('profile-avatar').textContent = initial;
  document.getElementById('sidebar-name').textContent = `${currentUser.firstName} ${currentUser.lastName}`;
  document.getElementById('sidebar-role').textContent = currentUser.role || 'User';
  document.getElementById('profile-role-badge').textContent = currentUser.role || 'Patient';
  document.getElementById('prof-firstname').value = currentUser.firstName || '';
  document.getElementById('prof-lastname').value  = currentUser.lastName  || '';
  document.getElementById('prof-email').value     = currentUser.email     || '';
  navigate('dashboard', document.querySelector('[data-view=dashboard]'));
}

function navigate(viewName, el) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById(`view-${viewName}`).classList.add('active');
  if (el) el.classList.add('active');
  if (viewName === 'dashboard') loadDashboard();
  if (viewName === 'history')   loadHistory();
}

async function loadDashboard() {
  try {
    const data = await get('/analysis/stats');
    document.getElementById('stat-total').textContent    = data.total    || 0;
    document.getElementById('stat-positive').textContent = data.positive || 0;
    document.getElementById('stat-negative').textContent = data.negative || 0;
    document.getElementById('stat-last').textContent     = data.lastDate ? new Date(data.lastDate).toLocaleDateString() : '—';
  } catch(e) {}
  try {
    const recent = await get('/analysis/recent?limit=5');
    const list = document.getElementById('recent-list');
    if (!recent.length) { list.innerHTML = '<p class="empty-state">No tests yet. Run your first analysis!</p>'; return; }
    list.innerHTML = recent.map(r => `
      <div class="result-item" onclick="showDetailModal(${JSON.stringify(r).replace(/"/g,'&quot;')})">
        <div class="result-item-left">
          <span class="result-date">${new Date(r.createdAt).toLocaleString()}</span>
          <span class="result-params">Hgb: ${r.hemoglobin} g/dL &nbsp;|&nbsp; MCV: ${r.mcv} fL</span>
        </div>
        <span class="result-badge-sm ${r.anemic ? 'badge-pos' : 'badge-neg'}">${r.anemic ? 'ANEMIC' : 'NORMAL'}</span>
      </div>`).join('');
  } catch(e) {}
}

async function runAnalysis() {
  const payload = {
    hemoglobin: parseFloat(v('p-hgb')) || null, hematocrit: parseFloat(v('p-hct')) || null,
    mcv: parseFloat(v('p-mcv')) || null, mch: parseFloat(v('p-mch')) || null,
    mchc: parseFloat(v('p-mchc')) || null, rbc: parseFloat(v('p-rbc')) || null,
    rdw: parseFloat(v('p-rdw')) || null, serumIron: parseFloat(v('p-iron')) || null,
    ferritin: parseFloat(v('p-ferritin')) || null, transferrinSaturation: parseFloat(v('p-transferrin')) || null,
    age: parseInt(v('p-age')) || null, sex: v('p-sex') || null,
  };
  if (!payload.hemoglobin || !payload.mcv || !payload.rbc) return showAnalyzeError('Hemoglobin, MCV and RBC are required.');
  hideEl('analyze-error'); hideEl('result-output'); showEl('result-waiting');
  try {
    const result = await post('/analysis', payload);
    renderResult(result); loadDashboard();
  } catch(e) { showAnalyzeError(e.message || 'Analysis failed.'); showEl('result-waiting'); }
}

function renderResult(r) {
  hideEl('result-waiting'); showEl('result-output');
  const badge = document.getElementById('result-badge');
  badge.textContent = r.anemic ? '⚠️' : '✅';
  badge.className = `result-badge ${r.anemic ? 'badge-anemic' : 'badge-normal'}`;
  document.getElementById('result-title').textContent = r.anemic ? 'Anemia Detected' : 'No Anemia Detected';
  document.getElementById('result-description').textContent = r.anemic
    ? `AI identified indicators of ${r.anemiaType || 'anemia'}. Please consult a healthcare professional.`
    : 'Your blood parameters appear to be within normal ranges.';
  const metrics = document.getElementById('result-metrics');
  metrics.innerHTML = [
    ['Hemoglobin', r.hemoglobin, 'g/dL',  r.hemoglobin < 12 ? 'low' : 'normal'],
    ['MCV',        r.mcv,        'fL',    r.mcv < 80 ? 'low' : r.mcv > 100 ? 'high' : 'normal'],
    ['RBC',        r.rbc,        '×10⁶',  r.rbc < 4.1 ? 'low' : 'normal'],
    ['Ferritin',   r.ferritin,   'ng/mL', r.ferritin && r.ferritin < 11 ? 'low' : 'normal'],
  ].filter(m => m[1] != null).map(([n,val,unit,cls]) =>
    `<div class="metric-row"><span class="metric-name">${n}</span><span class="metric-val ${cls}">${val} <small>${unit}</small></span></div>`
  ).join('');
  const pct = Math.round((r.confidence || 0.85) * 100);
  document.getElementById('conf-fill').style.width = pct + '%';
  document.getElementById('conf-value').textContent = pct + '%';
  const recs = r.anemic
    ? ['Consult a hematologist or primary care physician','Consider iron, B12 or folate supplementation','Repeat CBC in 4–6 weeks after treatment','Monitor energy levels and pallor symptoms']
    : ['Maintain a balanced iron-rich diet','Schedule annual complete blood count check','Stay hydrated and exercise regularly'];
  document.getElementById('result-recs').innerHTML = `<h4>Recommendations</h4><ul>${recs.map(r => `<li>${r}</li>`).join('')}</ul>`;
}

function clearForm() {
  ['p-hgb','p-hct','p-mcv','p-mch','p-mchc','p-rbc','p-rdw','p-iron','p-ferritin','p-transferrin','p-age'].forEach(id => document.getElementById(id).value = '');
  document.getElementById('p-sex').value = '';
  hideEl('result-output'); showEl('result-waiting');
}

async function loadHistory() {
  try {
    allHistory = await get('/analysis/history');
    renderHistoryTable(allHistory);
  } catch(e) { document.getElementById('history-tbody').innerHTML = '<tr><td colspan="7" class="empty-state">Could not load history.</td></tr>'; }
}

function renderHistoryTable(data) {
  const tbody = document.getElementById('history-tbody');
  if (!data.length) { tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No history yet.</td></tr>'; return; }
  tbody.innerHTML = data.map(r => `
    <tr>
      <td>${new Date(r.createdAt).toLocaleDateString()}</td>
      <td>${r.hemoglobin ?? '—'}</td><td>${r.mcv ?? '—'}</td><td>${r.rbc ?? '—'}</td>
      <td><span class="result-badge-sm ${r.anemic ? 'badge-pos' : 'badge-neg'}">${r.anemic ? 'ANEMIC' : 'NORMAL'}</span></td>
      <td>${Math.round((r.confidence || 0.85) * 100)}%</td>
      <td><button class="btn-detail" onclick='showDetailModal(${JSON.stringify(r)})'>View</button></td>
    </tr>`).join('');
}

function filterHistory() {
  const q = document.getElementById('history-search').value.toLowerCase();
  renderHistoryTable(allHistory.filter(r =>
    (r.anemic ? 'anemic' : 'normal').includes(q) || String(r.hemoglobin).includes(q) || new Date(r.createdAt).toLocaleDateString().includes(q)));
}

async function saveProfile() {
  const firstName = v('prof-firstname').trim(), lastName = v('prof-lastname').trim();
  if (!firstName || !lastName) return showProfileMsg('error', 'Name cannot be empty.');
  try {
    await put('/users/me', { firstName, lastName });
    currentUser.firstName = firstName; currentUser.lastName = lastName;
    document.getElementById('sidebar-name').textContent = `${firstName} ${lastName}`;
    document.getElementById('sidebar-avatar').textContent = firstName[0].toUpperCase();
    document.getElementById('profile-avatar').textContent = firstName[0].toUpperCase();
    showProfileMsg('success', 'Profile updated!');
  } catch(e) { showProfileMsg('error', e.message || 'Update failed.'); }
}

async function changePassword() {
  const current = v('pwd-current'), newPwd = v('pwd-new'), confirm = v('pwd-confirm');
  if (!current || !newPwd || !confirm) return showPwdMsg('error', 'Please fill all fields.');
  if (newPwd !== confirm) return showPwdMsg('error', 'Passwords do not match.');
  if (newPwd.length < 8) return showPwdMsg('error', 'Minimum 8 characters.');
  try {
    await put('/users/me/password', { currentPassword: current, newPassword: newPwd });
    ['pwd-current','pwd-new','pwd-confirm'].forEach(id => document.getElementById(id).value = '');
    showPwdMsg('success', 'Password changed!');
  } catch(e) { showPwdMsg('error', e.message || 'Failed.'); }
}

function showDetailModal(r) {
  const fields = [
    ['Date', new Date(r.createdAt).toLocaleString()], ['Result', r.anemic ? '⚠️ Anemic' : '✅ Normal'],
    ['Confidence', Math.round((r.confidence||0.85)*100)+'%'], ['Type', r.anemiaType || '—'],
    ['Hemoglobin', r.hemoglobin != null ? r.hemoglobin+' g/dL' : '—'], ['Hematocrit', r.hematocrit != null ? r.hematocrit+'%' : '—'],
    ['MCV', r.mcv != null ? r.mcv+' fL' : '—'], ['MCH', r.mch != null ? r.mch+' pg' : '—'],
    ['RBC', r.rbc != null ? r.rbc+' ×10⁶' : '—'], ['Ferritin', r.ferritin != null ? r.ferritin+' ng/mL' : '—'],
  ];
  document.getElementById('modal-content').innerHTML = `<div class="modal-detail-grid">${fields.map(([l,val]) =>
    `<div class="modal-detail-item"><div class="lbl">${l}</div><div class="val">${val}</div></div>`).join('')}</div>`;
  showEl('detail-modal');
}
function closeModal() { hideEl('detail-modal'); }

async function get(path) {
  const res = await fetch(API + path, { headers: authHeaders() });
  if (!res.ok) throw new Error(await errorMsg(res));
  return res.json();
}
async function post(path, body) {
  const res = await fetch(API + path, { method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeaders() }, body: JSON.stringify(body) });
  if (!res.ok) throw new Error(await errorMsg(res));
  return res.json();
}
async function put(path, body) {
  const res = await fetch(API + path, { method: 'PUT', headers: { 'Content-Type': 'application/json', ...authHeaders() }, body: JSON.stringify(body) });
  if (!res.ok) throw new Error(await errorMsg(res));
  return res.json();
}
async function errorMsg(res) {
  try { const j = await res.json(); return j.message || j.error || res.statusText; } catch(e) { return res.statusText; }
}
function authHeaders() { return authToken ? { Authorization: `Bearer ${authToken}` } : {}; }

function v(id) { return document.getElementById(id)?.value ?? ''; }
function showEl(id) { document.getElementById(id)?.classList.remove('hidden'); }
function hideEl(id) { document.getElementById(id)?.classList.add('hidden'); }
function showAuthError(panel, msg) { const el = document.getElementById(`${panel}-error`); el.textContent = msg; el.classList.remove('hidden'); }
function showAnalyzeError(msg) { const el = document.getElementById('analyze-error'); el.textContent = msg; el.classList.remove('hidden'); }
function showProfileMsg(type, msg) { const el = document.getElementById('profile-msg'); el.textContent = msg; el.className = `auth-error`; el.classList.remove('hidden'); setTimeout(() => el.classList.add('hidden'), 3000); }
function showPwdMsg(type, msg) { const el = document.getElementById('pwd-msg'); el.textContent = msg; el.className = `auth-error`; el.classList.remove('hidden'); setTimeout(() => el.classList.add('hidden'), 3000); }
function showToast(msg, type = 'success') { const t = document.getElementById('toast'); t.textContent = msg; t.className = `toast ${type}`; t.classList.remove('hidden'); setTimeout(() => t.classList.add('hidden'), 3000); }