import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { motion, useReducedMotion } from 'framer-motion';
import type { ReactNode } from 'react';

interface FeatureCardProps {
  icon: ReactNode;
  title: string;
  subtitle?: string;
  stat?: string;
  accent?: string;
  floating?: boolean;
  delay?: number;
}

/**
 * Small glassmorphism card used for floating showcase elements and in-slide chips.
 */
export function FeatureCard({
  icon,
  title,
  subtitle,
  stat,
  accent = '#3b82f6',
  floating = false,
  delay = 0
}: FeatureCardProps) {
  const reduceMotion = useReducedMotion();

  const card = (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 1.5,
        p: 1.5,
        borderRadius: 2.5,
        bgcolor: 'rgba(255, 255, 255, 0.045)',
        border: '1px solid rgba(148, 163, 184, 0.16)',
        backdropFilter: 'blur(8px)',
        minWidth: 224,
        boxShadow: '0 16px 40px -20px rgba(0, 0, 0, 0.65)'
      }}
    >
      <Box
        sx={{
          width: 34,
          height: 34,
          borderRadius: 2,
          display: 'grid',
          placeItems: 'center',
          bgcolor: `${accent}22`,
          color: accent,
          flexShrink: 0
        }}
      >
        {icon}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography sx={{ fontSize: 13, fontWeight: 700, color: 'text.primary' }}>{title}</Typography>
        {subtitle ? (
          <Typography sx={{ fontSize: 11.5, color: 'text.secondary', lineHeight: 1.4 }}>{subtitle}</Typography>
        ) : null}
      </Box>
      {stat ? (
        <Box sx={{ ml: 'auto', flexShrink: 0, fontSize: 14, fontWeight: 800, color: accent }}>{stat}</Box>
      ) : null}
    </Box>
  );

  if (!floating) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay, ease: 'easeOut' }}
      >
        {card}
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay, ease: 'easeOut' }}
    >
      <motion.div
        animate={reduceMotion ? undefined : { y: [0, -8, 0] }}
        transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut', repeatDelay: 1.2 }}
      >
        {card}
      </motion.div>
    </motion.div>
  );
}
