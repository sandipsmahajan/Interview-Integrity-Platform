import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Add, Flag } from '@mui/icons-material';
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
import { formatDate, titleCase } from '../lib/format';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import type { ColumnDef } from '@tanstack/react-table';
import type { FeatureResponse } from '../lib/types';

const FLAG_KINDS = ['BOOLEAN', 'STRING', 'NUMBER', 'JSON'];

const schema = z.object({
  code: z.string().min(1, 'Code is required').max(64),
  name: z.string().min(1, 'Name is required').max(150),
  description: z.string().max(1000).optional().or(z.literal('')),
  kind: z.string().min(1, 'Select a kind')
});

type FormValues = z.infer<typeof schema>;

export function FeatureFlagsPage() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const features = useQuery({ queryKey: ['features'], queryFn: () => api.listFeatures() });

  const createMutation = useMutation({
    mutationFn: api.createFeature,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['features'] });
      toast.success('Feature created');
      setOpen(false);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Creation failed')
  });

  const columns = useMemo<ColumnDef<FeatureResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'name',
        header: 'Feature',
        cell: (info) => (
          <Box>
            <Box sx={{ fontWeight: 600 }}>{info.getValue<string>()}</Box>
            <Box sx={{ fontSize: 12, color: 'text.secondary' }}>{info.row.original.code}</Box>
          </Box>
        )
      },
      {
        accessorKey: 'kind',
        header: 'Kind',
        cell: (info) => <Chip size="small" label={titleCase(info.getValue<string>())} variant="outlined" />
      },
      {
        accessorKey: 'description',
        header: 'Description',
        cell: (info) => info.getValue<string>() || '—'
      },
      { accessorKey: 'createdAt', header: 'Created', cell: (info) => formatDate(info.getValue<string>()) }
    ],
    []
  );

  return (
    <Box>
      <PageHeader
        title="Feature Flags"
        subtitle="Manage the feature catalog powering experiments and capabilities."
        actions={
          <Button variant="contained" startIcon={<Add />} onClick={() => setOpen(true)}>
            New feature
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={features.data ?? []}
        loading={features.isLoading}
        searchPlaceholder="Search features..."
        searchKeys={['name', 'code']}
        emptyTitle="No features yet"
        emptyDescription="Create a feature to manage flags and experiments for your organization."
      />
      <CreateFeatureDialog
        open={open}
        onClose={() => setOpen(false)}
        loading={createMutation.isPending}
        onSubmit={(payload) => createMutation.mutate(payload)}
      />
    </Box>
  );
}

function CreateFeatureDialog({
  open,
  onClose,
  loading,
  onSubmit
}: {
  open: boolean;
  onClose: () => void;
  loading: boolean;
  onSubmit: (payload: { code: string; name: string; description: string | null; kind: string }) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { code: '', name: '', description: '', kind: 'BOOLEAN' }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Flag color="primary" />
        New feature
      </DialogTitle>
      <form
        onSubmit={handleSubmit((values) =>
          onSubmit({ code: values.code, name: values.name, description: values.description || null, kind: values.kind })
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
            placeholder="desktop-client-v2"
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
          <TextField
            select
            label="Kind"
            {...register('kind')}
            error={Boolean(errors.kind)}
            helperText={errors.kind?.message}
          >
            {FLAG_KINDS.map((kind) => (
              <MenuItem key={kind} value={kind}>
                {titleCase(kind)}
              </MenuItem>
            ))}
          </TextField>
          <Typography variant="caption" color="text.secondary">
            Feature flags and experiments are managed through the feature-flag service.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button type="reset">Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? 'Creating...' : 'Create feature'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
