import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { GroupAdd, PersonAdd } from '@mui/icons-material';
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
import type { RecruiterResponse } from '../lib/types';

const schema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(150),
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  title: z.string().max(120).optional().or(z.literal('')),
  userId: z.string().optional().or(z.literal(''))
});

type FormValues = z.infer<typeof schema>;

const RECRUITER_STATUSES = ['ACTIVE', 'INACTIVE', 'ON_LEAVE'];

export function RecruitersPage() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const recruiters = useQuery({ queryKey: ['recruiters'], queryFn: () => api.listRecruiters() });
  const users = useQuery({ queryKey: ['users'], queryFn: () => api.listUsers(0, 100) });

  const createMutation = useMutation({
    mutationFn: api.createRecruiter,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruiters'] });
      toast.success('Recruiter added');
      setOpen(false);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Creation failed')
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => api.changeRecruiterStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruiters'] });
      toast.success('Status updated');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  const columns = useMemo<ColumnDef<RecruiterResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'fullName',
        header: 'Recruiter',
        cell: (info) => (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Box
              sx={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
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
      { accessorKey: 'title', header: 'Title', cell: (info) => info.getValue<string>() || '—' },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: (info) => (
          <TextField
            select
            size="small"
            value={info.getValue<string>()}
            onChange={(event) => statusMutation.mutate({ id: info.row.original.id, status: event.target.value })}
            aria-label="Change recruiter status"
            sx={{ minWidth: 140 }}
          >
            {RECRUITER_STATUSES.map((status) => (
              <MenuItem key={status} value={status}>
                {titleCase(status)}
              </MenuItem>
            ))}
          </TextField>
        )
      },
      { accessorKey: 'createdAt', header: 'Added', cell: (info) => formatDate(info.getValue<string>()) }
    ],
    [statusMutation]
  );

  return (
    <Box>
      <PageHeader
        title="Recruiters"
        subtitle="Manage your recruiting team and their working status."
        actions={
          <Button variant="contained" startIcon={<GroupAdd />} onClick={() => setOpen(true)}>
            Invite recruiter
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={recruiters.data ?? []}
        loading={recruiters.isLoading}
        searchPlaceholder="Search recruiters..."
        searchKeys={['fullName', 'email']}
        emptyTitle="No recruiters yet"
        emptyDescription="Invite a recruiter to start building your team."
      />
      <CreateRecruiterDialog
        open={open}
        onClose={() => setOpen(false)}
        loading={createMutation.isPending}
        users={(users.data?.items ?? []).filter((user) => user.status === 'ACTIVE')}
        onSubmit={(payload) => createMutation.mutate(payload)}
      />
    </Box>
  );
}

interface CreateRecruiterDialogProps {
  open: boolean;
  onClose: () => void;
  loading: boolean;
  users: { id: string; displayName: string; email: string }[];
  onSubmit: (payload: import('../lib/types').CreateRecruiterRequest) => void;
}

function CreateRecruiterDialog({ open, onClose, loading, users, onSubmit }: CreateRecruiterDialogProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: '', email: '', title: '', userId: '' }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Invite recruiter</DialogTitle>
      <form
        onSubmit={handleSubmit((values) =>
          onSubmit({ fullName: values.fullName, email: values.email, title: values.title || '', userId: values.userId || null })
        )}
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
            label="Work email"
            type="email"
            fullWidth
            {...register('email')}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
          />
          <TextField
            label="Title"
            fullWidth
            placeholder="Senior Recruiter"
            {...register('title')}
            error={Boolean(errors.title)}
            helperText={errors.title?.message}
          />
          <TextField
            select
            label="Linked platform user (optional)"
            fullWidth
            {...register('userId')}
            helperText="Link to an existing user account created in Settings > Users."
          >
            <MenuItem value="">No linked user</MenuItem>
            {users.map((user) => (
              <MenuItem key={user.id} value={user.id}>
                {user.displayName} ({user.email})
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button type="reset">Cancel</Button>
          <Button type="submit" variant="contained" startIcon={<PersonAdd />} disabled={loading}>
            {loading ? 'Inviting...' : 'Invite recruiter'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
