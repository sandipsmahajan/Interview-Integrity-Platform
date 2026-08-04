import type { CSSProperties } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { motion } from 'framer-motion';
import { useAnimationController } from './AnimationController';

interface IntegrityScoreBubbleProps {
  score?: number;
  label?: string;
  accent?: string;
  style?: CSSProperties;
}

/**
 * Floating integrity score bubble with a glowing animated ring gauge.
 */
export function IntegrityScoreBubble({
  score = 96,
  label = 'Integrity Score',
  accent = '#22c55e',
  style
}: IntegrityScoreBubbleProps) {
  const { enabled } = useAnimationController();
  const size = 74;
  const stroke = 6;
  const r = (size - stroke) / 2;
  const cx = size / 2;
  const cy = size / 2;
  const pct = Math.max(0, Math.min(100, score)) / 100;
  const circumference = 2 * Math.PI * r;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8, y: 8 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ duration: 0.55, delay: 0.15, ease: 'easeOut' }}
      style={style}
    >
      <motion.div
        animate={!enabled ? undefined : { y: [0, -8, 0] }}
        transition={{ duration: 5.2, repeat: Infinity, ease: 'easeInOut', repeatDelay: 1 }}
      >
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            px: 1.25,
            py: 1,
            borderRadius: 2.5,
            bgcolor: 'rgba(15, 23, 42, 0.82)',
            border: `1px solid ${accent}44`,
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            boxShadow: `0 14px 40px -18px rgba(0, 0, 0, 0.8), 0 0 24px -6px ${accent}33`
          }}
        >
          <Box sx={{ position: 'relative', width: size, height: size, flexShrink: 0 }}>
            <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} role="img" aria-label={`${label} ${score}`}>
              <circle
                cx={cx}
                cy={cy}
                r={r}
                fill="none"
                stroke="rgba(148, 163, 184, 0.18)"
                strokeWidth={stroke}
                strokeLinecap="round"
              />
              <motion.circle
                cx={cx}
                cy={cy}
                r={r}
                fill="none"
                stroke={accent}
                strokeWidth={stroke}
                strokeLinecap="round"
                strokeDasharray={circumference}
                initial={enabled ? { strokeDashoffset: circumference } : { strokeDashoffset: circumference * (1 - pct) }}
                animate={{ strokeDashoffset: circumference * (1 - pct) }}
                transition={{ duration: 1.5, ease: 'easeOut', delay: 0.3 }}
                transform={`rotate(-90 ${cx} ${cy})`}
              />
            </svg>
            <Box sx={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
              <Typography sx={{ fontSize: 17, fontWeight: 800, lineHeight: 1, color: accent }}>{score}</Typography>
              <Typography sx={{ fontSize: 8.5, color: 'text.secondary', mt: 0.25 }}>{label}</Typography>
            </Box>
          </Box>
        </Box>
      </motion.div>
    </motion.div>
  );
}
