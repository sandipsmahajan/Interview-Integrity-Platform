import { api, CONSENT_CATEGORIES } from './api.js';

const $ = (id) => document.getElementById(id);

let state = {
  launch: null,
  info: null,
  checks: [],
  remoteConfig: null,
  interview: null,
  settings: { theme: 'dark', language: 'en' },
  selectedCategories: new Set(),
  telemetryCount: 0,
};

function show(id) {
  document.querySelectorAll('.screen').forEach((screen) => screen.classList.remove('active'));
  $(id).classList.add('active');
}

function toast(message, type = 'error') {
  const el = $('toast');
  el.textContent = message;
  el.classList.add('visible');
  setTimeout(() => el.classList.remove('visible'), 4000);
}

function setStatus(id, status) {
  const row = $(id);
  if (row) {
    const icon = row.querySelector('.status-icon');
    if (icon) icon.className = `status-icon ${status}`;
  }
}

function renderSystemChecks() {
  const list = $('system-checks');
  list.replaceChildren(
    ...state.checks.map((check) => {
      const row = document.createElement('div');
      row.className = 'check-row';
      row.id = `check-${check.id}`;
      const label = document.createElement('span');
      label.className = 'label';
      label.textContent = check.label;
      const detail = document.createElement('span');
      detail.className = 'detail';
      detail.textContent = check.detail || '';
      const icon = document.createElement('span');
      icon.className = `status-icon ${check.status}`;
      row.append(icon, label, detail);
      return row;
    })
  );
}

function renderSplash() {
  const interview = state.interview;
  const launch = state.launch;
  setText('splash-company', interview?.companyName || '—');
  setText('splash-job', interview?.jobTitle || '—');
  setText('splash-time', interview?.startsAt ? new Date(interview.startsAt).toLocaleString() : '—');
  setText('splash-candidate', interview?.candidateName || '—');
  setText('splash-email', interview?.candidateEmail || '—');
  setText('splash-version', state.info?.clientVersion || '—');
  setText('splash-device', state.info?.deviceId || launch?.deviceId || '—');
}

function setText(id, value) {
  $(id).textContent = value;
}

function renderConsent() {
  const container = $('consent-categories');
  container.replaceChildren(
    ...CONSENT_CATEGORIES.map((category) => {
      const item = document.createElement('div');
      item.className = 'consent-category';
      const label = document.createElement('label');
      label.className = 'checkbox-row';
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.value = category.id;
      checkbox.checked = state.selectedCategories.has(category.id);
      checkbox.addEventListener('change', () => {
        if (checkbox.checked) {
          state.selectedCategories.add(category.id);
        } else {
          state.selectedCategories.delete(category.id);
        }
      });
      const text = document.createElement('span');
      const title = document.createElement('strong');
      title.textContent = category.title;
      const description = document.createElement('p');
      description.textContent = category.description;
      text.append(title, description);
      label.append(checkbox, text);
      item.appendChild(label);
      return item;
    })
  );

  const config = state.remoteConfig;
  if (config) {
    setText('consent-org', config.organizationName || '—');
    setText('consent-support', config.supportEmail || '—');
    setText('consent-retention', `${config.dataRetentionDays ?? 90} days`);
    const privacy = $('consent-privacy');
    const terms = $('consent-terms');
    if (config.privacyNoticeUrl) privacy.href = config.privacyNoticeUrl;
    if (config.termsUrl) terms.href = config.termsUrl;
  }
}

function updateConsentButton() {
  const understand = $('consent-understand').checked;
  const participate = $('consent-participate').checked;
  $('btn-accept-consent').disabled = !(understand && participate);
}

function renderTelemetry(event) {
  const log = $('telemetry-log');
  const entry = document.createElement('div');
  entry.className = 'telemetry-entry';

  const time = document.createElement('span');
  time.className = 'time';
  time.textContent = new Date().toLocaleTimeString();

  const icon = document.createElement('span');
  icon.className = `icon ${event.level || 'info'}`;
  icon.textContent = iconText(event);

  const message = document.createElement('span');
  message.className = 'message';
  message.textContent = event.message || JSON.stringify(event);

  entry.append(time, icon, message);
  log.appendChild(entry);
  log.scrollTop = log.scrollHeight;

  state.telemetryCount += 1;
  setText('telemetry-count', `${state.telemetryCount} events`);
}

function iconText(event) {
  switch (event.level) {
    case 'success':
      return '✓';
    case 'warning':
      return '!';
    case 'error':
      return '✕';
    default:
      return '•';
  }
}

async function boot() {
  show('screen-loading');

  const launch = await api.launchContext();
  state.launch = launch;

  try {
    const info = await api.appInfo();
    state.info = info;
  } catch {
    state.info = null;
  }

  try {
    state.checks = await api.systemChecks();
  } catch (error) {
    state.checks = [];
    toast(`System checks unavailable: ${error}`);
  }
  renderSystemChecks();

  try {
    state.remoteConfig = await api.remoteConfig();
  } catch {
    state.remoteConfig = null;
  }

  try {
    state.interview = await api.interview();
  } catch {
    state.interview = {
      companyName: 'Demo Company',
      jobTitle: 'Engineering Interview',
      startsAt: new Date().toISOString(),
      meetingUrl: 'https://meet.google.com/demo',
      candidateName: 'Demo Candidate',
      candidateEmail: 'demo@example.com',
    };
  }

  try {
    state.settings = await api.settings();
  } catch {
    state.settings = { theme: 'dark', language: 'en' };
  }
  applySettings(state.settings);

  renderSplash();
  show('screen-splash');
}

