import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { Activity, AlertTriangle, ClipboardList, Radio } from 'lucide-react';
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom';
import { AppShell, MetricTile } from '@interview-integrity/components';
import {
  Box,
  Chip,
  CssBaseline,
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from '@interview-integrity/ui';
import './style.css';

const interviews = [
  { name: 'Aarav Sharma', role: 'Senior Rust Engineer', status: 'Live', risk: 18, network: 'Stable', alerts: 1 },
  { name: 'Maya Iyer', role: 'Platform Architect', status: 'Waiting', risk: 0, network: 'Pending', alerts: 0 },
  { name: 'Noah Lee', role: 'Backend Lead', status: 'Review', risk: 42, network: 'Recovered', alerts: 3 }
];

const queryClient = new QueryClient();

function useInterviewQueue() {
  return useQuery({
    queryKey: ['interview-queue'],
    queryFn: async () => interviews,
    staleTime: 30_000
  });
}

function Queue() {
  const queue = useInterviewQueue();

  return (
    <Paper className="panel">
      <Typography variant="h6">Interview Queue</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Candidate</TableCell>
            <TableCell>Role</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Risk</TableCell>
            <TableCell>Network</TableCell>
            <TableCell>Alerts</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {(queue.data ?? []).map((row) => (
            <TableRow key={row.name}>
              <TableCell>{row.name}</TableCell>
              <TableCell>{row.role}</TableCell>
              <TableCell>{row.status}</TableCell>
              <TableCell>
                <LinearProgress variant="determinate" value={row.risk} />
              </TableCell>
              <TableCell>{row.network}</TableCell>
              <TableCell>{row.alerts}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <CssBaseline />
      <BrowserRouter>
        <AppShell
          title="Recruiter Console"
          status={<Chip icon={<Radio size={16} />} label="Live telemetry connected" color="success" variant="outlined" />}
        >
          <Box className="tabs">
            <NavLink to="/">Queue</NavLink>
            <NavLink to="/live">Live</NavLink>
          </Box>
        <Box className="grid">
            <MetricTile icon={<Activity />} label="Live Interviews" value={7} />
            <MetricTile icon={<AlertTriangle />} label="Open Alerts" value={4} />
            <MetricTile icon={<ClipboardList />} label="Reports Ready" value={12} />
        </Box>
          <Routes>
            <Route path="/" element={<Queue />} />
            <Route path="/live" element={<Queue />} />
          </Routes>
        </AppShell>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
