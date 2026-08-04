import type { CSSProperties, ReactNode } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { motion } from 'framer-motion';
import { useAnimationController } from './AnimationController';

interface FloatingBubbleProps {
  icon: ReactNode;
  label: string;
  sub?: string;
  accent?: string;
  delay?: number;
  style?: CSSProperties;
  pulse?: boolean;
}

/**
 * Small glass status bubble that floats independently around a slide visual.
 * Positioned via `style` (e.g. absolute top/right) inside the slide's visual
 * wrapper so it never collides with the carousel title or description.
 */
export function FloatingBubble({
  icon,
  label,
  sub,
  accent = '#3b82f6',
  delay = 0,
  style,
  pulse = false
}: FloatingBubbleProps) {
  const { enabled } = useAnimationController();

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8, y: 8 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ duration: 0.5, delay, ease: 'easeOut' }}
      style={style}
    >
      <motion.div
        animate={
          !enabled
            ? undefined
            : {
                y: [0, -7, 0]
              }
        }
        transition={{ duration: 4.8, repeat: Infinity, ease: 'easeInOut', repeatDelay: 0.9, delay }}
      >
        <Box
          sx={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 1,
            px: 1.25,
            py: 0.75,
            borderRadius: 999,
            bgcolor: 'rgba(15, 23, 42, 0.82)',
            border: '1px solid rgba(148, 163, 184, 0.22)',
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            boxShadow: '0 14px 36px -18px rgba(0, 0, 0, 0.75)',
            whiteSpace: 'nowrap'
          }}
        >
          <Box
            sx={{
              width: 26,
              height: 26,
              borderRadius: 8,
              display: 'grid',
              placeItems: 'center',
              bgcolor: `${accent}26`,
              color: accent,
              flexShrink: 0
            }}
          >
            {icon}
          </Box>
          <Box sx={{ minWidth: 0 }}>
            <Typography sx={{ fontSize: 11.5, fontWeight: 700, lineHeight: 1.25, color: 'text.primary' }}>
              {label}
            </Typography>
            {sub ? (
              <Typography sx={{ fontSize: 10, lineHeight: 1.3, color: 'text.secondary' }}>{sub}</Typography>
            ) : null}
          </Box>
          {pulse ? (
            <Box
              component={motion.span}
              sx={{ width: 7, height: 7, borderRadius: '50%', bgcolor: accent, flexShrink: 0, ml: 0.25 }}
              animate={!enabled ? undefined : { opacity: [1, 0.3, 1] }}
              transition={{ duration: 1.8, repeat: Infinity, ease: 'easeInOut' }}
            />
          ) : null}
        </Box>
      </motion.div>
    </motion.div>
  );
}