function applySettings(settings) {
  const theme = settings.theme === 'light' ? 'light' : 'dark';
  document.documentElement.dataset.theme = theme;
  $('setting-theme').value = theme;
  $('setting-language').value = settings.language || 'en';
}

async function onContinue() {
  show('screen-consent');
  renderConsent();
}

async function onAcceptConsent() {
  const btn = $('btn-accept-consent');
  btn.disabled = true;
  try {
    const categories = Array.from(state.selectedCategories);
    await api.acceptConsent(categories);
    setStatus('check-consent', 'pass');
    await startInterview();
  } catch (error) {
    btn.disabled = false;
    toast(`Failed to start: ${error}`);
  }
}

async function onDeclineConsent() {
  try {
    await api.declineConsent();
    show('screen-splash');
    toast('You declined consent. Monitoring cannot begin.', 'warning');
  } catch (error) {
    toast(`Failed: ${error}`);
  }
}

async function startInterview() {
  show('screen-loading');
  try {
    const interview = await api.startInterview();
    state.interview = interview;
    renderInterview(interview);
  } catch (error) {
    renderInterview(state.interview);
    toast(`Monitoring could not start: ${error}`);
  }
}

function renderInterview(interview) {
  show('screen-interview');
  setText('interview-title', interview.companyName || 'Interview');
  setText('meeting-url', interview.meetingUrl || '—');
  const frame = $('meeting-frame');
  if (interview.meetingUrl) {
    frame.src = interview.meetingUrl;
    setText('browser-status', 'Connected');
  }
}

async function onEndSession() {
  try {
    const summary = await api.endSession();
    setText('summary-score', String(summary.integrityScore ?? '—'));
    setText('summary-status', summary.status || '—');
    show('screen-summary');
  } catch (error) {
    toast(`Failed to end session: ${error}`);
  }
}

function onTogglePanel() {
  const panel = $('telemetry-panel');
  const btn = $('btn-toggle-panel');
  const collapsed = panel.classList.toggle('collapsed');
  btn.setAttribute('aria-expanded', String(!collapsed));
  btn.textContent = collapsed ? '▸' : '▾';
}

function onSearchTelemetry() {
  const query = $('telemetry-search').value.toLowerCase();
  document.querySelectorAll('.telemetry-entry').forEach((entry) => {
    const match = entry.textContent.toLowerCase().includes(query);
    entry.classList.toggle('hidden-by-search', !match);
  });
}

function bindEvents() {
  $('btn-continue-splash').addEventListener('click', onContinue);
  $('btn-accept-consent').addEventListener('click', onAcceptConsent);
  $('btn-decline-consent').addEventListener('click', onDeclineConsent);
  $('btn-end-session').addEventListener('click', onEndSession);
  $('btn-close-app').addEventListener('click', () => window.close());

  $('consent-understand').addEventListener('change', updateConsentButton);
  $('consent-participate').addEventListener('change', updateConsentButton);

  $('btn-toggle-panel').addEventListener('click', onTogglePanel);
  $('telemetry-search').addEventListener('input', onSearchTelemetry);
  $('btn-clear-telemetry').addEventListener('click', () => {
    $('telemetry-log').replaceChildren();
    state.telemetryCount = 0;
    setText('telemetry-count', '0 events');
  });

  $('btn-open-settings').addEventListener('click', () => $('settings-modal').showModal());
  $('btn-settings-interview').addEventListener('click', () => $('settings-modal').showModal());
  $('btn-save-settings').addEventListener('click', async (event) => {
    event.preventDefault();
    const settings = {
      theme: $('setting-theme').value,
      language: $('setting-language').value,
    };
    try {
      state.settings = await api.updateSettings(settings);
      applySettings(state.settings);
      $('settings-modal').close();
      toast('Settings saved.', 'success');
    } catch (error) {
      toast(`Failed to save settings: ${error}`);
    }
  });

  ['test-camera', 'test-microphone', 'test-speaker', 'test-network'].forEach((id) => {
    $(id).addEventListener('click', () => {
      setText('test-result', 'Diagnostic initiated. Results will appear in the telemetry panel.');
    });
  });

  const handle = $('telemetry-resize-handle');
  let dragging = false;
  handle.addEventListener('mousedown', () => {
    dragging = true;
    document.body.style.cursor = 'ns-resize';
  });
  window.addEventListener('mousemove', (event) => {
    if (!dragging) return;
    const panel = $('telemetry-panel');
    const height = window.innerHeight - event.clientY;
    panel.style.height = `${Math.min(50 * window.innerHeight / 100, Math.max(120, height))}px`;
    panel.classList.remove('collapsed');
  });
  window.addEventListener('mouseup', () => {
    dragging = false;
    document.body.style.cursor = '';
  });
}

function initConsentCategories() {
  state.selectedCategories = new Set();
  updateConsentButton();
}

window.addEventListener('DOMContentLoaded', () => {
  initConsentCategories();
  bindEvents();
  api.onTelemetryEvent(renderTelemetry);
  boot().catch((error) => {
    setText('loading-status', `Initialization failed: ${error}`);
  });
});
