import { motion } from 'framer-motion';

export function LoadingScreen() {
  return (
    <motion.div
      className="min-h-screen flex flex-col items-center justify-center gap-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      <div className="flex items-center gap-2">
        <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
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
        <span className="text-2xl font-bold">
          <span className="text-indigo-400">INTEGRITY</span>{' '}
          <span className="text-indigo-300">PRO</span>
        </span>
      </div>

      <div className="flex flex-col items-center gap-4">
        <motion.div
          className="w-8 h-8 border-2 border-indigo-400 border-t-transparent rounded-full"
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
        />
        <p className="text-slate-400 text-sm">
          Preparing your interview environment...
        </p>
        <motion.div className="flex gap-1">
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              className="w-1.5 h-1.5 rounded-full bg-indigo-400"
              animate={{ opacity: [0.3, 1, 0.3] }}
              transition={{
                duration: 1.2,
                repeat: Infinity,
                delay: i * 0.2,
              }}
            />
          ))}
        </motion.div>
      </div>
    </motion.div>
  );
}
