import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Add, PersonAdd } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { formatDate, titleCase } from '../lib/format';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import type { ColumnDef } from '@tanstack/react-table';
import type { CandidateResponse } from '../lib/types';

const schema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(150),
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  phone: z.string().max(50).optional().or(z.literal('')),
  source: z.string().max(120).optional().or(z.literal(''))
});

type FormValues = z.infer<typeof schema>;

const CANDIDATE_STATUSES = ['NEW', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED', 'WITHDRAWN'];

export function CandidatesPage() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const candidates = useQuery({ queryKey: ['candidates'], queryFn: () => api.listCandidates() });

  const createMutation = useMutation({
    mutationFn: api.createCandidate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidates'] });
      toast.success('Candidate created');
      setOpen(false);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Creation failed')
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => api.changeCandidateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidates'] });
      toast.success('Status updated');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  const columns = useMemo<ColumnDef<CandidateResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'fullName',
        header: 'Name',
        cell: (info) => (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Box
              sx={{
                width: 30,
                height: 30,
                borderRadius: '50%',
                bgcolor: 'secondary.main',
                color: 'secondary.contrastText',
                display: 'grid',
                placeItems: 'center',
                fontWeight: 700,
                fontSize: 12
              }}
            >
              {String(info.getValue<string>())
                .split(' ')
                .slice(0, 2)
                .map((part) => part[0]?.toUpperCase() ?? '')
                .join('')}
            </Box>
            <Box>
              <Box sx={{ fontWeight: 600 }}>{info.getValue<string>()}</Box>
              <Box sx={{ fontSize: 12, color: 'text.secondary' }}>{info.row.original.email}</Box>
            </Box>
          </Box>
        )
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: (info) => (
          <TextField
            select
            size="small"
            value={info.getValue<string>()}
            onChange={(event) => statusMutation.mutate({ id: info.row.original.id, status: event.target.value })}
            aria-label="Change candidate status"
            sx={{ minWidth: 140 }}
          >
            {CANDIDATE_STATUSES.map((status) => (
              <MenuItem key={status} value={status}>
                {titleCase(status)}
              </MenuItem>
            ))}
          </TextField>
        )
      },
      { accessorKey: 'phone', header: 'Phone', cell: (info) => info.getValue<string>() || '—' },
      { accessorKey: 'source', header: 'Source', cell: (info) => info.getValue<string>() || '—' },
      { accessorKey: 'createdAt', header: 'Added', cell: (info) => formatDate(info.getValue<string>()) }
    ],
    [statusMutation]
  );

  return (
    <Box>
      <PageHeader
        title="Candidates"
        subtitle="Manage the people moving through your interview pipeline."
        actions={
          <Button variant="contained" startIcon={<Add />} onClick={() => setOpen(true)}>
            Add candidate
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={candidates.data ?? []}
        loading={candidates.isLoading}
        searchPlaceholder="Search candidates..."
        searchKeys={['fullName', 'email']}
        emptyTitle="No candidates yet"
        emptyDescription="Add your first candidate to start scheduling interviews."
      />
      <CreateCandidateDialog
        open={open}
        onClose={() => setOpen(false)}
        loading={createMutation.isPending}
        onSubmit={(payload) => createMutation.mutate(payload)}
      />
    </Box>
  );
}

interface CreateCandidateDialogProps {
  open: boolean;
  onClose: () => void;
  loading: boolean;
  onSubmit: (payload: import('../lib/types').CreateCandidateRequest) => void;
}

function CreateCandidateDialog({ open, onClose, loading, onSubmit }: CreateCandidateDialogProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: '', email: '', phone: '', source: '' }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Add candidate</DialogTitle>
      <form
        onSubmit={handleSubmit((values) => onSubmit({ ...values, phone: values.phone || null, source: values.source || null }))}
        onReset={() => {
          reset();
          onClose();
        }}
      >
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Full name"
            fullWidth
            autoFocus
            {...register('fullName')}
            error={Boolean(errors.fullName)}
            helperText={errors.fullName?.message}
          />
          <TextField
            label="Email"
            type="email"
            fullWidth
            {...register('email')}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
          />
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            <TextField
              label="Phone"
              fullWidth
              {...register('phone')}
              error={Boolean(errors.phone)}
              helperText={errors.phone?.message}
            />
            <TextField
              label="Source"
              fullWidth
              placeholder="Referral, LinkedIn, Careers page..."
              {...register('source')}
              error={Boolean(errors.source)}
              helperText={errors.source?.message}
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button type="reset">Cancel</Button>
          <Button type="submit" variant="contained" startIcon={<PersonAdd />} disabled={loading}>
            {loading ? 'Adding...' : 'Add candidate'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
