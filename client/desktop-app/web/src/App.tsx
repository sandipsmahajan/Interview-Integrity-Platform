import { useEffect, useRef, useState } from "react";
import { api } from "./lib/api";
import { useAppStore } from "./stores/appStore";
import { CONSENT_CATEGORIES } from "./lib/types";

function LoadingScreen() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
      <div
        className="rounded-[var(--radius-xl)] p-10 max-w-[420px] w-full text-center"
        style={{
          backdropFilter: "blur(18px) saturate(140%)",
          background: "rgba(22, 28, 38, 0.72)",
          border: "1px solid var(--border)",
        }}
      >
        <div className="flex items-center justify-center gap-3 mb-6">
          <div className="flex items-center justify-center rounded-2xl bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white text-[22px] font-extrabold w-14 h-14">
            IP
          </div>
          <h1 className="text-[28px] font-bold tracking-tight text-[var(--text-primary)] m-0">
            Integrity Pro
          </h1>
        </div>
        <div className="mx-auto my-6 w-9 h-9 rounded-full border-[3px] border-[var(--border)] border-t-[var(--accent)] animate-spin" />
        <p className="text-[var(--text-secondary)] text-sm">
          Initializing desktop client...
        </p>
      </div>
    </div>
  );
}

function LoginScreen() {
  const { launchContext, setAuthResponse, setScreen, setError } = useAppStore();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!email || !password) return;
    setSubmitting(true);
    try {
      const auth = await api.authenticate(email, password);
      setAuthResponse(auth);
      setScreen("consent");
    } catch (err) {
      setError(String(err));
      setSubmitting(false);
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
      <div
        className="rounded-[var(--radius-xl)] p-10 max-w-[420px] w-full"
        style={{
          backdropFilter: "blur(18px) saturate(140%)",
          background: "rgba(22, 28, 38, 0.72)",
          border: "1px solid var(--border)",
        }}
      >
        <div className="flex items-center gap-3 mb-8">
          <div className="flex items-center justify-center rounded-2xl bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white text-[22px] font-extrabold w-14 h-14">
            IP
          </div>
          <div>
            <h1 className="text-[22px] font-bold text-[var(--text-primary)] m-0">
              Integrity Pro
            </h1>
            <p className="text-[var(--text-secondary)] text-sm m-0">
              Authenticate to continue
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label
              htmlFor="email"
              className="block text-[11px] font-bold uppercase tracking-widest text-[var(--text-secondary)] mb-1.5"
            >
              Email
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoFocus
              className="w-full bg-[rgba(255,255,255,0.04)] border border-[var(--border)] rounded-[var(--radius-sm)] text-[var(--text-primary)] text-sm px-3 py-2.5 outline-none transition-colors focus:border-[var(--accent)]"
              placeholder="you@company.com"
            />
          </div>
          <div>
            <label
              htmlFor="password"
              className="block text-[11px] font-bold uppercase tracking-widest text-[var(--text-secondary)] mb-1.5"
            >
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full bg-[rgba(255,255,255,0.04)] border border-[var(--border)] rounded-[var(--radius-sm)] text-[var(--text-primary)] text-sm px-3 py-2.5 outline-none transition-colors focus:border-[var(--accent)]"
              placeholder="Enter your password"
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="mt-2 flex items-center justify-center gap-2 bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white font-semibold text-sm py-2.5 px-4 rounded-[var(--radius-sm)] border-0 cursor-pointer transition-all hover:brightness-110 disabled:opacity-45 disabled:cursor-not-allowed"
          >
            {submitting && (
              <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
            )}
            {submitting ? "Authenticating..." : "Sign In"}
          </button>
        </form>

        {launchContext && (
          <p className="text-[var(--text-secondary)] text-xs text-center mt-6 m-0 font-mono">
            v{launchContext.clientVersion}
          </p>
        )}
      </div>
    </div>
  );
}

