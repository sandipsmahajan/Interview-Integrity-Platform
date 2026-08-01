import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Add, Policy } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { formatDate, titleCase } from '../lib/format';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import { StatusChip } from '../components/StatusChip';
import type { ColumnDef } from '@tanstack/react-table';
import type { PolicyResponse } from '../lib/types';

const schema = z.object({
  code: z.string().min(1, 'Code is required').max(100),
  name: z.string().min(1, 'Name is required').max(200),
  description: z.string().max(1000).optional().or(z.literal('')),
  defaultSeverity: z.string().min(1, 'Select a severity'),
  priority: z.coerce.number().min(0).max(1000)
});

type FormValues = z.input<typeof schema>;
type FormOutput = z.output<typeof schema>;

const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const POLICY_STATUSES = ['DRAFT', 'ACTIVE', 'ARCHIVED'];

export function PoliciesPage() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const policies = useQuery({ queryKey: ['policies'], queryFn: () => api.listPolicies() });

  const createMutation = useMutation({
    mutationFn: api.createPolicy,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['policies'] });
      toast.success('Policy created');
      setOpen(false);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Creation failed')
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => api.changePolicyStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['policies'] });
      toast.success('Policy status updated');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  const columns = useMemo<ColumnDef<PolicyResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: 'Policy',
        cell: (info) => (
          <Box>
            <Box sx={{ fontWeight: 600 }}>{info.getValue<string>()}</Box>
            <Box sx={{ fontSize: 12, color: 'text.secondary' }}>{info.row.original.code}</Box>
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
            aria-label="Change policy status"
            sx={{ minWidth: 130 }}
          >
            {POLICY_STATUSES.map((status) => (
              <MenuItem key={status} value={status}>
                {titleCase(status)}
              </MenuItem>
            ))}
          </TextField>
        )
      },
      {
        accessorKey: 'defaultSeverity',
        header: 'Default severity',
        cell: (info) => <StatusChip status={info.getValue<string>()} />
      },
      { accessorKey: 'priority', header: 'Priority' },
      {
        accessorKey: 'enabled',
        header: 'Enabled',
        cell: (info) => (info.getValue<boolean>() ? 'Yes' : 'No')
      },
      { accessorKey: 'version', header: 'Version' },
      { accessorKey: 'updatedAt', header: 'Updated', cell: (info) => formatDate(info.getValue<string>()) }
    ],
    [statusMutation]
  );

  return (
    <Box>
      <PageHeader
        title="Policy Engine"
        subtitle="Define and govern integrity policies for your interviews."
        actions={
          <Button variant="contained" startIcon={<Add />} onClick={() => setOpen(true)}>
            New policy
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={policies.data ?? []}
        loading={policies.isLoading}
        searchPlaceholder="Search policies..."
        searchKeys={['name', 'code']}
        emptyTitle="No policies yet"
        emptyDescription="Create a policy to define integrity rules for your interviews."
      />
      <CreatePolicyDialog
        open={open}
        onClose={() => setOpen(false)}
        loading={createMutation.isPending}
        onSubmit={(payload) => createMutation.mutate(payload)}
      />
    </Box>
  );
}

function CreatePolicyDialog({
  open,
  onClose,
  loading,
  onSubmit
}: {
  open: boolean;
  onClose: () => void;
  loading: boolean;
  onSubmit: (payload: import('../lib/types').CreatePolicyRequest) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<FormValues, unknown, FormOutput>({
    resolver: zodResolver(schema),
    defaultValues: { code: '', name: '', description: '', defaultSeverity: 'MEDIUM', priority: 100 }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Policy color="primary" />
        New policy
      </DialogTitle>
      <form
        onSubmit={handleSubmit((values) =>
          onSubmit({
            code: values.code,
            name: values.name,
            description: values.description || null,
            defaultSeverity: values.defaultSeverity,
            priority: values.priority
          })
        )}
        onReset={() => {
          reset();
          onClose();
        }}
      >
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Code"
            fullWidth
            autoFocus
            placeholder="no-virtual-machine"
            {...register('code')}
            error={Boolean(errors.code)}
            helperText={errors.code?.message}
          />
          <TextField
            label="Name"
            fullWidth
            {...register('name')}
            error={Boolean(errors.name)}
            helperText={errors.name?.message}
          />
          <TextField
            label="Description"
            fullWidth
            multiline
            minRows={2}
            {...register('description')}
            error={Boolean(errors.description)}
            helperText={errors.description?.message}
          />
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            <TextField
              select
              label="Default severity"
              {...register('defaultSeverity')}
              error={Boolean(errors.defaultSeverity)}
              helperText={errors.defaultSeverity?.message}
            >
              {SEVERITIES.map((severity) => (
                <MenuItem key={severity} value={severity}>
                  {titleCase(severity)}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Priority"
              type="number"
              slotProps={{ htmlInput: { min: 0, max: 1000 } }}
              {...register('priority')}
              error={Boolean(errors.priority)}
              helperText={errors.priority?.message}
            />
          </Box>
          <Typography variant="caption" color="text.secondary">
            Rules can be attached to a policy after creation via the policy rule API.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button type="reset">Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? 'Creating...' : 'Create policy'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
