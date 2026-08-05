import { create } from "zustand";
import type {
  AppScreen,
  LaunchContext,
  AuthResponse,
  SystemCheck,
  RemoteConfig,
  InterviewContext,
  SessionSummary,
  ClientSettings,
  FeatureFlags,
  BrowserPolicy,
  TelemetryPanelEvent,
} from "../lib/types";

interface AppState {
  screen: AppScreen;
  error: string | null;
  launchContext: LaunchContext | null;
  authResponse: AuthResponse | null;
  systemChecks: SystemCheck[];
  remoteConfig: RemoteConfig | null;
  interview: InterviewContext | null;
  consentGranted: boolean;
  interviewActive: boolean;
  interviewEnded: boolean;
  sessionSummary: SessionSummary | null;
  settings: ClientSettings;
  featureFlags: FeatureFlags | null;
  browserPolicy: BrowserPolicy | null;
  telemetryEvents: TelemetryPanelEvent[];
  interviewStartTime: number | null;

  setScreen: (screen: AppScreen) => void;
  setError: (error: string | null) => void;
  setLaunchContext: (ctx: LaunchContext) => void;
  setAuthResponse: (auth: AuthResponse) => void;
  setSystemChecks: (checks: SystemCheck[]) => void;
  setRemoteConfig: (config: RemoteConfig) => void;
  setInterview: (interview: InterviewContext) => void;
  setConsentGranted: (granted: boolean) => void;
  setInterviewActive: (active: boolean) => void;
  setInterviewEnded: (ended: boolean) => void;
  setSessionSummary: (summary: SessionSummary) => void;
  setSettings: (settings: ClientSettings) => void;
  setFeatureFlags: (flags: FeatureFlags) => void;
  setBrowserPolicy: (policy: BrowserPolicy) => void;
  addTelemetryEvent: (event: TelemetryPanelEvent) => void;
  setInterviewStartTime: (time: number) => void;
}

export const useAppStore = create<AppState>((set) => ({
  screen: "loading",
  error: null,
  launchContext: null,
  authResponse: null,
  systemChecks: [],
  remoteConfig: null,
  interview: null,
  consentGranted: false,
  interviewActive: false,
  interviewEnded: false,
  sessionSummary: null,
  settings: { theme: "dark", language: "en" },
  featureFlags: null,
  browserPolicy: null,
  telemetryEvents: [],
  interviewStartTime: null,

  setScreen: (screen) => set({ screen }),
  setError: (error) => set({ error }),
  setLaunchContext: (launchContext) => set({ launchContext }),
  setAuthResponse: (authResponse) => set({ authResponse }),
  setSystemChecks: (systemChecks) => set({ systemChecks }),
  setRemoteConfig: (remoteConfig) => set({ remoteConfig }),
  setInterview: (interview) => set({ interview }),
  setConsentGranted: (consentGranted) => set({ consentGranted }),
  setInterviewActive: (interviewActive) => set({ interviewActive }),
  setInterviewEnded: (interviewEnded) => set({ interviewEnded }),
  setSessionSummary: (sessionSummary) => set({ sessionSummary }),
  setSettings: (settings) => set({ settings }),
  setFeatureFlags: (featureFlags) => set({ featureFlags }),
  setBrowserPolicy: (browserPolicy) => set({ browserPolicy }),
  addTelemetryEvent: (event) =>
    set((state) => ({
      telemetryEvents: [...state.telemetryEvents, event],
    })),
  setInterviewStartTime: (interviewStartTime) => set({ interviewStartTime }),
}));
