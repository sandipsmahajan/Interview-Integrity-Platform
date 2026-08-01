import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FactCheckOutlined, GppGoodOutlined } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { formatDateTime, titleCase } from '../lib/format';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import { StatusChip } from '../components/StatusChip';
import type { ColumnDef } from '@tanstack/react-table';
import type { ViolationResponse } from '../lib/types';

const REVIEW_ACTIONS = ['ACKNOWLEDGED', 'DISMISSED', 'ESCALATED'];

export function IntegrityPage() {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<ViolationResponse | null>(null);

  const violations = useQuery({ queryKey: ['violations'], queryFn: () => api.listViolations() });

  const reviewMutation = useMutation({
    mutationFn: ({ id, action, comment }: { id: string; action: string; comment?: string }) =>
      api.reviewViolation(id, { action, comment }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['violations'] });
      toast.success('Violation reviewed');
      setSelected(null);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Review failed')
  });

  const columns = useMemo<ColumnDef<ViolationResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'ruleCode',
        header: 'Rule',
        cell: (info) => <Typography sx={{ fontWeight: 600 }}>{titleCase(info.getValue<string>())}</Typography>
      },
      {
        accessorKey: 'severity',
        header: 'Severity',
        cell: (info) => <StatusChip status={info.getValue<string>()} />
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: (info) => <StatusChip status={info.getValue<string>()} />
      },
      {
        accessorKey: 'message',
        header: 'Message',
        cell: (info) => info.getValue<string>() || '—'
      },
      { accessorKey: 'occurredAt', header: 'Occurred', cell: (info) => formatDateTime(info.getValue<string>()) },
      {
        header: 'Actions',
        id: 'actions',
        cell: (info) => (
          <Button size="small" variant="outlined" onClick={() => setSelected(info.row.original)}>
            Review
          </Button>
        )
      }
    ],
    []
  );

  return (
    <Box>
      <PageHeader
        title="Integrity"
        subtitle="Triage policy violations across your interview environment."
      />
      <DataTable
        columns={columns}
        data={violations.data ?? []}
        loading={violations.isLoading}
        searchPlaceholder="Search violations..."
        searchKeys={['ruleCode', 'message']}
        emptyTitle="No violations"
        emptyDescription="No integrity violations have been detected yet."
      />
      <ReviewDialog
        violation={selected}
        loading={reviewMutation.isPending}
        onClose={() => setSelected(null)}
        onSubmit={(action, comment) => reviewMutation.mutate({ id: selected!.id, action, comment })}
      />
    </Box>
  );
}

function ReviewDialog({
  violation,
  loading,
  onClose,
  onSubmit
}: {
  violation: ViolationResponse | null;
  loading: boolean;
  onClose: () => void;
  onSubmit: (action: string, comment: string) => void;
}) {
  const [action, setAction] = useState(REVIEW_ACTIONS[0]);
  const [comment, setComment] = useState('');

  return (
    <Dialog open={Boolean(violation)} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <GppGoodOutlined color="warning" />
        Review violation
      </DialogTitle>
      {violation ? (
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <StatusChip status={violation.severity} />
            <StatusChip status={violation.status} />
            <Chip size="small" label={violation.ruleCode} variant="outlined" />
          </Box>
          <Typography variant="body2">{violation.message}</Typography>
          <Typography variant="caption" color="text.secondary">
            {formatDateTime(violation.occurredAt)} · Session {violation.sessionId.slice(0, 8)}
          </Typography>
          {violation.evidence ? (
            <Box component="pre" sx={{ bgcolor: 'background.default', borderRadius: 1, p: 1.5, fontSize: 12, overflow: 'auto' }}>
              {JSON.stringify(JSON.parse(violation.evidence), null, 2)}
            </Box>
          ) : null}
          <TextField
            select
            label="Decision"
            value={action}
            onChange={(event) => setAction(event.target.value)}
            fullWidth
          >
            {REVIEW_ACTIONS.map((item) => (
              <MenuItem key={item} value={item}>
                {titleCase(item)}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Comment (optional)"
            multiline
            minRows={2}
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            fullWidth
          />
        </DialogContent>
      ) : null}
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          startIcon={<FactCheckOutlined />}
          disabled={loading || !violation}
          onClick={() => onSubmit(action, comment)}
        >
          {loading ? 'Submitting...' : 'Submit decision'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
