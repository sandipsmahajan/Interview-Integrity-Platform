import { useState } from 'react';
import { motion } from 'framer-motion';
import { api } from '../lib/api';
import { useAppStore } from '../stores/appStore';

export function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { setAuthResponse, setRemoteConfig, setInterview, setScreen } =
    useAppStore();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const auth = await api.authenticate(email, password);
      setAuthResponse(auth);
      const [config, interview] = await Promise.all([
        api.getRemoteConfig(),
        api.getInterview(),
      ]);
      setRemoteConfig(config);
      setInterview(interview);
      setScreen('consent');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <motion.div
      className="min-h-screen flex flex-col items-center justify-center p-6"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
    >
      <div className="w-full max-w-sm">
        <div className="flex items-center justify-center gap-2 mb-8">
          <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
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
          <span className="text-xl font-bold">
            <span className="text-indigo-400">INTEGRITY</span>{' '}
            <span className="text-indigo-300">PRO</span>
          </span>
        </div>

        <div className="bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-6">
          <h1 className="text-lg font-semibold text-white mb-1">Sign In</h1>
          <p className="text-sm text-slate-400 mb-6">
            Enter your interview credentials to continue.
          </p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                Username or Email
              </label>
              <input
                type="text"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-[var(--bg-primary)] border border-[var(--border)] rounded-lg px-3 py-2 text-white text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
                placeholder="candidate@example.com"
                required
                autoFocus
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-[var(--bg-primary)] border border-[var(--border)] rounded-lg px-3 py-2 text-white text-sm placeholder:text-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
                placeholder="Temporary password"
                required
              />
            </div>

            {error && (
              <motion.div
                className="bg-red-500/10 border border-red-500/30 rounded-lg px-3 py-2 text-sm text-red-400"
                initial={{ opacity: 0, y: -4 }}
                animate={{ opacity: 1, y: 0 }}
              >
                {error}
              </motion.div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-medium rounded-lg py-2.5 text-sm transition-colors mt-2"
            >
              {loading ? 'Authenticating...' : 'Sign In'}
            </button>
          </form>
        </div>

        <p className="text-center text-xs text-slate-500 mt-6">
          Your credentials expire when the interview ends.
        </p>
      </div>
    </motion.div>
  );
}
