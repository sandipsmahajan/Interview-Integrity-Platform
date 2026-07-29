import type { ReactNode } from 'react';
import { Box, Paper, Typography } from '@interview-integrity/ui';

export interface AppShellProps {
  title: string;
  status?: ReactNode;
  children: ReactNode;
}

export function AppShell({ title, status, children }: AppShellProps) {
  return (
    <Box className="app-shell">
      <Box className="topbar">
        <Typography variant="h5">{title}</Typography>
        {status}
      </Box>
      {children}
    </Box>
  );
}

export interface MetricTileProps {
  icon: ReactNode;
  label: string;
  value: string | number;
}

export function MetricTile({ icon, label, value }: MetricTileProps) {
  return (
    <Paper className="metric tile">
      {icon}
      <Typography>{label}</Typography>
      <strong>{value}</strong>
    </Paper>
  );
}
