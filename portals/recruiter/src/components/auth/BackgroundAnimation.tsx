import { useMemo } from 'react';
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
        pointerEvents: 'none',
        willChange: 'transform'
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

interface Particle {
  left: number;
  top: number;
  size: number;
  drift: number;
  duration: number;
  delay: number;
}

const PARTICLES: Particle[] = Array.from({ length: 16 }, (_, index) => ({
  left: (index * 53) % 100,
  top: (index * 37) % 100,
  size: 2 + ((index * 7) % 4),
  drift: 20 + ((index * 13) % 30),
  duration: 6 + ((index * 3) % 6),
  delay: (index * 0.7) % 6
}));

/**
 * Full-screen animated dark gradient (glowing blobs + masked grid + subtle
 * drifting particles) used behind the auth screens. Pure transform/opacity
 * animations for smoothness, honoring reduced-motion preferences.
 */
export function BackgroundAnimation() {
  const reduceMotion = useReducedMotion();
  const particles = useMemo(() => PARTICLES, []);

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
      {!reduceMotion
        ? particles.map((particle, index) => (
            <motion.div
              key={index}
              aria-hidden
              style={{
                position: 'absolute',
                left: `${particle.left}%`,
                top: `${particle.top}%`,
                width: particle.size,
                height: particle.size,
                borderRadius: '50%',
                background: 'rgba(148, 197, 255, 0.55)',
                boxShadow: '0 0 8px 1px rgba(96, 165, 250, 0.35)',
                pointerEvents: 'none',
                willChange: 'transform, opacity'
              }}
              animate={{ y: [0, particle.drift * -1, 0], opacity: [0.2, 0.7, 0.2] }}
              transition={{ duration: particle.duration, delay: particle.delay, repeat: Infinity, ease: 'easeInOut' }}
            />
          ))
        : null}
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
