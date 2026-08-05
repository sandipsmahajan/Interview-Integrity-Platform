import { invoke } from "@tauri-apps/api/core";
import { listen, type UnlistenFn } from "@tauri-apps/api/event";
import type {
  LaunchContext,
  AuthResponse,
  SystemCheck,
  RemoteConfig,
  InterviewContext,
  SessionSummary,
  BrowserPolicy,
  ClientSettings,
  FeatureFlags,
  AppInfo,
  TelemetryPanelEvent,
} from "./types";

async function getLaunchContext(): Promise<LaunchContext> {
  return invoke<LaunchContext>("getLaunchContext");
}

async function authenticate(
  email: string,
  password: string,
): Promise<AuthResponse> {
  return invoke<AuthResponse>("authenticate", { email, password });
}

async function getSystemChecks(): Promise<SystemCheck[]> {
  return invoke<SystemCheck[]>("getSystemChecks");
}

async function getRemoteConfig(): Promise<RemoteConfig> {
  return invoke<RemoteConfig>("getRemoteConfig");
}

async function getInterview(): Promise<InterviewContext> {
  return invoke<InterviewContext>("getInterview");
}

async function acceptConsent(categories: string[]): Promise<void> {
  return invoke<void>("acceptConsent", { categories });
}

async function declineConsent(): Promise<void> {
  return invoke<void>("declineConsent");
}

async function startInterview(): Promise<InterviewContext> {
  return invoke<InterviewContext>("startInterview");
}

async function endSession(): Promise<SessionSummary> {
  return invoke<SessionSummary>("endSession");
}

async function getBrowserPolicy(): Promise<BrowserPolicy> {
  return invoke<BrowserPolicy>("getBrowserPolicy");
}

async function validateNavigation(url: string): Promise<boolean> {
  return invoke<boolean>("validateNavigation", { url });
}

async function getSettings(): Promise<ClientSettings> {
  return invoke<ClientSettings>("getSettings");
}

async function updateSettings(
  settings: ClientSettings,
): Promise<ClientSettings> {
  return invoke<ClientSettings>("updateSettings", { settings });
}

async function getFeatureFlags(): Promise<FeatureFlags> {
  return invoke<FeatureFlags>("getFeatureFlags");
}

async function getAppInfo(): Promise<AppInfo> {
  return invoke<AppInfo>("getAppInfo");
}

async function onTelemetryEvent(
  callback: (event: TelemetryPanelEvent) => void,
): Promise<UnlistenFn> {
  return listen<TelemetryPanelEvent>("telemetry-panel-event", (event) => {
    callback(event.payload);
  });
}

export const api = {
  getLaunchContext,
  authenticate,
  getSystemChecks,
  getRemoteConfig,
  getInterview,
  acceptConsent,
  declineConsent,
  startInterview,
  endSession,
  getBrowserPolicy,
  validateNavigation,
  getSettings,
  updateSettings,
  getFeatureFlags,
  getAppInfo,
  onTelemetryEvent,
};
