import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { motion, useReducedMotion } from 'framer-motion';
import type { ReactNode } from 'react';

interface DashboardPreviewProps {
  children: ReactNode;
  url?: string;
}

/**
 * Browser-window style mockup used as the product visual inside the showcase slides.
 */
export function DashboardPreview({ children, url = 'app.integrity.pro' }: DashboardPreviewProps) {
  return (
    <Box
      sx={{
        borderRadius: 3,
        border: '1px solid rgba(148, 163, 184, 0.18)',
        overflow: 'hidden',
        bgcolor: 'rgba(13, 19, 34, 0.85)',
        boxShadow: '0 32px 80px -24px rgba(0, 0, 0, 0.7)',
        backdropFilter: 'blur(12px)'
      }}
    >
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          px: 2,
          py: 1.25,
          borderBottom: '1px solid rgba(148, 163, 184, 0.12)',
          bgcolor: 'rgba(255, 255, 255, 0.03)'
        }}
      >
        <Box sx={{ display: 'flex', gap: 0.6 }}>
          {['#f87171', '#fbbf24', '#34d399'].map((color) => (
            <Box key={color} sx={{ width: 9, height: 9, borderRadius: '50%', bgcolor: color }} />
          ))}
        </Box>
        <Box
          sx={{
            flex: 1,
            mx: 2,
            textAlign: 'center',
            py: 0.4,
            px: 1,
            borderRadius: 6,
            bgcolor: 'rgba(255, 255, 255, 0.05)',
            fontSize: 11,
            color: 'text.secondary',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis'
          }}
        >
          {url}
        </Box>
      </Box>
      <Box sx={{ p: 2.25, display: 'flex', flexDirection: 'column', gap: 1.5 }}>{children}</Box>
    </Box>
  );
}

interface IntegrityGaugeProps {
  value?: number;
  size?: number;
}

/**
 * Animated semicircle integrity score gauge.
 */
export function IntegrityGauge({ value = 92, size = 118 }: IntegrityGaugeProps) {
  const reduceMotion = useReducedMotion();
  const stroke = 10;
  const r = (size - stroke) / 2;
  const cx = size / 2;
  const cy = size / 2;
  const pct = Math.max(0, Math.min(100, value)) / 100;
  const arc = `M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`;

  return (
    <Box sx={{ position: 'relative', width: size, height: cy + 10 }}>
      <svg width={size} height={cy + 10} viewBox={`0 0 ${size} ${cy + 10}`} role="img" aria-label={`Integrity score ${value}`}>
        <path d={arc} stroke="rgba(148, 163, 184, 0.18)" strokeWidth={stroke} fill="none" strokeLinecap="round" />
        <motion.path
          d={arc}
          stroke="#22c55e"
          strokeWidth={stroke}
          fill="none"
          strokeLinecap="round"
          initial={reduceMotion ? { pathLength: pct } : { pathLength: 0 }}
          animate={{ pathLength: pct }}
          transition={{ duration: 1.4, ease: 'easeOut', delay: 0.3 }}
        />
      </svg>
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'flex-end',
          pb: 0.5
        }}
      >
        <Typography sx={{ fontSize: 20, fontWeight: 800, color: '#22c55e', lineHeight: 1.1 }}>{value}</Typography>
        <Typography sx={{ fontSize: 9.5, color: 'text.secondary' }}>Integrity</Typography>
      </Box>
    </Box>
  );
}

interface MiniBarsProps {
  values?: number[];
}

/**
 * Animated vertical bar chart used in the analytics slide.
 */
export function MiniBars({ values = [40, 62, 48, 78, 60, 92, 74, 96] }: MiniBarsProps) {
  const reduceMotion = useReducedMotion();
  const max = Math.max(...values);
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-end',
        gap: 0.75,
        height: 68,
        pt: 0.5
      }}
      role="img"
      aria-label="Weekly violation trend chart"
    >
      {values.map((value, index) => {
        const isLast = index === values.length - 1;
        return (
          <motion.div
            key={index}
            style={{
              width: 22,
              borderRadius: 5,
              background: isLast
                ? 'linear-gradient(180deg, #3b82f6, #2563eb)'
                : 'linear-gradient(180deg, rgba(59,130,246,0.45), rgba(37,99,235,0.22))'
            }}
            initial={reduceMotion ? { height: `${(value / max) * 100}%` } : { height: 0 }}
            animate={{ height: `${(value / max) * 100}%` }}
            transition={{ duration: 0.7, delay: 0.2 + index * 0.08, ease: 'easeOut' }}
          />
        );
      })}
    </Box>
  );
}

interface PillProps {
  children: ReactNode;
  color?: string;
  bg?: string;
}

export function Pill({ children, color = '#94a3b8', bg = 'rgba(148, 163, 184, 0.12)' }: PillProps) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.5,
        px: 1,
        py: 0.3,
        borderRadius: 6,
        fontSize: 10.5,
        fontWeight: 600,
        color,
        bgcolor: bg
      }}
    >
      {children}
    </Box>
  );
}
