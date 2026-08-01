import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Add, Download, PlayArrow } from '@mui/icons-material';
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
import { formatDateTime, titleCase } from '../lib/format';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import { StatusChip } from '../components/StatusChip';
import type { ColumnDef } from '@tanstack/react-table';
import type { ReportResponse } from '../lib/types';

const REPORT_TYPES = ['SESSION', 'CANDIDATE', 'INTERVIEW', 'RECRUITER', 'ORGANIZATION', 'INTEGRITY'];
const REPORT_FORMATS = ['PDF', 'XLSX', 'CSV'];

const schema = z.object({
  type: z.string().min(1, 'Select a type'),
  title: z.string().min(1, 'Title is required').max(255),
  format: z.string().min(1, 'Select a format')
});

type FormValues = z.infer<typeof schema>;

export function ReportsPage() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const reports = useQuery({ queryKey: ['reports'], queryFn: () => api.listReports() });

  const createMutation = useMutation({
    mutationFn: api.createReport,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      toast.success('Report requested');
      setOpen(false);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Creation failed')
  });

  const generateMutation = useMutation({
    mutationFn: api.generateReport,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      toast.success('Report generation started');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Generation failed')
  });

  const columns = useMemo<ColumnDef<ReportResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'title',
        header: 'Report',
        cell: (info) => (
          <Box>
            <Box sx={{ fontWeight: 600 }}>{info.getValue<string>()}</Box>
            <Box sx={{ fontSize: 12, color: 'text.secondary' }}>{titleCase(info.row.original.type)}</Box>
          </Box>
        )
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: (info) => <StatusChip status={info.getValue<string>()} />
      },
      { accessorKey: 'format', header: 'Format', cell: (info) => info.getValue<string>() || '—' },
      {
        accessorKey: 'score',
        header: 'Score',
        cell: (info) => (info.getValue<number | null>() == null ? '—' : `${Number(info.getValue()).toFixed(1)}%`)
      },
      { accessorKey: 'requestedAt', header: 'Requested', cell: (info) => formatDateTime(info.getValue<string>()) },
      {
        header: 'Actions',
        id: 'actions',
        cell: (info) => {
          const row = info.row.original;
          return (
            <Box sx={{ display: 'flex', gap: 1 }}>
              {row.status === 'REQUESTED' ? (
                <Button size="small" startIcon={<PlayArrow />} onClick={() => generateMutation.mutate(row.id)}>
                  Generate
                </Button>
              ) : null}
              {row.status === 'READY' ? (
                <Button
                  size="small"
                  startIcon={<Download />}
                  onClick={() => toast.success('Download will stream when a storage object is attached.')}
                >
                  Download
                </Button>
              ) : null}
            </Box>
          );
        }
      }
    ],
    [generateMutation]
  );

  return (
    <Box>
      <PageHeader
        title="Reports"
        subtitle="Request integrity and operational reports, then export them."
        actions={
          <Button variant="contained" startIcon={<Add />} onClick={() => setOpen(true)}>
            New report
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={reports.data ?? []}
        loading={reports.isLoading}
        searchPlaceholder="Search reports..."
        searchKeys={['title']}
        emptyTitle="No reports yet"
        emptyDescription="Request your first report to start tracking interview integrity."
      />
      <CreateReportDialog
        open={open}
        onClose={() => setOpen(false)}
        loading={createMutation.isPending}
        onSubmit={(payload) => createMutation.mutate(payload)}
      />
    </Box>
  );
}

function CreateReportDialog({
  open,
  onClose,
  loading,
  onSubmit
}: {
  open: boolean;
  onClose: () => void;
  loading: boolean;
  onSubmit: (payload: import('../lib/types').CreateReportRequest) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: 'INTEGRITY', title: '', format: 'PDF' }
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>New report</DialogTitle>
      <form
        onSubmit={handleSubmit((values) => onSubmit({ type: values.type, title: values.title, format: values.format }))}
        onReset={() => {
          reset();
          onClose();
        }}
      >
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Title"
            fullWidth
            autoFocus
            {...register('title')}
            error={Boolean(errors.title)}
            helperText={errors.title?.message}
          />
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            <TextField
              select
              label="Type"
              {...register('type')}
              error={Boolean(errors.type)}
              helperText={errors.type?.message}
            >
              {REPORT_TYPES.map((type) => (
                <MenuItem key={type} value={type}>
                  {titleCase(type)}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Format"
              {...register('format')}
              error={Boolean(errors.format)}
              helperText={errors.format?.message}
            >
              {REPORT_FORMATS.map((format) => (
                <MenuItem key={format} value={format}>
                  {format}
                </MenuItem>
              ))}
            </TextField>
          </Box>
          <Typography variant="caption" color="text.secondary">
            Reports become available for download once generation completes.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button type="reset">Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? 'Requesting...' : 'Request report'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
