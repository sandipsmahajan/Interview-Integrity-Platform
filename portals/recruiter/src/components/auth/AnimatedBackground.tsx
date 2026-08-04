import Box from '@mui/material/Box';
import { motion, useReducedMotion } from 'framer-motion';

interface BlobProps {
  size: number;
  color: string;
  x: number;
  y: number;
  duration: number;
  delay?: number;
}

function Blob({ size, color, x, y, duration, delay = 0 }: BlobProps) {
  const reduceMotion = useReducedMotion();
  return (
    <motion.div
      aria-hidden
      style={{
        position: 'absolute',
        width: size,
        height: size,
        borderRadius: '50%',
        left: 0,
        top: 0,
        x,
        y,
        background: `radial-gradient(circle at center, ${color} 0%, transparent 65%)`,
        filter: 'blur(60px)',
        opacity: 0.5,
        pointerEvents: 'none'
      }}
      animate={
        reduceMotion
          ? undefined
          : {
              x: [x, x + 130, x - 70, x],
              y: [y, y - 90, y + 80, y]
            }
      }
      transition={{ duration, delay, repeat: Infinity, ease: 'easeInOut' }}
    />
  );
}

/**
 * Full-screen animated dark gradient used behind the auth screens.
 */
export function AnimatedBackground() {
  return (
    <Box
      aria-hidden
      sx={{
        position: 'absolute',
        inset: 0,
        overflow: 'hidden',
        bgcolor: '#0b1220',
        zIndex: 0
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          backgroundImage:
            'linear-gradient(rgba(148,163,184,0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,0.05) 1px, transparent 1px)',
          backgroundSize: '52px 52px',
          WebkitMaskImage: 'radial-gradient(ellipse at 30% 40%, black 0%, transparent 75%)',
          maskImage: 'radial-gradient(ellipse at 30% 40%, black 0%, transparent 75%)'
        }}
      />
      <Blob size={560} color="rgba(37, 99, 235, 0.55)" x={-90} y={-140} duration={22} />
      <Blob size={480} color="rgba(14, 165, 233, 0.4)" x={760} y={90} duration={26} delay={2} />
      <Blob size={520} color="rgba(99, 102, 241, 0.35)" x={360} y={460} duration={24} delay={4} />
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          background: 'radial-gradient(1200px 700px at 72% 50%, transparent 30%, rgba(2,6,23,0.55) 100%)'
        }}
      />
    </Box>
  );
}
