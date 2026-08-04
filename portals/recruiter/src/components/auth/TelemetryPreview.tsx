import type { ReactNode } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { motion } from 'framer-motion';
import { Cpu, Mic, MonitorUp, Network, Radio, ShieldAlert, MonitorDot } from 'lucide-react';

export interface TelemetryItem {
  icon: ReactNode;
  label: string;
  state: string;
  color: string;
}

const DEFAULT_ITEMS: TelemetryItem[] = [
  { icon: <Cpu size={13} />, label: 'System events', state: 'Normal', color: '#22c55e' },
  { icon: <Mic size={13} />, label: 'Audio devices', state: '1 active', color: '#22c55e' },
  { icon: <MonitorUp size={13} />, label: 'Display changes', state: 'Monitored', color: '#3b82f6' },
  { icon: <MonitorDot size={13} />, label: 'Screen sharing', state: 'Active', color: '#f59e0b' },
  { icon: <Network size={13} />, label: 'Network events', state: 'Watching', color: '#3b82f6' },
  { icon: <ShieldAlert size={13} />, label: 'Policy violations', state: '1 flagged', color: '#ef4444' }
];

interface TelemetryPreviewProps {
  items?: TelemetryItem[];
  title?: string;
}

/**
 * Reusable live telemetry grid used by the showcase slides.
 */
export function TelemetryPreview({ items = DEFAULT_ITEMS, title = 'Interview telemetry' }: TelemetryPreviewProps) {
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{title}</Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontSize: 10.5, color: '#22d3ee' }}>
          <Radio size={11} />
          streaming
        </Box>
      </Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
        {items.map((item, index) => (
          <motion.div
            key={item.label}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 + index * 0.08, duration: 0.4, ease: 'easeOut' }}
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                px: 1.25,
                py: 0.9,
                borderRadius: 1.5,
                bgcolor: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid rgba(148, 163, 184, 0.12)'
              }}
            >
              <Box sx={{ color: item.color, flexShrink: 0, display: 'grid', placeItems: 'center' }}>{item.icon}</Box>
              <Box sx={{ minWidth: 0, flex: 1 }}>
                <Typography
                  sx={{ fontSize: 10.5, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                >
                  {item.label}
                </Typography>
                <Typography sx={{ fontSize: 9.5, color: item.color }}>{item.state}</Typography>
              </Box>
            </Box>
          </motion.div>
        ))}
      </Box>
    </Box>
  );
}
