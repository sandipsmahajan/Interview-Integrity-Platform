import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({ icon, title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        py: 8,
        px: 3,
        textAlign: 'center',
        color: 'text.secondary'
      }}
    >
      {icon ? <Box sx={{ fontSize: 48, mb: 2, opacity: 0.6 }}>{icon}</Box> : null}
      <Typography variant="h6" sx={{ fontWeight: 600, color: 'text.primary' }}>
        {title}
      </Typography>
      {description ? (
        <Typography variant="body2" sx={{ maxWidth: 420, mt: 1 }}>
          {description}
        </Typography>
      ) : null}
      {actionLabel && onAction ? (
        <Button variant="outlined" sx={{ mt: 3 }} onClick={onAction}>
          {actionLabel}
        </Button>
      ) : null}
    </Box>
  );
}
