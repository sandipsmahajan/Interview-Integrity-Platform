import { motion } from 'framer-motion';
import { useAppStore } from '../stores/appStore';

function IntegrityGauge({ score }: { score: number }) {
  const clamped = Math.max(0, Math.min(100, score));
  const color =
    clamped >= 90
      ? '#22c55e'
      : clamped >= 70
        ? '#eab308'
        : '#ef4444';

  return (
    <div className="flex flex-col items-center gap-2">
      <div className="relative w-20 h-20">
        <svg className="w-full h-full -rotate-90" viewBox="0 0 80 80">
          <circle
            cx="40"
            cy="40"
            r="34"
            fill="none"
            stroke="var(--border)"
            strokeWidth="6"
          />
          <motion.circle
            cx="40"
            cy="40"
            r="34"
            fill="none"
            stroke={color}
            strokeWidth="6"
            strokeLinecap="round"
            strokeDasharray={`${clamped * 2.136} 213.6`}
            initial={{ strokeDashoffset: 0 }}
            animate={{ strokeDashoffset: 0 }}
            transition={{ duration: 1.2, ease: 'easeOut' }}
          />
        </svg>
        <div className="absolute inset-0 flex items-center justify-center">
          <motion.span
            className="text-lg font-bold text-white"
            initial={{ opacity: 0, scale: 0.5 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.6, duration: 0.4 }}
          >
            {clamped}
          </motion.span>
        </div>
      </div>
      <span className="text-[11px] font-bold uppercase tracking-widest text-slate-500">
        Integrity Score
      </span>
    </div>
  );
}

export function SummaryScreen() {
  const {
    sessionSummary,
    launchContext,
    telemetryEvents,
  } = useAppStore();

  const events = sessionSummary?.totalEvents ?? telemetryEvents.length;
  const violations = sessionSummary?.violations ?? 0;
  const score = sessionSummary?.integrityScore ?? 100;

  const stats = [
    {
      label: 'Total Events',
      value: events.toLocaleString(),
      icon: (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path
            d="M2 4h12M2 8h12M2 12h8"
            stroke="#94a3b8"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
        </svg>
      ),
    },
    {
      label: 'Violations',
      value: violations.toLocaleString(),
      icon: (
        <svg
          width="16"
          height="16"
          viewBox="0 0 16 16"
          fill="none"
          className={violations > 0 ? 'text-red-400' : 'text-green-400'}
        >
          <path
            d="M8 1v8M8 13h.01"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
        </svg>
      ),
    },
    {
      label: 'Session ID',
      value: sessionSummary?.sessionId ?? '—',
      icon: (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path
            d="M5 3h8v10H3V6l2-3z"
            stroke="#94a3b8"
            strokeWidth="1.5"
            strokeLinejoin="round"
          />
          <path
            d="M5 3v3H2"
            stroke="#94a3b8"
            strokeWidth="1.5"
            strokeLinejoin="round"
          />
          <path
            d="M6 9h4M6 11h2"
            stroke="#94a3b8"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
        </svg>
      ),
    },
    {
      label: 'Ended At',
      value: sessionSummary?.endedAt
        ? new Date(sessionSummary.endedAt).toLocaleTimeString(undefined, {
            hour: '2-digit',
            minute: '2-digit',
          })
        : '—',
      icon: (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <circle
            cx="8"
            cy="8"
            r="6"
            stroke="#94a3b8"
            strokeWidth="1.5"
          />
          <path
            d="M8 5v3.5l2 1.5"
            stroke="#94a3b8"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      ),
    },
  ];

  return (
    <motion.div
      className="min-h-screen flex items-center justify-center p-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      <motion.div
        className="bg-[var(--bg-card)] border border-[var(--border)] rounded-2xl p-10 max-w-lg w-full text-center"
        initial={{ scale: 0.95, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
      >
        {/* Green Checkmark */}
        <motion.div
          className="flex items-center justify-center mx-auto mb-6 w-20 h-20 rounded-full bg-green-500/10"
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{
            type: 'spring',
            stiffness: 200,
            damping: 14,
            delay: 0.2,
          }}
        >
          <motion.svg
            width="32"
            height="32"
            viewBox="0 0 24 24"
            fill="none"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 0.6, delay: 0.6, ease: 'easeInOut' }}
          >
            <motion.path
              d="M5 13l4 4L19 7"
              stroke="#22c55e"
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </motion.svg>
        </motion.div>

        <h1 className="text-2xl font-bold text-white mb-2">
          Interview Completed Successfully
        </h1>
        <p className="text-sm text-slate-400 mb-8 leading-relaxed">
          Your interview session has ended. Below is a summary of your session
          data.
        </p>

        {/* Integrity Gauge */}
        <div className="flex justify-center mb-8">
          <IntegrityGauge score={score} />
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-2 gap-3 text-left mb-8">
          {stats.map((stat) => (
            <div
              key={stat.label}
              className="bg-[var(--bg-primary)] border border-[var(--border)] rounded-xl p-4"
            >
              <div className="flex items-center gap-2 mb-2">
                {stat.icon}
                <p className="text-[11px] font-bold uppercase tracking-widest text-slate-500">
                  {stat.label}
                </p>
              </div>
              <p className="text-base font-semibold text-white">
                {stat.value}
              </p>
            </div>
          ))}
        </div>

        {/* Close Button */}
        <button
          onClick={() => window.close()}
          className="flex items-center justify-center gap-2 mx-auto bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg py-2.5 px-6 text-sm transition-colors"
        >
          Close Application
        </button>

        {launchContext && (
          <p className="text-xs text-slate-600 font-mono mt-6">
            v{launchContext.clientVersion} &middot; {launchContext.deviceId}
          </p>
        )}
      </motion.div>
    </motion.div>
  );
}
