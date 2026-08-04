import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { motion } from 'framer-motion';

export interface TimelineEvent {
  title: string;
  status: string;
  color: string;
}

const DEFAULT_EVENTS: TimelineEvent[] = [
  { title: 'Identity verified', status: 'Passed', color: '#22c55e' },
  { title: 'Invisible copilot flagged', status: 'Violation', color: '#ef4444' },
  { title: 'Screen share confirmed', status: 'Monitored', color: '#3b82f6' },
  { title: 'Session ended', status: 'Evidence captured', color: '#f59e0b' }
];

interface TimelinePreviewProps {
  events?: TimelineEvent[];
  title?: string;
}

/**
 * Reusable animated vertical integrity timeline used by the showcase slides.
 */
export function TimelinePreview({ events = DEFAULT_EVENTS, title = 'Live integrity timeline' }: TimelinePreviewProps) {
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{title}</Typography>
        <Box
          component={motion.span}
          sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, fontSize: 10.5, color: '#22c55e' }}
          animate={{ opacity: [1, 0.4, 1] }}
          transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
        >
          <Box sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: '#22c55e' }} />
          live
        </Box>
      </Box>
      <Box sx={{ position: 'relative', ml: 0.5, mt: 0.75 }}>
        <Box sx={{ position: 'absolute', left: 3, top: 6, bottom: 6, width: 2, bgcolor: 'rgba(148, 163, 184, 0.2)' }} />
        {events.map((event, index) => (
          <motion.div
            key={event.title}
            initial={{ opacity: 0, x: -14 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.25 + index * 0.14, duration: 0.45, ease: 'easeOut' }}
          >
            <Box sx={{ position: 'relative', pl: 3.25, py: 0.8, display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box
                sx={{
                  position: 'absolute',
                  left: 0,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  bgcolor: event.color,
                  boxShadow: `0 0 10px ${event.color}`
                }}
              />
              <Typography sx={{ fontSize: 12, fontWeight: 700, color: 'text.primary', flex: 1 }}>{event.title}</Typography>
              <Typography sx={{ fontSize: 10.5, color: event.color }}>{event.status}</Typography>
            </Box>
          </motion.div>
        ))}
      </Box>
    </Box>
  );
}
