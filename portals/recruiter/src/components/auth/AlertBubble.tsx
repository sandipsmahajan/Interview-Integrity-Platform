import type { CSSProperties } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { motion } from 'framer-motion';
import { BellRing } from 'lucide-react';
import { useAnimationController } from './AnimationController';

interface AlertBubbleProps {
  title: string;
  message?: string;
  time?: string;
  accent?: string;
  style?: CSSProperties;
}

/**
 * Floating real-time alert bubble styled like a live notification toast.
 */
export function AlertBubble({ title, message, time = 'now', accent = '#ef4444', style }: AlertBubbleProps) {
  const { enabled } = useAnimationController();

  return (
    <motion.div
      initial={{ opacity: 0, x: 16, scale: 0.92 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      transition={{ duration: 0.55, delay: 0.3, ease: 'easeOut' }}
      style={style}
    >
      <motion.div
        animate={!enabled ? undefined : { y: [0, -6, 0] }}
        transition={{ duration: 4.2, repeat: Infinity, ease: 'easeInOut', repeatDelay: 1.4 }}
      >
        <Box
          sx={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: 1,
            minWidth: 208,
            maxWidth: 236,
            p: 1.25,
            borderRadius: 2.5,
            bgcolor: 'rgba(15, 23, 42, 0.9)',
            border: `1px solid ${accent}52`,
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            boxShadow: `0 18px 44px -18px rgba(0, 0, 0, 0.85), 0 0 26px -8px ${accent}44`
          }}
        >
          <Box sx={{ position: 'relative', flexShrink: 0 }}>
            <Box
              sx={{
                width: 28,
                height: 28,
                borderRadius: 8,
                display: 'grid',
                placeItems: 'center',
                bgcolor: `${accent}26`,
                color: accent
              }}
            >
              <BellRing size={14} />
            </Box>
            <Box
              component={motion.span}
              sx={{ position: 'absolute', top: -2, right: -2, width: 8, height: 8, borderRadius: '50%', bgcolor: accent }}
              animate={!enabled ? undefined : { scale: [1, 1.6, 1], opacity: [1, 0.4, 1] }}
              transition={{ duration: 1.6, repeat: Infinity, ease: 'easeInOut' }}
            />
          </Box>
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography sx={{ fontSize: 12, fontWeight: 800, lineHeight: 1.3, color: 'text.primary' }}>{title}</Typography>
            {message ? (
              <Typography sx={{ fontSize: 10.5, lineHeight: 1.4, color: 'text.secondary' }}>{message}</Typography>
            ) : null}
          </Box>
          <Typography sx={{ fontSize: 9.5, color: 'text.secondary', flexShrink: 0, mt: 0.25 }}>{time}</Typography>
        </Box>
      </motion.div>
    </motion.div>
  );
}
