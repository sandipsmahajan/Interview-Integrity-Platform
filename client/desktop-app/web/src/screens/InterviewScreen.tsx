import { useEffect, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { api } from '../lib/api';
import { useAppStore } from '../stores/appStore';
import type { TelemetryPanelEvent } from '../lib/types';

function ConnectionIndicator() {
  const [status, setStatus] = useState<'connected' | 'disconnected' | 'reconnecting'>('connected');

  useEffect(() => {
    const interval = setInterval(() => {
      setStatus((prev) => {
        if (prev === 'connected') return 'reconnecting';
        if (prev === 'reconnecting') return 'connected';
        if (prev === 'disconnected') return 'reconnecting';
        return prev;
      });
    }, 15000);
    return () => clearInterval(interval);
  }, []);

  const colors = {
    connected: 'bg-green-500',
    reconnecting: 'bg-yellow-500',
    disconnected: 'bg-red-500',
  };

  const labels = {
    connected: 'Connected',
    reconnecting: 'Reconnecting',
    disconnected: 'Disconnected',
  };

  return (
    <div className="flex items-center gap-1.5">
      <motion.div
        className={`w-2 h-2 rounded-full ${colors[status]}`}
        animate={status === 'reconnecting' ? { opacity: [1, 0.3, 1] } : {}}
        transition={{ duration: 1.5, repeat: Infinity }}
      />
      <span className="text-[11px] text-slate-400">{labels[status]}</span>
    </div>
  );
}

function InterviewTimer() {
  const { interview, interviewStartTime } = useAppStore();
  const [remaining, setRemaining] = useState<string>('--:--');

  useEffect(() => {
    if (!interview?.endsAt) return;
    const update = () => {
      const now = Date.now();
      const end = new Date(interview.endsAt).getTime();
      const diff = end - now;

      if (diff <= 0) {
        setRemaining('00:00');
        return;
      }

      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      if (hours > 0) {
        setRemaining(
          `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`,
        );
      } else {
        setRemaining(
          `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`,
        );
      }
    };
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [interview?.endsAt, interviewStartTime]);

  return (
    <div className="flex items-center gap-1.5">
      <svg
        width="14"
        height="14"
        viewBox="0 0 14 14"
        fill="none"
        className="text-slate-400"
      >
        <circle
          cx="7"
          cy="7"
          r="5.5"
          stroke="currentColor"
          strokeWidth="1.5"
        />
        <path
          d="M7 4v3.5l2 2"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      <span className="text-sm font-mono font-semibold text-white tabular-nums">
        {remaining}
      </span>
    </div>
  );
}

function StatusIcon({ status }: { status: string }) {
  const colors: Record<string, string> = {
    success: 'text-green-400',
    warning: 'text-yellow-400',
    error: 'text-red-400',
    info: 'text-blue-400',
  };

  const symbols: Record<string, string> = {
    success: '\u2713',
    warning: '!',
    error: '\u2715',
    info: '\u2022',
  };

  return (
    <span
      className={`text-[10px] text-center w-4 flex-shrink-0 ${colors[status] ?? 'text-indigo-400'}`}
    >
      {symbols[status] ?? symbols.info}
    </span>
  );
}

function TelemetryPanel() {
  const {
    telemetryEvents,
    addTelemetryEvent,
  } = useAppStore();

  const [collapsed, setCollapsed] = useState(false);
  const [search, setSearch] = useState('');
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let unlisten: (() => void) | undefined;
    api.onTelemetryEvent((event: TelemetryPanelEvent) => {
      addTelemetryEvent(event);
    }).then((fn) => {
      unlisten = fn;
    });
    return () => {
      unlisten?.();
    };
  }, [addTelemetryEvent]);

  useEffect(() => {
    if (logRef.current && !collapsed) {
      logRef.current.scrollTop = logRef.current.scrollHeight;
    }
  }, [telemetryEvents.length, collapsed]);

  const filtered = search.length > 0
    ? telemetryEvents.filter(
        (e) =>
          e.message.toLowerCase().includes(search.toLowerCase()) ||
          e.kind.toLowerCase().includes(search.toLowerCase()),
      )
    : telemetryEvents;

  return (
    <div
      className="flex-shrink-0 flex flex-col border-t border-[var(--border)] bg-[#0a0e13] transition-[height] duration-[var(--transition)]"
      style={{
        height: collapsed ? 40 : Math.min(220, telemetryEvents.length * 28 + 60),
      }}
    >
      <div className="flex items-center justify-between flex-shrink-0 px-3 py-2">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="bg-transparent border-0 text-slate-400 cursor-pointer text-sm p-0 w-5 flex items-center justify-center"
            aria-expanded={!collapsed}
          >
            {collapsed ? '\u25B8' : '\u25BE'}
          </button>
          <h3 className="text-[13px] font-semibold text-white">
            Telemetry
          </h3>
          <span className="text-[11px] text-slate-500">
            {telemetryEvents.length} events
          </span>
        </div>
        <AnimatePresence>
          {!collapsed && (
            <motion.div
              className="flex items-center gap-2"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
            >
              <input
                type="text"
                placeholder="Filter events..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-[180px] bg-white/[0.05] border border-[var(--border)] rounded-lg text-white text-xs px-2.5 py-1.5 outline-none focus:border-indigo-500"
              />
              <button
                onClick={() =>
                  useAppStore.setState({ telemetryEvents: [] })
                }
                className="bg-transparent border border-[var(--border)] rounded-lg text-slate-400 text-xs cursor-pointer px-2 py-1 transition-colors hover:border-indigo-500"
              >
                Clear
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <AnimatePresence>
        {!collapsed && (
          <motion.div
            ref={logRef}
            className="flex-1 overflow-y-auto px-3 pb-2.5 font-mono text-xs"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
          >
            {filtered.length === 0 && (
              <p className="text-slate-500 text-xs py-2">
                No telemetry events yet.
              </p>
            )}
            {filtered.map((event, i) => (
              <div
                key={`${event.timestamp}-${i}`}
                className="grid grid-cols-[72px_16px_1fr] gap-2.5 items-center py-1.5 border-b border-white/[0.04] animate-[fade-in_200ms_ease]"
              >
                <span className="text-slate-500 text-[11px] tabular-nums">
                  {new Date(event.timestamp).toLocaleTimeString()}
                </span>
                <StatusIcon status={event.status} />
                <span className="text-[#d7dee8] text-xs break-all">
                  {event.message}
                </span>
              </div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export function InterviewScreen() {
  const {
    interview,
    launchContext,
    telemetryEvents,
    setScreen,
    setSessionSummary,
    setInterviewEnded,
  } = useAppStore();

  const [showEndConfirm, setShowEndConfirm] = useState(false);

  async function handleEndSession() {
    try {
      const summary = await api.endSession();
      setSessionSummary(summary);
    } catch {
      // proceed even on failure
    } finally {
      setInterviewEnded(true);
      setScreen('summary');
    }
  }

  const handleMinimize = useCallback(() => {
    try {
      // @ts-expect-error Tauri global
      if (window.__TAURI_INTERNALS__) {
        import('@tauri-apps/api/window').then(({ getCurrentWindow }) => {
          getCurrentWindow().minimize();
        });
      }
    } catch {
      // minimize not available
    }
  }, []);

  const eventCounts = telemetryEvents.reduce(
    (acc, e) => {
      acc[e.status] = (acc[e.status] ?? 0) + 1;
      return acc;
    },
    {} as Record<string, number>,
  );

  return (
    <motion.div
      className="flex flex-col h-screen overflow-hidden"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      {/* Top Bar */}
      <div className="flex items-center justify-between flex-shrink-0 px-4 py-3 bg-[var(--bg-card)] border-b border-[var(--border)]">
        <div className="flex items-center gap-3">
          <svg width="22" height="22" viewBox="0 0 32 32" fill="none">
            <path
              d="M16 2L4 8v8c0 8.4 5.1 16.2 12 18 6.9-1.8 12-9.6 12-18V8L16 2z"
              stroke="#6366f1"
              strokeWidth="2"
              fill="none"
            />
            <path
              d="M11 16l4 4 6-8"
              stroke="#22c55e"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          <div>
            <h2 className="text-sm font-semibold text-white">
              {interview?.companyName ?? 'Interview'}
            </h2>
            <p className="text-[11px] text-slate-400">
              {interview?.jobTitle ?? 'Active Session'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <InterviewTimer />
          <ConnectionIndicator />

          <div className="relative">
            <button
              onClick={handleMinimize}
              className="bg-transparent border border-[var(--border)] rounded-lg text-slate-400 cursor-pointer p-1.5 transition-colors hover:text-white hover:border-slate-500"
              title="Minimize window"
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path
                  d="M2 7h10"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                />
              </svg>
            </button>
          </div>

          <button
            onClick={() => setShowEndConfirm(true)}
            className="flex items-center gap-2 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 font-medium text-sm py-1.5 px-3 cursor-pointer transition-all hover:bg-red-500/20"
          >
            End Interview
          </button>
        </div>
      </div>

      {/* Main Area */}
      <div className="flex-1 min-h-0 overflow-hidden p-3 pb-0">
        <div className="flex flex-col h-full overflow-hidden rounded-t-xl bg-[var(--bg-card)] border border-[var(--border)] border-b-0">
          {/* Browser Toolbar */}
          <div className="flex items-center gap-2 px-3 py-2.5 border-b border-[var(--border)]">
            <div className="w-2 h-2 rounded-full bg-slate-500" />
            <div className="w-2 h-2 rounded-full bg-slate-500" />
            <div className="w-2 h-2 rounded-full bg-slate-500" />
            <div className="flex-1 mx-2 bg-white/[0.04] border border-[var(--border)] rounded-lg text-slate-400 text-xs px-2.5 py-1.5 truncate">
              {interview?.meetingUrl ?? 'No meeting URL configured'}
            </div>
          </div>

          {/* Meeting IFrame */}
          <iframe
            src={interview?.meetingUrl ?? 'about:blank'}
            title="Meeting"
            className="flex-1 w-full border-0 bg-black"
            sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
          />
        </div>
      </div>

      {/* Bottom Status Bar */}
      <div className="flex-shrink-0 flex items-center justify-between px-4 py-1.5 bg-[#0a0e13] border-t border-[var(--border)]">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1.5">
            <motion.div
              className="w-1.5 h-1.5 rounded-full bg-green-500"
              animate={{ opacity: [1, 0.4, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            />
            <span className="text-[11px] text-slate-400">Monitoring Active</span>
          </div>
          <span className="text-[11px] text-slate-500">
            {telemetryEvents.length} events collected
          </span>
          {(eventCounts.error ?? 0) > 0 && (
            <span className="text-[11px] text-red-400">
              {eventCounts.error} violation{(eventCounts.error ?? 0) !== 1 ? 's' : ''}
            </span>
          )}
        </div>
        <div className="flex items-center gap-3">
          {interview && (
            <span className="text-[11px] text-slate-500 font-mono">
              {interview.id}
            </span>
          )}
          {launchContext && (
            <span className="text-[11px] text-slate-600">
              v{launchContext.clientVersion}
            </span>
          )}
        </div>
      </div>

      {/* Telemetry Panel */}
      <TelemetryPanel />

      {/* End Interview Confirmation Modal */}
      <AnimatePresence>
        {showEndConfirm && (
          <motion.div
            className="fixed inset-0 z-50 flex items-center justify-center p-6"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <div
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => setShowEndConfirm(false)}
            />
            <motion.div
              className="relative bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-6 max-w-sm w-full shadow-lg"
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
            >
              <h3 className="text-base font-semibold text-white mb-2">
                End Interview Session?
              </h3>
              <p className="text-sm text-slate-400 mb-6">
                This will stop all monitoring and end your interview. This
                action cannot be undone.
              </p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setShowEndConfirm(false)}
                  className="bg-transparent border border-[var(--border)] rounded-lg text-slate-300 text-sm font-medium py-2 px-4 cursor-pointer transition-colors hover:border-slate-500"
                >
                  Cancel
                </button>
                <button
                  onClick={() => {
                    setShowEndConfirm(false);
                    handleEndSession();
                  }}
                  className="bg-red-600 hover:bg-red-500 text-white font-medium text-sm rounded-lg py-2 px-4 cursor-pointer transition-colors"
                >
                  End Interview
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Monitoring Tooltip */}
      <motion.div
        className="fixed bottom-4 left-1/2 -translate-x-1/2 bg-[var(--bg-card)] border border-[var(--border)] rounded-lg px-3 py-2 shadow-lg z-40"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 2 }}
      >
        <div className="flex items-center gap-2">
          <motion.div
            className="w-1.5 h-1.5 rounded-full bg-green-500"
            animate={{ opacity: [1, 0.4, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          />
          <span className="text-xs text-slate-300">
            Monitoring is active and recording session integrity data
          </span>
        </div>
      </motion.div>
    </motion.div>
  );
}
