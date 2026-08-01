import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Add, Videocam } from '@mui/icons-material';
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
import type { InterviewResponse } from '../lib/types';

const schema = z.object({
  title: z.string().min(1, 'Title is required').max(250),
  candidateId: z.string().min(1, 'Select a candidate'),
  recruiterId: z.string().min(1, 'Select a recruiter'),
  roundNumber: z.coerce.number().min(1).max(100),
  mode: z.string().min(1, 'Select a mode'),
  startsAt: z.string().min(1, 'Start time is required'),
  endsAt: z.string().min(1, 'End time is required'),
  meetingUrl: z.string().url('Enter a valid URL').optional().or(z.literal(''))
});

type FormValues = z.input<typeof schema>;
type FormOutput = z.output<typeof schema>;

const INTERVIEW_MODES = ['IN_PERSON', 'VIDEO', 'PHONE', 'ASYNC'];

export function InterviewsPage() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const interviews = useQuery({ queryKey: ['interviews'], queryFn: () => api.listInterviews() });
  const candidates = useQuery({ queryKey: ['candidates'], queryFn: () => api.listCandidates() });
  const recruiters = useQuery({ queryKey: ['recruiters'], queryFn: () => api.listRecruiters() });

  const createMutation = useMutation({
    mutationFn: api.createInterview,
    onSuccess: (interview) => {
      queryClient.invalidateQueries({ queryKey: ['interviews'] });
      toast.success('Interview created');
      setOpen(false);
      navigate(`/interviews/${interview.id}`);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Creation failed')
  });

  const columns = useMemo<ColumnDef<InterviewResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'title',
        header: 'Title',
        cell: (info) => (
          <Link to={`/interviews/${info.row.original.id}`} style={{ fontWeight: 600, color: 'primary.main' }}>
            {info.getValue<string>()}
          </Link>
        )
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: (info) => <StatusChip status={info.getValue<string>()} />
      },
      { accessorKey: 'mode', header: 'Mode', cell: (info) => titleCase(info.getValue<string>()) },
      {
        accessorKey: 'startsAt',
        header: 'Starts',
        cell: (info) => formatDateTime(info.getValue<string>())
      },
      {
        accessorKey: 'endsAt',
        header: 'Ends',
        cell: (info) => formatDateTime(info.getValue<string>())
      },
      {
        header: 'Actions',
        id: 'actions',
        cell: (info) => (
          <Button size="small" onClick={() => navigate(`/interviews/${info.row.original.id}`)} startIcon={<Videocam />}>
            Open
          </Button>
        )
      }
    ],
    [navigate]
  );

  return (
    <Box>
      <PageHeader
        title="Interviews"
        subtitle="Schedule, monitor and manage your interview pipeline."
        actions={
          <Button variant="contained" startIcon={<Add />} onClick={() => setOpen(true)}>
            Create interview
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={interviews.data ?? []}
        loading={interviews.isLoading}
        searchPlaceholder="Search interviews..."
        searchKeys={['title']}
        emptyTitle="No interviews yet"
        emptyDescription="Create your first interview to start scheduling candidates."
      />

      <CreateInterviewDialog
        open={open}
        onClose={() => setOpen(false)}
        candidates={candidates.data ?? []}
        recruiters={recruiters.data ?? []}
        loading={createMutation.isPending}
        onSubmit={createMutation.mutate}
      />
    </Box>
  );
}

interface CreateInterviewDialogProps {
  open: boolean;
  onClose: () => void;
  candidates: { id: string; fullName: string }[];
  recruiters: { id: string; fullName: string }[];
  loading: boolean;
  onSubmit: (payload: import('../lib/types').CreateInterviewRequest) => void;
}

function CreateInterviewDialog({ open, onClose, candidates, recruiters, loading, onSubmit }: CreateInterviewDialogProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<FormValues, unknown, FormOutput>({
    resolver: zodResolver(schema),
    defaultValues: { title: '', candidateId: '', recruiterId: '', roundNumber: 1, mode: 'VIDEO', startsAt: '', endsAt: '', meetingUrl: '' }
  });

  function submit(values: FormOutput) {
    const startsAt = new Date(values.startsAt).toISOString();
    const endsAt = new Date(values.endsAt).toISOString();
    onSubmit({
      candidateId: values.candidateId,
      recruiterId: values.recruiterId,
      roundNumber: values.roundNumber,
      title: values.title,
      mode: values.mode,
      startsAt,
      endsAt,
      meetingUrl: values.meetingUrl || null,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
    });
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Create interview</DialogTitle>
      <form
        onSubmit={handleSubmit(submit)}
        onReset={() => {
          reset();
          onClose();
        }}
      >
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Title"
            fullWidth
            {...register('title')}
            error={Boolean(errors.title)}
            helperText={errors.title?.message}
          />
          <TextField
            select
            label="Candidate"
            fullWidth
            {...register('candidateId')}
            error={Boolean(errors.candidateId)}
            helperText={errors.candidateId?.message}
          >
            <MenuItem value="">Select candidate</MenuItem>
            {candidates.map((candidate) => (
              <MenuItem key={candidate.id} value={candidate.id}>
                {candidate.fullName}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Recruiter"
            fullWidth
            {...register('recruiterId')}
            error={Boolean(errors.recruiterId)}
            helperText={errors.recruiterId?.message}
          >
            <MenuItem value="">Select recruiter</MenuItem>
            {recruiters.map((recruiter) => (
              <MenuItem key={recruiter.id} value={recruiter.id}>
                {recruiter.fullName}
              </MenuItem>
            ))}
          </TextField>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            <TextField
              select
              label="Mode"
              {...register('mode')}
              error={Boolean(errors.mode)}
              helperText={errors.mode?.message}
            >
              {INTERVIEW_MODES.map((mode) => (
                <MenuItem key={mode} value={mode}>
                  {titleCase(mode)}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Round number"
              type="number"
              slotProps={{ htmlInput: { min: 1, max: 100 } }}
              {...register('roundNumber')}
              error={Boolean(errors.roundNumber)}
              helperText={errors.roundNumber?.message}
            />
          </Box>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            <TextField
              label="Starts at"
              type="datetime-local"
              slotProps={{ inputLabel: { shrink: true } }}
              {...register('startsAt')}
              error={Boolean(errors.startsAt)}
              helperText={errors.startsAt?.message}
            />
            <TextField
              label="Ends at"
              type="datetime-local"
              slotProps={{ inputLabel: { shrink: true } }}
              {...register('endsAt')}
              error={Boolean(errors.endsAt)}
              helperText={errors.endsAt?.message}
            />
          </Box>
          <TextField
            label="Meeting link"
            fullWidth
            placeholder="https://..."
            {...register('meetingUrl')}
            error={Boolean(errors.meetingUrl)}
            helperText={errors.meetingUrl?.message}
          />
          <Typography variant="body2" color="text.secondary">
            A desktop client link and meeting link will be available from the interview detail view.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button type="reset">Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? 'Creating...' : 'Create interview'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
