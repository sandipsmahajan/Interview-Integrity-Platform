export interface LaunchContext {
  interviewId: string;
  linkToken: string;
  deviceId: string;
  clientVersion: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresAt: string;
}

export interface SystemCheck {
  name: string;
  status: CheckStatus;
  message: string;
}

export type CheckStatus = "ok" | "warning" | "error";

export interface RemoteConfig {
  featureFlags: Record<string, boolean>;
  orgName: string;
  supportEmail: string;
  dataRetentionDays: number;
  privacyPolicyUrl: string;
  termsOfServiceUrl: string;
}

export interface InterviewContext {
  id: string;
  companyName: string;
  jobTitle: string;
  startsAt: string;
  endsAt: string;
  meetingUrl: string;
  candidateName: string;
  candidateEmail: string;
  recruiterName: string;
}

export type InterviewStatus =
  | "scheduled"
  | "in_progress"
  | "completed"
  | "cancelled"
  | "expired";

export type TelemetryKind =
  | "Heartbeat"
  | "Device"
  | "Display"
  | "WindowFocus"
  | "Process"
  | "Network"
  | "Audio"
  | "Video"
  | "Browser"
  | "Crash"
  | "Lifecycle"
  | "SystemHealth";

export type PanelStatus = "info" | "success" | "warning" | "error";

export interface TelemetryPanelEvent {
  kind: TelemetryKind;
  status: PanelStatus;
  message: string;
  timestamp: string;
}

export interface BrowserPolicy {
  allowedDomains: string[];
  disableDevtools: boolean;
  disableDownloads: boolean;
  disableExtensions: boolean;
  disablePrinting: boolean;
  blockPopups: boolean;
}

export interface SessionSummary {
  sessionId: string;
  endedAt: string;
  integrityScore: number;
  status: string;
  totalEvents: number;
  violations: number;
}

export type Theme = "dark" | "light";

export interface ClientSettings {
  theme: Theme;
  language: string;
}

export interface FeatureFlags {
  heartbeatEnabled: boolean;
  deviceEnabled: boolean;
  displayEnabled: boolean;
  windowFocusEnabled: boolean;
  processEnabled: boolean;
  networkEnabled: boolean;
  audioEnabled: boolean;
  videoEnabled: boolean;
  browserEnabled: boolean;
  systemHealthEnabled: boolean;
  lifecycleEnabled: boolean;
  policyEngineEnabled: boolean;
  streamingEnabled: boolean;
}

export interface AppInfo {
  productName: string;
  clientVersion: string;
  deviceId: string;
  sessionId: string;
  consentGranted: boolean;
}

export type AppScreen =
  | "loading"
  | "login"
  | "consent"
  | "interview"
  | "summary"
  | "error";

export const CONSENT_CATEGORIES = [
  {
    id: "processes",
    label: "Running Processes",
    description:
      "Monitor applications and processes running during your interview.",
  },
  {
    id: "overlay",
    label: "Overlay Detection",
    description:
      "Detect hidden or transparent overlays that may assist with answers.",
  },
  {
    id: "display",
    label: "Display Configuration",
    description: "Monitor connected monitors and display settings.",
  },
  {
    id: "microphone",
    label: "Microphone Devices",
    description:
      "Detect microphone devices and audio configuration changes.",
  },
  {
    id: "camera",
    label: "Camera Detection",
    description: "Detect cameras including virtual camera software.",
  },
  {
    id: "window_focus",
    label: "Window Focus",
    description:
      "Track which application window has focus during the interview.",
  },
  {
    id: "clipboard",
    label: "Clipboard Monitoring",
    description: "Monitor clipboard usage based on organization policy.",
  },
  {
    id: "network",
    label: "Network Status",
    description:
      "Monitor network connectivity, VPN, and routing changes.",
  },
  {
    id: "telemetry",
    label: "Interview Telemetry",
    description:
      "Collect system health, browser events, and interview timeline data.",
  },
] as const;
