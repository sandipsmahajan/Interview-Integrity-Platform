import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  CalendarToday,
  CheckCircle,
  EventBusy,
  LiveTv,
  RadioButtonChecked,
  WarningAmber
} from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis
} from 'recharts';
import { api } from '../lib/api';
import { formatDateTime, titleCase } from '../lib/format';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { StatusChip } from '../components/StatusChip';
import BoxLoader from '../components/BoxLoader';
import { useAuth } from '../hooks/useAuth';

export function DashboardPage() {
  const { user } = useAuth();

  const interviews = useQuery({
    queryKey: ['interviews'],
    queryFn: () => api.listInterviews(),
    refetchInterval: 30_000
  });

  const candidates = useQuery({
    queryKey: ['candidates'],
    queryFn: () => api.listCandidates()
  });

  const violations = useQuery({
    queryKey: ['violations'],
    queryFn: () => api.listViolations()
  });

  const recruiters = useQuery({
    queryKey: ['recruiters'],
    queryFn: () => api.listRecruiters()
  });

  const summary = useMemo(() => {
    const list = interviews.data ?? [];
    const today = new Date().toDateString();
    const upcoming = list.filter(
      (item) =>
        item.status === 'SCHEDULED' &&
        new Date(item.startsAt).toDateString() === today
    );
    const live = list.filter((item) => item.status === 'LIVE');
    const completed = list.filter((item) => item.status === 'COMPLETED');
    return {
      total: list.length,
      today: upcoming.length,
      live: live.length,
      completed: completed.length,
      scheduled: list.filter((item) => item.status === 'SCHEDULED').length,
      alerts: (violations.data ?? []).filter((item) => item.status === 'OPEN' || item.status === 'REVIEW').length
    };
  }, [interviews.data, violations.data]);

  const trend = useMemo(() => {
    const list = (interviews.data ?? []).filter((item) => item.status === 'COMPLETED');
    const buckets = new Map<string, number>();
    for (const item of list) {
      const day = new Date(item.startsAt).toLocaleDateString();
      buckets.set(day, (buckets.get(day) ?? 0) + 1);
    }
    const days = Array.from({ length: 14 }, (_, index) => {
      const date = new Date();
      date.setDate(date.getDate() - (13 - index));
      const key = date.toLocaleDateString();
      return { date: date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }), completed: buckets.get(key) ?? 0 };
    });
    return days;
  }, [interviews.data]);

  if (interviews.isLoading || candidates.isLoading || recruiters.isLoading) {
    return <BoxLoader rows={6} />;
  }

  const upcomingList = (interviews.data ?? [])
    .filter((item) => item.status === 'SCHEDULED')
    .sort((a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime())
    .slice(0, 5);

  const recentActivity = (violations.data ?? []).slice(0, 5);

  return (
    <Box>
      <PageHeader
        title={`Good to see you${user?.displayName ? `, ${user.displayName.split(' ')[0]}` : ''}`}
        subtitle="Your interview operations at a glance."
        actions={
          <Button component={Link} to="/interviews" variant="contained" startIcon={<CalendarToday />}>
            New interview
          </Button>
        }
      />

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <MetricCard icon={<CalendarToday />} label="Today's interviews" value={summary.today} hint={`${summary.scheduled} scheduled total`} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <MetricCard icon={<LiveTv />} label="Live interviews" value={summary.live} hint="Running right now" color="#f59e0b" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <MetricCard icon={<WarningAmber />} label="Open integrity alerts" value={summary.alerts} hint="Awaiting triage" color="#dc2626" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <MetricCard icon={<CheckCircle />} label="Completed" value={summary.completed} hint={`${candidates.data?.length ?? 0} candidates in pipeline`} color="#16a34a" />
        </Grid>
      </Grid>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Completed interviews (14 days)
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={trend}>
                  <defs>
                    <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#2563eb" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#2563eb" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--divider, #e2e8f0)" />
                  <XAxis dataKey="date" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis fontSize={11} allowDecimals={false} tickLine={false} axisLine={false} />
                  <RechartsTooltip />
                  <Area type="monotone" dataKey="completed" stroke="#2563eb" strokeWidth={2} fill="url(#trendFill)" />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Integrity alerts
              </Typography>
              {recentActivity.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No open violations. Your interview environment is healthy.
                </Typography>
              ) : (
                <Stack divider={<Divider />} spacing={1}>
                  {recentActivity.map((item) => (
                    <Box key={item.id}>
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <RadioButtonChecked fontSize="small" sx={{ color: item.severity === 'CRITICAL' || item.severity === 'HIGH' ? 'error.main' : 'warning.main' }} />
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {titleCase(item.ruleCode)}
                        </Typography>
                        <StatusChip status={item.severity} />
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {formatDateTime(item.occurredAt)}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
              <Button component={Link} to="/integrity" sx={{ mt: 2 }} fullWidth>
                View all alerts
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Upcoming interviews
              </Typography>
              {upcomingList.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No scheduled interviews yet. Schedule your first interview to get started.
                </Typography>
              ) : (
                <Stack divider={<Divider />} spacing={1.5}>
                  {upcomingList.map((item) => (
                    <Box key={item.id} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                      <EventBusy color="action" />
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                          {item.title}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {formatDateTime(item.startsAt)} · {item.mode}
                        </Typography>
                      </Box>
                      <StatusChip status={item.status} />
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 5 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Recruiter performance
              </Typography>
              {recruiters.data?.length ? (
                <Stack divider={<Divider />} spacing={1.5}>
                  {recruiters.data.slice(0, 5).map((recruiter) => (
                    <Box key={recruiter.id} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                      <Box
                        sx={{
                          width: 34,
                          height: 34,
                          borderRadius: '50%',
                          bgcolor: 'primary.main',
                          color: 'primary.contrastText',
                          display: 'grid',
                          placeItems: 'center',
                          fontWeight: 700,
                          fontSize: 13
                        }}
                      >
                        {recruiter.fullName
                          .split(' ')
                          .slice(0, 2)
                          .map((part) => part[0]?.toUpperCase() ?? '')
                          .join('')}
                      </Box>
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                          {recruiter.fullName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {recruiter.title || 'Recruiter'}
                        </Typography>
                      </Box>
                      <StatusChip status={recruiter.status} />
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  No recruiter profiles yet.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
