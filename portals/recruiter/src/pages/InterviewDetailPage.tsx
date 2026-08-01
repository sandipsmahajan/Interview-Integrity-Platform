import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Cancel,
  DesktopWindows,
  EventBusy,
  Link as LinkIcon,
  NoteAdd,
  Schedule,
  Videocam
} from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { formatDateTime, titleCase } from '../lib/format';
import { PageHeader } from '../components/PageHeader';
import { StatusChip } from '../components/StatusChip';
import BoxLoader from '../components/BoxLoader';
import type { ViolationResponse } from '../lib/types';

export function InterviewDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();

  const interview = useQuery({
    queryKey: ['interview', id],
    queryFn: () => api.getInterview(id!),
    enabled: Boolean(id),
    refetchInterval: 15_000
  });

  const violations = useQuery({
    queryKey: ['violations'],
    queryFn: () => api.listViolations()
  });

  const candidates = useQuery({ queryKey: ['candidates'], queryFn: () => api.listCandidates() });
  const recruiters = useQuery({ queryKey: ['recruiters'], queryFn: () => api.listRecruiters() });

  const [elapsed, setElapsed] = useState(0);

  const interviewViolations = useMemo(() => {
    const all = violations.data ?? [];
    return all.filter((item) => item.interviewId === id);
  }, [violations.data, id]);

  const candidate = useMemo(
    () => candidates.data?.find((item) => item.id === interview.data?.candidateId),
    [candidates.data, interview.data]
  );
  const recruiter = useMemo(
    () => recruiters.data?.find((item) => item.id === interview.data?.recruiterId),
    [recruiters.data, interview.data]
  );

  useEffect(() => {
    if (!interview.data?.startsAt) return;
    const start = new Date(interview.data.startsAt).getTime();
    const timer = window.setInterval(() => {
      setElapsed(Math.max(0, Math.floor((Date.now() - start) / 1000)));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [interview.data?.startsAt]);

  const cancelMutation = useMutation({
    mutationFn: () => api.cancelInterview(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interview', id] });
      toast.success('Interview cancelled');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Cancel failed')
  });

  const noShowMutation = useMutation({
    mutationFn: () => api.markInterviewNoShow(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interview', id] });
      toast.success('Marked as no-show');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  if (interview.isLoading) return <BoxLoader rows={4} />;

  const data = interview.data;
  if (!data) {
    return (
      <PageHeader title="Interview not found" subtitle="The interview may have been deleted." />
    );
  }

  const hours = Math.floor(elapsed / 3600);
  const minutes = Math.floor((elapsed % 3600) / 60);
  const seconds = elapsed % 60;
  const timerLabel = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  const desktopLink = `${window.location.origin}/desktop/join?interview=${data.id}`;

  function copy(value: string, label: string) {
    void navigator.clipboard?.writeText(value);
    toast.success(`${label} copied to clipboard`);
  }

  return (
    <Box>
      <PageHeader
        title={data.title}
        subtitle={`${formatDateTime(data.startsAt)} · ${titleCase(data.mode)} · Round ${data.roundNumber}`}
        actions={<StatusChip status={data.status} size="medium" />}
      />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card variant="outlined" sx={{ mb: 2 }}>
            <CardContent>
              <Stack direction="row" spacing={2} sx={{ alignItems: 'center', mb: 2 }}>
                <Box
                  sx={{
                    fontVariantNumeric: 'tabular-nums',
                    fontSize: 28,
                    fontWeight: 800,
                    fontFamily: 'monospace',
                    px: 2,
                    py: 1,
                    borderRadius: 2,
                    bgcolor: 'background.default',
                    border: 1,
                    borderColor: 'divider'
                  }}
                >
                  {timerLabel}
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Elapsed since scheduled start
                </Typography>
              </Stack>
              <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
                <Tooltip title="Copy desktop client join link">
                  <Button startIcon={<DesktopWindows />} variant="outlined" onClick={() => copy(desktopLink, 'Desktop client link')}>
                    Desktop client link
                  </Button>
                </Tooltip>
                <Tooltip title="Copy meeting link">
                  <Button
                    startIcon={<Videocam />}
                    variant="outlined"
                    disabled={!data.meetingUrl}
                    onClick={() => copy(data.meetingUrl, 'Meeting link')}
                  >
                    Meeting link
                  </Button>
                </Tooltip>
              </Stack>
              <Divider sx={{ my: 2 }} />
              <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
                <Button startIcon={<Schedule />} variant="outlined" disabled>
                  Reschedule
                </Button>
                <Button startIcon={<EventBusy />} variant="outlined" onClick={() => noShowMutation.mutate()} disabled={cancelMutation.isPending}>
                  Mark no-show
                </Button>
                <Button
                  startIcon={<Cancel />}
                  color="error"
                  variant="outlined"
                  onClick={() => cancelMutation.mutate()}
                  disabled={cancelMutation.isPending || ['CANCELLED', 'COMPLETED'].includes(data.status)}
                >
                  Cancel interview
                </Button>
              </Stack>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Chip icon={<LinkIcon />} label={data.meetingUrl || 'No meeting link set'} />
              </Box>
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Integrity timeline
              </Typography>
              {interviewViolations.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No integrity events recorded for this interview yet. Telemetry events will appear here in real time.
                </Typography>
              ) : (
                <Stack divider={<Divider />} spacing={1.5}>
                  {interviewViolations.map((item: ViolationResponse) => (
                    <Box key={item.id}>
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {titleCase(item.ruleCode)}
                        </Typography>
                        <StatusChip status={item.severity} />
                        <StatusChip status={item.status} />
                      </Stack>
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                        {item.message}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {formatDateTime(item.occurredAt)}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined" sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Details
              </Typography>
              <Stack spacing={1.5}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Candidate
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {candidate?.fullName ?? data.candidateId}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {candidate?.email ?? 'No candidate profile'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Recruiter
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {recruiter?.fullName ?? data.recruiterId}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Timezone
                  </Typography>
                  <Typography variant="body2">{data.timezone || 'Default'}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Round
                  </Typography>
                  <Typography variant="body2">{data.roundNumber}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
                <NoteAdd color="action" fontSize="small" />
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  Recruiter notes
                </Typography>
              </Stack>
              <Typography variant="body2" color="text.secondary">
                Recruiter notes for this interview are managed through the candidate profile notes API. Add observations from the candidate screen.
              </Typography>
              <Button component={Link} to={candidate ? `/candidates/${candidate.id}` : '/candidates'} size="small" sx={{ mt: 1 }} startIcon={<NoteAdd />}>
                Open candidate profile
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
