const invoke = window.__TAURI__?.core?.invoke;
const listen = window.__TAURI__?.event?.listen;

export const api = {
  available: Boolean(invoke),

  async launchContext() {
    return invoke('get_launch_context', { args: [] });
  },

  async authenticate(email, password) {
    return invoke('authenticate', { email, password });
  },

  async systemChecks() {
    return invoke('get_system_checks');
  },

  async remoteConfig() {
    return invoke('get_remote_config');
  },

  async interview() {
    return invoke('get_interview');
  },

  async acceptConsent(categories) {
    return invoke('accept_consent', { categories });
  },

  async declineConsent() {
    return invoke('decline_consent');
  },

  async startInterview() {
    return invoke('start_interview');
  },

  async endSession() {
    return invoke('end_session');
  },

  async browserPolicy() {
    return invoke('browser_policy');
  },

  async validateNavigation(url) {
    return invoke('validate_navigation', { url });
  },

  async settings() {
    return invoke('get_settings');
  },

  async updateSettings(settings) {
    return invoke('update_settings', { settings });
  },

  async featureFlags() {
    return invoke('get_feature_flags');
  },

  async appInfo() {
    return invoke('get_app_info');
  },

  onTelemetryEvent(callback) {
    if (!listen) return () => {};
    let unlisten = () => {};
    listen('telemetry-panel-event', (event) => {
      callback(event.payload);
    }).then((fn) => {
      unlisten = fn;
    });
    return () => unlisten();
  },
};

export const CONSENT_CATEGORIES = [
  {
    id: 'device',
    title: 'Device Information',
    description:
      'Operating system version, hardware configuration, hostname, and device identifier to verify your interview environment.',
  },
  {
    id: 'display',
    title: 'Display Configuration',
    description:
      'Number of monitors, resolution, primary monitor, and display changes to detect unauthorized screen sharing setups.',
  },
  {
    id: 'audio',
    title: 'Audio Devices',
    description:
      'Available microphones, active microphone selection, and device changes including multiple active inputs where exposed by the OS.',
  },
  {
    id: 'video',
    title: 'Video Devices',
    description: 'Camera availability, active camera selection, and device changes during the interview session.',
  },
  {
    id: 'window_focus',
    title: 'Window Focus Events',
    description:
      'Foreground window changes and browser focus to ensure the interview remains the primary activity.',
  },
  {
    id: 'lifecycle',
    title: 'Application Lifecycle Events',
    description:
      'Application start, close, crash, and window state changes for session integrity and crash recovery.',
  },
  {
    id: 'meeting',
    title: 'Meeting Diagnostics',
    description: 'Meeting connection status, browser status, navigation status, and meeting duration.',
  },
  {
    id: 'network',
    title: 'Network Quality',
    description:
      'Latency, packet loss, connection type, IP changes, and VPN/proxy indicators where determinable.',
  },
  {
    id: 'integrity',
    title: 'Integrity Telemetry',
    description:
      'Authorized integrity signals including CPU, memory, disk usage, and periodic heartbeats for session continuity.',
  },
];
