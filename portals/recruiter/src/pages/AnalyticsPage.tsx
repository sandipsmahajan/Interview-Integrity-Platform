import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import { api } from '../lib/api';
import { PageHeader } from '../components/PageHeader';
import BoxLoader from '../components/BoxLoader';
import { titleCase } from '../lib/format';

const STATUS_COLORS: Record<string, string> = {
  SCHEDULED: '#2563eb',
  LIVE: '#f59e0b',
  COMPLETED: '#16a34a',
  CANCELLED: '#dc2626',
  NO_SHOW: '#94a3b8'
};

export function AnalyticsPage() {
  const interviews = useQuery({ queryKey: ['interviews'], queryFn: () => api.listInterviews() });
  const violations = useQuery({ queryKey: ['violations'], queryFn: () => api.listViolations() });
  const recruiters = useQuery({ queryKey: ['recruiters'], queryFn: () => api.listRecruiters() });

  const statusData = useMemo(() => {
    const buckets = new Map<string, number>();
    for (const item of interviews.data ?? []) {
      buckets.set(item.status, (buckets.get(item.status) ?? 0) + 1);
    }
    return Array.from(buckets.entries()).map(([name, value]) => ({ name: titleCase(name), value, color: STATUS_COLORS[name] ?? '#94a3b8' }));
  }, [interviews.data]);

  const violationTrend = useMemo(() => {
    const buckets = new Map<string, number>();
    for (const item of violations.data ?? []) {
      const day = new Date(item.occurredAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
      buckets.set(day, (buckets.get(day) ?? 0) + 1);
    }
    return Array.from(buckets.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, count]) => ({ date, count }));
  }, [violations.data]);

  const recruiterLoad = useMemo(() => {
    const buckets = new Map<string, number>();
    const names = new Map<string, string>();
    for (const item of interviews.data ?? []) {
      const recruiter = recruiters.data?.find((r) => r.id === item.recruiterId);
      const name = recruiter?.fullName ?? item.recruiterId.slice(0, 8);
      names.set(item.recruiterId, name);
      buckets.set(item.recruiterId, (buckets.get(item.recruiterId) ?? 0) + 1);
    }
    return Array.from(buckets.entries()).map(([id, count]) => ({ name: names.get(id) ?? id, interviews: count }));
  }, [interviews.data, recruiters.data]);

  const severityData = useMemo(() => {
    const buckets = new Map<string, number>();
    for (const item of violations.data ?? []) {
      buckets.set(item.severity, (buckets.get(item.severity) ?? 0) + 1);
    }
    return Array.from(buckets.entries()).map(([name, value]) => ({ name: titleCase(name), value }));
  }, [violations.data]);

  if (interviews.isLoading || violations.isLoading || recruiters.isLoading) {
    return <BoxLoader rows={6} />;
  }

  return (
    <Box>
      <PageHeader title="Analytics" subtitle="Understand interview volume, integrity trends and team performance." />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Interviews by status
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie data={statusData} dataKey="value" nameKey="name" outerRadius={100} label>
                    {statusData.map((entry, index) => (
                      <Cell key={index} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Violations by severity
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={severityData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--divider, #e2e8f0)" />
                  <XAxis dataKey="name" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis fontSize={11} allowDecimals={false} tickLine={false} axisLine={false} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#dc2626" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Recruiter workload
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={recruiterLoad} layout="vertical" margin={{ left: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--divider, #e2e8f0)" />
                  <XAxis type="number" fontSize={11} allowDecimals={false} tickLine={false} axisLine={false} />
                  <YAxis type="category" dataKey="name" fontSize={11} width={120} tickLine={false} axisLine={false} />
                  <Tooltip />
                  <Bar dataKey="interviews" fill="#0ea5e9" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Integrity violations over time
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={violationTrend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--divider, #e2e8f0)" />
                  <XAxis dataKey="date" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis fontSize={11} allowDecimals={false} tickLine={false} axisLine={false} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#d97706" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
