import Chip from '@mui/material/Chip';
import { titleCase } from '../lib/format';

const STATUS_COLORS: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default' | 'primary' | 'secondary'> = {
  ACTIVE: 'success',
  COMPLETED: 'success',
  PASSED: 'success',
  SENT: 'success',
  DELIVERED: 'success',
  READ: 'success',
  ENABLED: 'success',
  VERIFIED: 'success',
  LIVE: 'success',
  APPROVED: 'success',
  PENDING: 'warning',
  SCHEDULED: 'info',
  REVIEW: 'info',
  OPEN: 'info',
  REQUESTED: 'info',
  DRAFT: 'default',
  DISABLED: 'default',
  INACTIVE: 'default',
  EXPIRED: 'default',
  FAILED: 'error',
  CANCELLED: 'error',
  REVOKED: 'error',
  LOCKED: 'error',
  NO_SHOW: 'error',
  CRITICAL: 'error',
  HIGH: 'error',
  BLOCKED: 'error',
  VIOLATED: 'error',
  URGENT: 'error',
  MEDIUM: 'warning',
  LOW: 'info'
};

export function StatusChip({ status, size = 'small' }: { status: string; size?: 'small' | 'medium' }) {
  const normalized = status?.toUpperCase() ?? '';
  const color = STATUS_COLORS[normalized] ?? 'default';
  return <Chip label={titleCase(normalized)} color={color} size={size} variant="outlined" />;
}