function ConsentScreen() {
  const {
    launchContext,
    remoteConfig,
    systemChecks,
    interview,
    setConsentGranted,
    setInterview,
    setScreen,
    setInterviewActive,
    setError,
  } = useAppStore();

  const [selectedCategories, setSelectedCategories] = useState<Set<string>>(
    new Set(),
  );
  const [understandChecked, setUnderstandChecked] = useState(false);
  const [participateChecked, setParticipateChecked] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const config = await api.getRemoteConfig();
        useAppStore.getState().setRemoteConfig(config);
        const ctx = await api.getInterview();
        setInterview(ctx);
      } catch {
        // use defaults
      }
      setLoaded(true);
    }
    load();
  }, [setInterview]);

  function toggleCategory(id: string) {
    setSelectedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  async function handleAccept() {
    setSubmitting(true);
    try {
      const categories = Array.from(selectedCategories);
      await api.acceptConsent(categories);
      setConsentGranted(true);

      try {
        const ctx = await api.startInterview();
        setInterview(ctx);
      } catch {
        // continue with existing interview context
      }

      setInterviewActive(true);
      setScreen("interview");
    } catch (err) {
      setError(String(err));
      setSubmitting(false);
    }
  }

  async function handleDecline() {
    try {
      await api.declineConsent();
    } catch {
      // ignore
    }
  }

  if (!loaded) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
        <div className="w-9 h-9 rounded-full border-[3px] border-[var(--border)] border-t-[var(--accent)] animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[var(--bg-primary)] overflow-auto p-6">
      <div className="max-w-[1100px] mx-auto flex flex-col gap-[18px]">
        <div
          className="flex items-center justify-between rounded-[var(--radius-lg)] p-4 px-5"
          style={{
            backdropFilter: "blur(18px) saturate(140%)",
            background: "rgba(22, 28, 38, 0.72)",
            border: "1px solid var(--border)",
          }}
        >
          <div className="flex items-center gap-3.5">
            <div className="flex items-center justify-center rounded-xl bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white text-base font-extrabold w-10 h-10">
              IP
            </div>
            <h2 className="text-lg font-semibold text-[var(--text-primary)] m-0">
              Consent &amp; Permissions
            </h2>
          </div>
          <p className="text-[11px] font-bold uppercase tracking-widest text-[var(--text-secondary)] m-0">
            {remoteConfig?.orgName ?? interview?.companyName ?? "Organization"}
          </p>
        </div>

        <div className="grid gap-4 grid-cols-[2fr_1fr] max-[900px]:grid-cols-1">
          <div
            className="rounded-[var(--radius-lg)] p-5"
            style={{
              backdropFilter: "blur(18px) saturate(140%)",
              background: "rgba(22, 28, 38, 0.72)",
              border: "1px solid var(--border)",
            }}
          >
            <h3 className="text-sm font-semibold text-[var(--text-primary)] m-0 mb-4">
              Monitoring Categories
            </h3>
            <p className="text-[var(--text-secondary)] text-sm leading-relaxed m-0 mb-4">
              Select the categories you consent to monitor during your interview
              session. Descriptions explain what data is collected for each
              category.
            </p>

            <div className="flex flex-col">
              {CONSENT_CATEGORIES.map((category) => (
                <label
                  key={category.id}
                  className="flex items-start gap-3 cursor-pointer py-3.5 border-b border-[var(--border)] last:border-b-0"
                >
                  <input
                    type="checkbox"
                    checked={selectedCategories.has(category.id)}
                    onChange={() => toggleCategory(category.id)}
                    className="mt-0.5 accent-[var(--accent)]"
                  />
                  <div>
                    <strong className="text-sm text-[var(--text-primary)]">
                      {category.label}
                    </strong>
                    <p className="text-[var(--text-secondary)] text-[13px] leading-relaxed m-0 mt-1">
                      {category.description}
                    </p>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <div
              className="rounded-[var(--radius-lg)] p-5"
              style={{
                backdropFilter: "blur(18px) saturate(140%)",
                background: "rgba(22, 28, 38, 0.72)",
                border: "1px solid var(--border)",
              }}
            >
              <h3 className="text-sm font-semibold text-[var(--text-primary)] m-0 mb-3">
                Session Info
              </h3>
              <dl className="flex flex-col gap-2 m-0">
                {[
                  ["Organization", remoteConfig?.orgName ?? "—"],
                  ["Interview", interview?.jobTitle ?? "—"],
                  ["Candidate", interview?.candidateName ?? "—"],
                  ["Support", remoteConfig?.supportEmail ?? "—"],
                  [
                    "Retention",
                    `${remoteConfig?.dataRetentionDays ?? 90} days`,
                  ],
                ].map(([label, value]) => (
                  <div key={label}>
                    <dt className="text-[11px] font-bold uppercase tracking-widest text-[var(--text-secondary)] mb-1">
                      {label}
                    </dt>
                    <dd className="text-[15px] font-semibold text-[var(--text-primary)] m-0">
                      {value}
                    </dd>
                  </div>
                ))}
              </dl>

              <div className="flex flex-col gap-2 mt-4">
                {remoteConfig?.privacyPolicyUrl && (
                  <a
                    href={remoteConfig.privacyPolicyUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-[var(--accent)] text-sm no-underline hover:underline"
                  >
                    Privacy Policy
                  </a>
                )}
                {remoteConfig?.termsOfServiceUrl && (
                  <a
                    href={remoteConfig.termsOfServiceUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-[var(--accent)] text-sm no-underline hover:underline"
                  >
                    Terms of Service
                  </a>
                )}
              </div>
            </div>

            <div
              className="rounded-[var(--radius-lg)] p-5"
              style={{
                backdropFilter: "blur(18px) saturate(140%)",
                background: "rgba(22, 28, 38, 0.72)",
                border: "1px solid var(--border)",
              }}
            >
              <h3 className="text-sm font-semibold text-[var(--text-primary)] m-0 mb-3">
                System Status
              </h3>
              <div className="flex flex-col gap-2">
                {systemChecks.map((check) => (
                  <div
                    key={check.name}
                    className="flex items-center gap-3 bg-[rgba(255,255,255,0.03)] border border-[var(--border)] rounded-[var(--radius-sm)] px-3 py-2.5"
                  >
                    <span
                      className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                      style={{
                        background:
                          check.status === "ok"
                            ? "var(--success)"
                            : check.status === "warning"
                              ? "var(--warning)"
                              : "var(--error)",
                        boxShadow: `0 0 0 4px ${
                          check.status === "ok"
                            ? "rgba(34, 197, 94, 0.14)"
                            : check.status === "warning"
                              ? "rgba(234, 179, 8, 0.14)"
                              : "rgba(239, 68, 68, 0.14)"
                        }`,
                      }}
                    />
                    <div className="flex-1 min-w-0">
                      <span className="text-sm font-semibold text-[var(--text-primary)]">
                        {check.name}
                      </span>
                      {check.message && (
                        <span className="text-xs text-[var(--text-secondary)] ml-2">
                          {check.message}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div
          className="rounded-[var(--radius-lg)] p-5"
          style={{
            backdropFilter: "blur(18px) saturate(140%)",
            background: "rgba(22, 28, 38, 0.72)",
            border: "1px solid var(--border)",
          }}
        >
          <h3 className="text-sm font-semibold text-[var(--text-primary)] m-0 mb-3">
            Confirmation
          </h3>
          <div className="flex flex-col gap-3">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={understandChecked}
                onChange={(e) => setUnderstandChecked(e.target.checked)}
                className="mt-0.5 accent-[var(--accent)]"
              />
              <span className="text-sm text-[var(--text-secondary)]">
                I understand the monitoring categories and consent to
                data collection during my interview session.
              </span>
            </label>
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={participateChecked}
                onChange={(e) => setParticipateChecked(e.target.checked)}
                className="mt-0.5 accent-[var(--accent)]"
              />
              <span className="text-sm text-[var(--text-secondary)]">
                I voluntarily participate in this monitored interview
                session and understand I can withdraw at any time.
              </span>
            </label>
          </div>
        </div>

        <div className="flex justify-end gap-3">
          <button
            onClick={handleDecline}
            className="flex items-center gap-2 bg-transparent border border-[var(--border)] rounded-[var(--radius-sm)] text-[var(--text-primary)] font-semibold text-sm py-2.5 px-4 cursor-pointer transition-all hover:border-[var(--error)] hover:text-[var(--error)]"
          >
            Decline
          </button>
          <button
            onClick={handleAccept}
            disabled={!understandChecked || !participateChecked || submitting}
            className="flex items-center gap-2 bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white border-0 rounded-[var(--radius-sm)] font-semibold text-sm py-2.5 px-4 cursor-pointer transition-all hover:brightness-110 disabled:opacity-45 disabled:cursor-not-allowed"
          >
            {submitting && (
              <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
            )}
            Accept &amp; Start Interview
          </button>
        </div>

        {launchContext && (
          <p className="text-[var(--text-secondary)] text-xs text-center font-mono m-0">
            v{launchContext.clientVersion} &middot;{" "}
            {launchContext.deviceId}
          </p>
        )}
      </div>
    </div>
  );
}

function InterviewScreen() {
  const {
    interview,
    launchContext,
    telemetryEvents,
    addTelemetryEvent,
    setScreen,
    setSessionSummary,
    setInterviewEnded,
  } = useAppStore();

  const [telemetryCollapsed, setTelemetryCollapsed] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let unlisten: (() => void) | undefined;
    api.onTelemetryEvent((event) => {
      addTelemetryEvent(event);
    }).then((fn) => {
      unlisten = fn;
    });
    return () => {
      unlisten?.();
    };
  }, [addTelemetryEvent]);

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight;
    }
  }, [telemetryEvents]);

  async function handleEndSession() {
    try {
      const summary = await api.endSession();
      setSessionSummary(summary);
      setInterviewEnded(true);
      setScreen("summary");
    } catch {
      // proceed to summary even on failure
      setScreen("summary");
    }
  }

  const filteredEvents =
    searchQuery.length > 0
      ? telemetryEvents.filter(
          (e) =>
            e.message.toLowerCase().includes(searchQuery.toLowerCase()) ||
            e.kind.toLowerCase().includes(searchQuery.toLowerCase()),
        )
      : telemetryEvents;

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-[var(--bg-primary)]">
      <div
        className="flex items-center justify-between flex-shrink-0 px-4 py-3 rounded-none"
        style={{
          backdropFilter: "blur(18px) saturate(140%)",
          background: "rgba(22, 28, 38, 0.72)",
          borderBottom: "1px solid var(--border)",
        }}
      >
        <div className="flex items-center gap-3.5">
          <div className="flex items-center justify-center rounded-xl bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white text-base font-extrabold w-10 h-10">
            IP
          </div>
          <h2 className="text-[15px] font-semibold text-[var(--text-primary)] m-0">
            {interview?.companyName ?? "Interview"}
          </h2>
        </div>
        <div className="flex items-center gap-2.5">
          <span className="text-[var(--text-secondary)] text-xs font-mono">
            v{launchContext?.clientVersion ?? "—"}
          </span>
          <button
            onClick={handleEndSession}
            className="flex items-center gap-2 bg-[rgba(239,68,68,0.12)] border border-[rgba(239,68,68,0.35)] rounded-[var(--radius-sm)] text-[var(--error)] font-semibold text-sm py-2 px-3.5 cursor-pointer transition-all hover:bg-[rgba(239,68,68,0.2)]"
          >
            End Session
          </button>
        </div>
      </div>

      <div className="flex-1 min-h-0 overflow-hidden p-3 pb-0">
        <div
          className="flex flex-col h-full overflow-hidden rounded-[var(--radius-md)] rounded-b-none"
          style={{
            backdropFilter: "blur(18px) saturate(140%)",
            background: "rgba(22, 28, 38, 0.72)",
            border: "1px solid var(--border)",
            borderBottom: "none",
          }}
        >
          <div className="flex items-center gap-2 px-3 py-2.5 border-b border-[var(--border)]">
            <div className="w-2 h-2 rounded-full bg-[var(--text-secondary)]" />
            <div className="w-2 h-2 rounded-full bg-[var(--text-secondary)]" />
            <div className="w-2 h-2 rounded-full bg-[var(--text-secondary)]" />
            <div className="flex-1 mx-2 bg-[rgba(255,255,255,0.04)] border border-[var(--border)] rounded-[var(--radius-sm)] text-[var(--text-secondary)] text-xs px-2.5 py-1.5 truncate">
              {interview?.meetingUrl ?? "No meeting URL configured"}
            </div>
          </div>
          <iframe
            src={interview?.meetingUrl ?? "about:blank"}
            title="Meeting"
            className="flex-1 w-full border-0 bg-black"
            sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
          />
        </div>
      </div>

      <div
        className="flex-shrink-0 flex flex-col border-t border-[var(--border)] bg-[#0a0e13] transition-height"
        style={{ height: telemetryCollapsed ? 40 : 220, minHeight: 40 }}
      >
        <div className="flex items-center justify-between flex-shrink-0 px-3 py-2">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setTelemetryCollapsed(!telemetryCollapsed)}
              className="bg-transparent border-0 text-[var(--text-secondary)] cursor-pointer text-sm p-0"
              aria-expanded={!telemetryCollapsed}
            >
              {telemetryCollapsed ? "\u25B8" : "\u25BE"}
            </button>
            <h3 className="text-[13px] font-semibold text-[var(--text-primary)] m-0">
              Telemetry
            </h3>
            <span className="text-[11px] text-[var(--text-secondary)]">
              {telemetryEvents.length} events
            </span>
          </div>
          {!telemetryCollapsed && (
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Filter events..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-[180px] bg-[rgba(255,255,255,0.05)] border border-[var(--border)] rounded-[var(--radius-sm)] text-[var(--text-primary)] text-xs px-2.5 py-1.5 outline-none focus:border-[var(--accent)]"
              />
              <button
                onClick={() =>
                  useAppStore.setState({ telemetryEvents: [] })
                }
                className="bg-transparent border border-[var(--border)] rounded-[var(--radius-sm)] text-[var(--text-secondary)] text-xs cursor-pointer px-2 py-1 transition-colors hover:border-[var(--accent)]"
              >
                Clear
              </button>
            </div>
          )}
        </div>

        {!telemetryCollapsed && (
          <>
            <div className="flex-shrink-0 h-1 bg-transparent cursor-ns-resize hover:bg-[rgba(99,102,241,0.14)]" />
            <div
              ref={logRef}
              className="flex-1 overflow-y-auto px-3 pb-2.5 font-mono text-xs"
            >
              {filteredEvents.length === 0 && (
                <p className="text-[var(--text-secondary)] text-xs py-2 m-0">
                  No telemetry events yet.
                </p>
              )}
              {filteredEvents.map((event, i) => (
                <div
                  key={`${event.timestamp}-${i}`}
                  className="grid grid-cols-[72px_16px_1fr] gap-2.5 items-center py-1.5 border-b border-[rgba(255,255,255,0.04)] animate-[fade-in_200ms_ease]"
                >
                  <span className="text-[var(--text-secondary)] text-[11px]">
                    {new Date(event.timestamp).toLocaleTimeString()}
                  </span>
                  <span
                    className="text-[10px] text-center"
                    style={{
                      color:
                        event.status === "success"
                          ? "var(--success)"
                          : event.status === "warning"
                            ? "var(--warning)"
                            : event.status === "error"
                              ? "var(--error)"
                              : "var(--accent)",
                    }}
                  >
                    {event.status === "success"
                      ? "\u2713"
                      : event.status === "warning"
                        ? "!"
                        : event.status === "error"
                          ? "\u2715"
                          : "\u2022"}
                  </span>
                  <span className="text-[#d7dee8] text-xs break-all">
                    {event.message}
                  </span>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function SummaryScreen() {
  const sessionSummary = useAppStore((s) => s.sessionSummary);
  const { launchContext, telemetryEvents } = useAppStore();

  return (
    <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
      <div
        className="rounded-[var(--radius-xl)] p-10 max-w-[480px] w-full text-center"
        style={{
          backdropFilter: "blur(18px) saturate(140%)",
          background: "rgba(22, 28, 38, 0.72)",
          border: "1px solid var(--border)",
        }}
      >
        <div className="flex items-center justify-center mx-auto mb-4 w-16 h-16 rounded-full bg-[rgba(34,197,94,0.14)] text-[var(--success)] text-[28px]">
          &#10003;
        </div>
        <h1 className="text-[28px] font-bold text-[var(--text-primary)] m-0 mb-2">
          Session Complete
        </h1>
        <p className="text-[var(--text-secondary)] text-sm leading-relaxed m-0 mb-6">
          Your interview session has ended. Below is a summary of the session.
        </p>

        <div className="grid grid-cols-2 gap-3 text-left mb-6">
          {[
            ["Integrity Score", sessionSummary?.integrityScore?.toString() ?? "—"],
            ["Status", sessionSummary?.status ?? "—"],
            ["Total Events", sessionSummary?.totalEvents?.toString() ?? String(telemetryEvents.length)],
            ["Violations", sessionSummary?.violations?.toString() ?? "—"],
          ].map(([label, value]) => (
            <div key={label}>
              <dt className="text-[11px] font-bold uppercase tracking-widest text-[var(--text-secondary)] mb-1">
                {label}
              </dt>
              <dd className="text-[15px] font-semibold text-[var(--text-primary)] m-0">
                {value}
              </dd>
            </div>
          ))}
        </div>

        <button
          onClick={() => window.close()}
          className="flex items-center justify-center gap-2 mx-auto bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white border-0 rounded-[var(--radius-sm)] font-semibold text-sm py-2.5 px-5 cursor-pointer transition-all hover:brightness-110"
        >
          Close Application
        </button>

        {launchContext && (
          <p className="text-[var(--text-secondary)] text-xs font-mono mt-6 m-0">
            v{launchContext.clientVersion}
          </p>
        )}
      </div>
    </div>
  );
}

function ErrorScreen() {
  const error = useAppStore((s) => s.error);
  const setScreen = useAppStore((s) => s.setScreen);
  const launchContext = useAppStore((s) => s.launchContext);

  return (
    <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
      <div
        className="rounded-[var(--radius-xl)] p-10 max-w-[420px] w-full text-center"
        style={{
          backdropFilter: "blur(18px) saturate(140%)",
          background: "rgba(22, 28, 38, 0.72)",
          border: "1px solid var(--border)",
        }}
      >
        <div className="flex items-center justify-center mx-auto mb-4 w-16 h-16 rounded-full bg-[rgba(239,68,68,0.14)] text-[var(--error)] text-[28px]">
          !
        </div>
        <h1 className="text-[22px] font-bold text-[var(--text-primary)] m-0 mb-2">
          Something Went Wrong
        </h1>
        <p className="text-[var(--text-secondary)] text-sm leading-relaxed m-0 mb-6">
          {error ?? "An unexpected error occurred during initialization."}
        </p>
        <div className="flex gap-3 justify-center">
          <button
            onClick={() => {
              useAppStore.getState().setError(null);
              setScreen("loading");
              window.location.reload();
            }}
            className="flex items-center gap-2 bg-gradient-to-br from-[var(--accent)] to-[var(--accent-secondary)] text-white border-0 rounded-[var(--radius-sm)] font-semibold text-sm py-2.5 px-4 cursor-pointer transition-all hover:brightness-110"
          >
            Retry
          </button>
          <button
            onClick={() => window.close()}
            className="flex items-center gap-2 bg-transparent border border-[var(--border)] rounded-[var(--radius-sm)] text-[var(--text-primary)] font-semibold text-sm py-2.5 px-4 cursor-pointer transition-all hover:border-[var(--error)]"
          >
            Close
          </button>
        </div>
        {launchContext && (
          <p className="text-[var(--text-secondary)] text-xs font-mono mt-6 m-0">
            v{launchContext.clientVersion}
          </p>
        )}
      </div>
    </div>
  );
}

function App() {
  const screen = useAppStore((s) => s.screen);
  const { setLaunchContext, setSystemChecks, setSettings, setScreen } =
    useAppStore();

  useEffect(() => {
    async function init() {
      try {
        const ctx = await api.getLaunchContext();
        setLaunchContext(ctx);
        const checks = await api.getSystemChecks();
        setSystemChecks(checks);
        const settings = await api.getSettings();
        setSettings(settings);
        setScreen("login");
      } catch {
        setScreen("error");
      }
    }
    init();
  }, [setLaunchContext, setSystemChecks, setSettings, setScreen]);

  return (
    <div className="min-h-screen bg-[var(--bg-primary)]">
      {screen === "loading" && <LoadingScreen />}
      {screen === "login" && <LoginScreen />}
      {screen === "consent" && <ConsentScreen />}
      {screen === "interview" && <InterviewScreen />}
      {screen === "summary" && <SummaryScreen />}
      {screen === "error" && <ErrorScreen />}
    </div>
  );
}

export default App;
