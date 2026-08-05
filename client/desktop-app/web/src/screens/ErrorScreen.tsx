import { motion } from 'framer-motion';
import { useAppStore } from '../stores/appStore';

export function ErrorScreen() {
  const error = useAppStore((s) => s.error);
  const setScreen = useAppStore((s) => s.setScreen);
  const setError = useAppStore((s) => s.setError);
  const launchContext = useAppStore((s) => s.launchContext);

  function handleRetry() {
    setError(null);
    setScreen('loading');
    window.location.reload();
  }

  return (
    <motion.div
      className="min-h-screen flex items-center justify-center p-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      <motion.div
        className="bg-[var(--bg-card)] border border-[var(--border)] rounded-2xl p-10 max-w-md w-full text-center"
        initial={{ scale: 0.95, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: 'easeOut' }}
      >
        {/* Red X Icon */}
        <motion.div
          className="flex items-center justify-center mx-auto mb-6 w-20 h-20 rounded-full bg-red-500/10"
          initial={{ scale: 0, rotate: -90 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{
            type: 'spring',
            stiffness: 200,
            damping: 14,
            delay: 0.2,
          }}
        >
          <motion.svg
            width="28"
            height="28"
            viewBox="0 0 24 24"
            fill="none"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 0.4, delay: 0.7, ease: 'easeInOut' }}
          >
            <motion.path
              d="M6 6l12 12M18 6L6 18"
              stroke="#ef4444"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </motion.svg>
        </motion.div>

        <h1 className="text-2xl font-bold text-white mb-2">
          Something Went Wrong
        </h1>
        <p className="text-sm text-slate-400 mb-8 leading-relaxed">
          {error ?? 'An unexpected error occurred during initialization. Please try again or contact support.'}
        </p>

        {/* Action Buttons */}
        <div className="flex gap-3 justify-center mb-8">
          <button
            onClick={handleRetry}
            className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg py-2.5 px-5 text-sm transition-colors"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 14 14"
              fill="none"
            >
              <path
                d="M1.5 3.5v3h3M12.5 10.5v-3h-3"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <path
                d="M3.5 8a5 5 0 017.5-1.5L12.5 8M10.5 6a5 5 0 01-7.5 1.5L1.5 6"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            Retry
          </button>
          <button
            onClick={() => window.close()}
            className="bg-transparent border border-[var(--border)] rounded-lg text-slate-300 font-medium text-sm py-2.5 px-5 cursor-pointer transition-colors hover:border-red-500/50 hover:text-red-400"
          >
            Close
          </button>
        </div>

        {/* Support Info */}
        <div className="bg-[var(--bg-primary)] border border-[var(--border)] rounded-xl p-4 text-left">
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-widest mb-3">
            Support Information
          </h3>
          <div className="flex flex-col gap-2">
            {launchContext?.deviceId && (
              <div className="flex justify-between items-center">
                <span className="text-[11px] text-slate-500">Device ID</span>
                <span className="text-xs font-mono text-slate-300">
                  {launchContext.deviceId}
                </span>
              </div>
            )}
            {launchContext?.clientVersion && (
              <div className="flex justify-between items-center">
                <span className="text-[11px] text-slate-500">Client Version</span>
                <span className="text-xs font-mono text-slate-300">
                  v{launchContext.clientVersion}
                </span>
              </div>
            )}
            {launchContext?.interviewId && (
              <div className="flex justify-between items-center">
                <span className="text-[11px] text-slate-500">Interview ID</span>
                <span className="text-xs font-mono text-slate-300 truncate max-w-[180px]">
                  {launchContext.interviewId}
                </span>
              </div>
            )}
            {error && (
              <div className="mt-2 pt-2 border-t border-[var(--border)]">
                <span className="text-[11px] text-slate-500">Error Detail</span>
                <p className="text-xs font-mono text-red-400 mt-1 break-all">
                  {error}
                </p>
              </div>
            )}
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
